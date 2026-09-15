"""Cliente HTTP/DWR de Renfe con estado aislado por búsqueda.

No inicia navegadores ni intenta evadir bloqueos o CAPTCHA. Las pruebas deben
inyectar ``httpx.MockTransport``: este módulo no realiza peticiones al importar.
"""

import asyncio
import random
import re
import secrets
import time
from collections.abc import Awaitable, Callable
from dataclasses import dataclass
from datetime import UTC, date, datetime
from email.utils import parsedate_to_datetime

import httpx

from app.renfe.parser import TrainList, parse_train_list
from app.renfe.stations import Station

SEARCH_URL = "https://venta.renfe.com/vol/buscarTren.do?Idioma=es&Pais=ES"
DWR_ENDPOINT = "https://venta.renfe.com/vol/dwr/call/plaincall"
SYSTEM_ID_URL = f"{DWR_ENDPOINT}/__System.generateId.dwr"
UPDATE_SESSION_URL = f"{DWR_ENDPOINT}/buyEnlacesManager.actualizaObjetosSesion.dwr"
TRAIN_LIST_URL = f"{DWR_ENDPOINT}/trainEnlacesManager.getTrainsList.dwr"
_TOKEN_CALLBACK = re.compile(
    r"(?:dwr\.engine\.remote\.|r\.)handleCallback\s*\(\s*['\"]\d+['\"]\s*,\s*"
    r"['\"]\d+['\"]\s*,\s*['\"]([A-Za-z0-9]+)['\"]"
)


class RenfeClientError(RuntimeError):
    """Error controlado del protocolo HTTP/DWR."""


class RenfeAccessBlockedError(RenfeClientError):
    """Renfe respondió 403; no se debe reintentar ni evadir el bloqueo."""


class RenfeRateLimitedError(RenfeClientError):
    """Renfe mantuvo el límite de tasa tras los reintentos permitidos."""


class RenfeResponseError(RenfeClientError):
    """Renfe respondió con un estado HTTP no recuperable."""


class RenfeTransportError(RenfeClientError):
    """La conexión falló tras los reintentos permitidos."""


class RenfeBudgetExceededError(RenfeClientError):
    """La búsqueda excedió su presupuesto total de tiempo."""


@dataclass(frozen=True, slots=True)
class RequestMetric:
    phase: str
    status_code: int | None
    duration_s: float
    bytes_received: int


@dataclass(frozen=True, slots=True)
class SearchMetrics:
    request_count: int
    duration_s: float
    bytes_received: int
    requests: tuple[RequestMetric, ...]


@dataclass(frozen=True, slots=True)
class SearchResult:
    trains: TrainList
    metrics: SearchMetrics


Sleep = Callable[[float], Awaitable[None]]
Jitter = Callable[[], float]


