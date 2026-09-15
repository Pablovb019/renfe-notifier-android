"""Planificador de comprobaciones con un único propietario.

Cada ciclo lee los seguimientos activos desde la base de datos (recuperación
tras reinicio), los agrupa por clave de consulta y ejecuta una búsqueda por
grupo con concurrencia global limitada. Nunca acumula tareas atrasadas: el
bucle espera a que termine el ciclo anterior antes de dormir.
"""

import asyncio
import logging
from collections.abc import Awaitable, Callable
from datetime import UTC, datetime

from app.followups.database import FollowUpRepository
from app.followups.domain import FollowUp, Lifecycle
from app.monitoring import monitoring
from app.renfe.client import RenfeClientError, SearchResult
from app.renfe.stations import StationCatalog
from app.scheduler.plan import GroupKey, build_plan, error_observation, train_list_to_observation

logger = logging.getLogger(__name__)

SearchFn = Callable[..., Awaitable[SearchResult]]
Clock = Callable[[], datetime]
Sleep = Callable[[float], Awaitable[None]]


class SchedulerService:
    """Ejecuta un ciclo por tick con concurrencia limitada por grupo.

    - Un único propietario del planificador: arranca una sola instancia.
    - Ninguna búsqueda cuando no hay seguimientos activos.
    - Agrupación por (origen, destino, fecha, plaza_h); cada grupo una vez por
      ciclo (exclusión de comprobaciones simultáneas del mismo grupo).
    - Concurrencia global limitada (1 o 2) mediante semáforo.
    - Sin acumulación de ciclo atrasados.
    - Recuperación tras reinicio: el estado se rehidrata desde SQLite en cada
      ciclo; no se guarda estado volátil en memoria.
    - Aislamiento de sesiones: ``search_fn`` debe crear una sesión HTTP efímera
      por llamada (``RenfeDwrClient.search``).
    """

    def __init__(
        self,
        *,
        repository: FollowUpRepository,
        search_fn: SearchFn,
        catalog: StationCatalog,
        clock: Clock = lambda: datetime.now(UTC),
        interval_s: float = 30.0,
        concurrency: int = 2,
        sleep: Sleep = asyncio.sleep,
    ) -> None:
        self._repository = repository
        self._search = search_fn
        self._catalog = catalog
        self._clock = clock
        self._interval_s = interval_s
        self._concurrency = concurrency
        self._sleep = sleep

    async def run_once(self) -> None:
        """Un único ciclo del planificador: leer, agrupar, buscar y persistir."""
        active = list(self._repository.list(Lifecycle.ACTIVE))
        if not active:
            return
        plans = build_plan(active)
        semaphore = asyncio.Semaphore(self._concurrency)

        async def guarded(group: tuple[GroupKey, list[FollowUp]]) -> None:
            async with semaphore:
                await self._check_group(*group)

        await asyncio.gather(*(guarded(plan) for plan in plans))

    async def run_forever(self) -> None:
        """Bucle infinito: un ciclo completo y después la pausa configurada."""
        while True:
            await self.run_once()
            await self._sleep(self._interval_s)

    async def _check_group(self, key: GroupKey, followups: list[FollowUp]) -> None:
        origin = self._catalog.by_code(key.origin_code)
        destination = self._catalog.by_code(key.destination_code)
        if origin is None or destination is None:
            logger.error("Estación desconocida para el grupo %s; grupo descartado.", key)
            return

        now = self._clock()
        monitoring.record_logical_query()
        try:
            result = await self._search(
                origin=origin,
                destination=destination,
                travel_date=key.travel_date,
                plaza_h=key.plaza_h,
            )
            monitoring.record_search(
                http_requests=result.metrics.request_count,
                bytes_received=result.metrics.bytes_received,
            )
            observation = train_list_to_observation(result.trains, now)
        except RenfeClientError as error:
            logger.warning("Error en la búsqueda del grupo %s: %s", key, error)
            observation = error_observation(now)

        for followup in followups:
            outcome = followup.apply(observation)
            if outcome.followup is not followup:
                self._repository.save(outcome.followup, now=now)
            if outcome.new_episode:
                self._repository.add_episode(
                    followup.followup_id,
                    outcome.followup.episode,
                    now,
                    outcome.followup.seen_available_train_ids,
                )
