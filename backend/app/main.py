"""Aplicación ASGI del backend, con emparejamiento autenticado."""

import asyncio
import logging
from collections.abc import AsyncIterator, Awaitable, Callable
from contextlib import asynccontextmanager
from datetime import date
from pathlib import Path

from fastapi import FastAPI, Request, Response
from fastapi.middleware.cors import CORSMiddleware
from fastapi.responses import JSONResponse

from app import APP_VERSION
from app.api.diagnostics import router as diagnostics_router
from app.api.fcm import router as fcm_router
from app.api.followups import router as followups_router
from app.api.health import router as health_router
from app.api.pairing import router as pairing_router
from app.api.search import router as search_router
from app.config import Settings
from app.followups.database import FollowUpRepository
from app.middleware.logging import SensitiveFormatter
from app.middleware.rate_limit import InMemoryRateLimiter
from app.notifications.fcm import AutoTokenProvider, FcmNotificationSender
from app.pairing.database import PairingRepository
from app.pairing.service import PairingService
from app.reminders.delivery import AlertDeliveryService
from app.reminders.queue import AlertQueue
from app.renfe.client import RenfeDwrClient, SearchResult
from app.renfe.search import TrainSearchEngine
from app.renfe.stations import Station, StationCatalog
from app.scheduler.service import SchedulerService, SearchFn

logger = logging.getLogger(__name__)

LOG_FORMAT = "%(asctime)s [%(levelname)s] %(name)s: %(message)s"

# Flag global para evitar inicialización múltiple en lifespan (TestClient llama lifespan varias veces)
_lifespan_initialized: set[Path] = set()


def create_app(settings: Settings | None = None) -> FastAPI:
    """Factoría de la aplicación; facilita los tests sin efectos globales."""
    app_settings = settings or Settings()

    @asynccontextmanager
    async def lifespan(app: FastAPI) -> AsyncIterator[None]:
        # Evitar inicialización múltiple en TestClient (merged_lifespan llama lifespan varias veces)
        if app_settings.database_path in _lifespan_initialized:
            logger.info("Lifespan ya inicializado, omitiendo...")
            yield
            return
        _lifespan_initialized.add(app_settings.database_path)

        logging.basicConfig(
            level=logging.DEBUG if app_settings.debug else logging.INFO,
            format=LOG_FORMAT,
            force=True,
        )
        for handler in logging.getLogger().handlers:
            handler.setFormatter(SensitiveFormatter(LOG_FORMAT))
        pairing_service = PairingService(
            PairingRepository(app_settings.database_path),
            code_ttl_s=app_settings.pairing_code_ttl_s,
            max_attempts=app_settings.pairing_max_attempts,
        )
        pairing_service.initialize()
        followups_repository = FollowUpRepository(app_settings.database_path)
        followups_repository.initialize()
        alert_queue = AlertQueue(
            app_settings.database_path, interval_s=app_settings.reminder_interval_s
        )
        alert_queue.initialize()
        catalog = StationCatalog()
        search_engine = TrainSearchEngine(RenfeDwrClient(), catalog)
        fcm_sender = (
            FcmNotificationSender(
                project_id=app_settings.fcm_project_id,
                token_provider=AutoTokenProvider(),
                timeout_s=app_settings.fcm_timeout_s,
                default_ttl_s=app_settings.fcm_default_ttl_s,
                package=app_settings.fcm_app_package,
            )
            if app_settings.fcm_project_id
            else None
        )
        app.state.settings = app_settings
        app.state.pairing_service = pairing_service
        app.state.followups_repository = followups_repository
        app.state.alert_queue = alert_queue
        app.state.station_catalog = catalog
        app.state.search_engine = search_engine
        app.state.fcm_sender = fcm_sender
        app.state.test_sender = fcm_sender

        background_tasks: list[asyncio.Task[None]] = []
        if app_settings.scheduler_enabled:
            scheduler = SchedulerService(
                repository=followups_repository,
                search_fn=_search_adapter(search_engine),
                catalog=catalog,
                interval_s=app_settings.scheduler_interval_s,
                queue=alert_queue,
                initial_delay_s=app_settings.reminder_initial_delay_s,
                max_attempts=app_settings.reminder_max_attempts,
            )
            delivery = AlertDeliveryService(
                queue=alert_queue,
                followups=followups_repository,
                pairing=pairing_service,
                catalog=catalog,
                sender=fcm_sender,
                batch=app_settings.delivery_batch,
                interval_s=app_settings.delivery_interval_s,
            )
            background_tasks = [
                asyncio.create_task(scheduler.run_forever(), name="renfe-scheduler"),
                asyncio.create_task(delivery.run_forever(), name="renfe-delivery"),
            ]
            logger.info("Planificador y entrega de avisos activados.")
        logger.info("Arrancando renfe-notifier-backend")
        yield
        for task in background_tasks:
            task.cancel()
        await asyncio.gather(*background_tasks, return_exceptions=True)
        logger.info("Backend detenido correctamente")

    app = FastAPI(
        title="Renfe Notifier Backend",
        version=APP_VERSION,
        docs_url="/docs" if app_settings.debug else None,
        redoc_url=None,
        openapi_url="/openapi.json" if app_settings.debug else None,
        lifespan=lifespan,
    )

    app.add_middleware(
        CORSMiddleware,
        allow_origins=["http://localhost", "http://127.0.0.1"] if app_settings.debug else [],
        allow_credentials=False,
        allow_methods=["GET", "POST", "PUT", "DELETE"],
        allow_headers=["Authorization", "Content-Type"],
    )

    app.include_router(health_router)
    app.include_router(pairing_router)
    app.include_router(search_router)
    app.include_router(followups_router)
    app.include_router(fcm_router)
    app.include_router(diagnostics_router)

    claim_limiter = InMemoryRateLimiter(
        limit=app_settings.rate_limit_max_requests,
        window_s=app_settings.rate_limit_window_s,
    )

    @app.middleware("http")
    async def limit_pairing_claims(
        request: Request, call_next: Callable[..., Awaitable[Response]]
    ) -> Response:
        if request.url.path == "/api/v1/pairing/claim":
            key = request.client.host if request.client else "unknown"
            if not claim_limiter.allowed(key):
                return JSONResponse(status_code=429, content={"detail": "Demasiadas peticiones"})
        return await call_next(request)

    return app


def _search_adapter(search_engine: TrainSearchEngine) -> SearchFn:
    """Traduce la firma del planificador (objetos Station) a la del motor (códigos)."""

    async def search(
        *,
        origin: Station,
        destination: Station,
        travel_date: date,
        plaza_h: bool,
    ) -> SearchResult:
        return await search_engine.search(
            origin_code=origin.code,
            destination_code=destination.code,
            travel_date=travel_date,
            plaza_h=plaza_h,
        )

    return search


app = create_app()