class RenfeDwrClient:
    """Ejecuta las cinco peticiones DWR comprobadas en una sesión efímera."""

    def __init__(
        self,
        *,
        per_request_timeout_s: float = 10.0,
        total_budget_s: float = 30.0,
        max_retries: int = 2,
        transport: httpx.AsyncBaseTransport | None = None,
        sleep: Sleep = asyncio.sleep,
        jitter: Jitter = random.random,
    ) -> None:
        if per_request_timeout_s <= 0 or total_budget_s <= 0:
            raise ValueError("Los timeouts deben ser positivos")
        if max_retries < 0:
            raise ValueError("max_retries no puede ser negativo")
        self._per_request_timeout_s = per_request_timeout_s
        self._total_budget_s = total_budget_s
        self._max_retries = max_retries
        self._transport = transport
        self._sleep = sleep
        self._jitter = jitter

    async def search(
        self,
        *,
        origin: Station,
        destination: Station,
        travel_date: date,
        plaza_h: bool,
    ) -> SearchResult:
        """Consulta una ruta usando una sesión aislada y cerrada al terminar."""
        started_at = time.monotonic()
        metrics: list[RequestMetric] = []
        timeout = httpx.Timeout(self._per_request_timeout_s)
        try:
            async with asyncio.timeout(self._total_budget_s):
                async with httpx.AsyncClient(
                    timeout=timeout,
                    transport=self._transport,
                    follow_redirects=True,
                ) as session:
                    search_id = secrets.token_hex(4)
                    date_text = travel_date.strftime("%d/%m/%Y")
                    await self._post(
                        session,
                        "search",
                        SEARCH_URL,
                        self._search_payload(origin, destination, date_text, plaza_h),
                        metrics,
                    )
                    await self._post(
                        session,
                        "generate_id_0",
                        SYSTEM_ID_URL,
                        self._generate_id_payload(search_id, 0),
                        metrics,
                    )
                    token_response = await self._post(
                        session,
                        "generate_id_1",
                        SYSTEM_ID_URL,
                        self._generate_id_payload(search_id, 1),
                        metrics,
                    )
                    dwr_token = self._extract_dwr_token(token_response.text)
                    script_session_id = self._script_session_id(dwr_token)
                    session.cookies.set(
                        "DWRSESSIONID", dwr_token, domain="venta.renfe.com", path="/vol"
                    )
                    await self._post(
                        session,
                        "update_session",
                        UPDATE_SESSION_URL,
                        self._update_session_payload(search_id, script_session_id, 2),
                        metrics,
                    )
                    train_response = await self._post(
                        session,
                        "train_list",
                        TRAIN_LIST_URL,
                        self._train_list_payload(search_id, script_session_id, 3, plaza_h),
                        metrics,
                    )
                    trains = parse_train_list(train_response.text, plaza_h_requested=plaza_h)
        except TimeoutError as error:
            raise RenfeBudgetExceededError(
                "Se agotó el presupuesto total de la búsqueda"
            ) from error

        duration_s = time.monotonic() - started_at
        return SearchResult(
            trains=trains,
            metrics=SearchMetrics(
                request_count=len(metrics),
                duration_s=duration_s,
                bytes_received=sum(metric.bytes_received for metric in metrics),
                requests=tuple(metrics),
            ),
        )

    async def _post(
        self,
        session: httpx.AsyncClient,
        phase: str,
        url: str,
        data: dict[str, str],
        metrics: list[RequestMetric],
    ) -> httpx.Response:
        for attempt in range(self._max_retries + 1):
            started_at = time.monotonic()
            try:
                response = await session.post(url, data=data)
            except httpx.TransportError as error:
                metrics.append(RequestMetric(phase, None, time.monotonic() - started_at, 0))
                if attempt == self._max_retries:
                    raise RenfeTransportError(f"Fallo de transporte durante {phase}") from error
                await self._sleep(self._backoff_delay(attempt, None))
                continue

            metrics.append(
                RequestMetric(
                    phase,
                    response.status_code,
                    time.monotonic() - started_at,
                    len(response.content),
                )
            )
            if response.status_code == 403:
                raise RenfeAccessBlockedError("Renfe rechazó la consulta (HTTP 403)")
            if response.status_code == 429:
                if attempt == self._max_retries:
                    raise RenfeRateLimitedError("Renfe mantiene el límite de tasa (HTTP 429)")
                await self._sleep(self._backoff_delay(attempt, response.headers.get("Retry-After")))
                continue
            if response.status_code in {502, 503, 504}:
                if attempt == self._max_retries:
                    raise RenfeResponseError(f"Error HTTP {response.status_code} durante {phase}")
                await self._sleep(self._backoff_delay(attempt, response.headers.get("Retry-After")))
                continue
            if response.is_error:
                raise RenfeResponseError(f"Error HTTP {response.status_code} durante {phase}")
            return response
        raise AssertionError("Bucle de reintentos inesperadamente agotado")

    def _backoff_delay(self, attempt: int, retry_after: str | None) -> float:
        backoff = min(8.0, float(2**attempt)) + (self._jitter() * 0.25)
        retry_after_s = self._retry_after_seconds(retry_after)
        return max(backoff, retry_after_s) if retry_after_s is not None else backoff

    @staticmethod
    def _retry_after_seconds(value: str | None) -> float | None:
        if value is None:
            return None
        try:
            return max(0.0, float(value))
        except ValueError:
            try:
                retry_at = parsedate_to_datetime(value)
            except (TypeError, ValueError):
                return None
            if retry_at.tzinfo is None:
                retry_at = retry_at.replace(tzinfo=UTC)
            return max(0.0, (retry_at - datetime.now(UTC)).total_seconds())

    @staticmethod
    def _extract_dwr_token(response_text: str) -> str:
        match = _TOKEN_CALLBACK.search(response_text)
        if match is None:
            raise RenfeResponseError("La segunda respuesta generateId no contiene token DWR")
        return match.group(1)

    @staticmethod
    def _script_session_id(dwr_token: str) -> str:
        return f"{dwr_token}/{int(time.time() * 1000)}-{secrets.token_hex(4)}"

    @staticmethod
    def _search_payload(
        origin: Station, destination: Station, date_text: str, plaza_h: bool
    ) -> dict[str, str]:
        return {
            "idGo": date_text,
            "idVuelta": "",
            "idOrig": origin.code,
            "idDest": destination.code,
            "nombOrig": origin.name,
            "nombDest": destination.name,
            "FechaIda": date_text,
            "FechaVuelta": "",
            "horaIda": "",
            "horaVuelta": "",
            "Plaza": "H" if plaza_h else "N",
            "df": "false",
        }

    @staticmethod
    def _generate_id_payload(search_id: str, batch_id: int) -> dict[str, str]:
        return {
            "callCount": "1",
            "page": "/vol/buscarTren.do?Idioma=es&Pais=ES",
            "httpSessionState": "!",
            "scriptSessionId": "",
            "c0-scriptName": "__System",
            "c0-methodName": "generateId",
            "c0-id": f"0:{search_id}",
            "c0-param0": f"string:{batch_id:04d}",
            "batchId": str(batch_id),
        }

    @staticmethod
    def _update_session_payload(
        search_id: str, script_session_id: str, batch_id: int
    ) -> dict[str, str]:
        return {
            "callCount": "1",
            "page": "/vol/buscarTren.do?Idioma=es&Pais=ES",
            "httpSessionState": "!",
            "scriptSessionId": script_session_id,
            "c0-scriptName": "buyEnlacesManager",
            "c0-methodName": "actualizaObjetosSesion",
            "c0-id": f"0:{search_id}",
            "c0-param0": f"string:{search_id}",
            "c0-param1": f"string:{script_session_id}",
            "batchId": str(batch_id),
        }

    @staticmethod
    def _train_list_payload(
        search_id: str, script_session_id: str, batch_id: int, plaza_h: bool
    ) -> dict[str, str]:
        return {
            "callCount": "1",
            "page": "/vol/buscarTren.do?Idioma=es&Pais=ES",
            "httpSessionState": "!",
            "scriptSessionId": script_session_id,
            "c0-scriptName": "trainEnlacesManager",
            "c0-methodName": "getTrainsList",
            "c0-id": f"0:{search_id}",
            "c0-param0": f"string:{script_session_id}",
            "c0-param1": f"string:{search_id}",
            "c0-param2": "string:H" if plaza_h else "string:N",
            "batchId": str(batch_id),
        }
