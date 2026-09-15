"""Transporte de mensajería push mediante Firebase Cloud Messaging HTTP v1.

Solo se envían *data payload* completos (sin bloque ``notification``): la app
los muestra como notificación nativa, sin consulta adicional de red. La
autenticación usa OAuth2 vía Application Default Credentials; en la VM se
resuelve automáticamente con la identidad de la instancia (permisos mínimos) y
en local mediante un archivo de cuenta de servicio (``GOOGLE_APPLICATION_CREDENTIALS``
y `pip install google-auth`, opcional). Ninguna credencial se lee en el contexto
del modelo ni viaja por el API: solo se intercambia un access token OAuth2 efímero.

Los reintentos no viven aquí: la cola de avisos existente (``AlertQueue``)
acota intentos y deduplica por ``event_id``. Este módulo es idempotente por
mensaje y nunca reintenta por su cuenta.
"""

from dataclasses import dataclass
from typing import Literal, Protocol

import httpx

from app.notifications.base import NotificationSendError

FCM_ENDPOINT = "https://fcm.googleapis.com/v1/projects/{project_id}/messages:send"
FCM_SEND_SCOPE = "https://www.googleapis.com/auth/firebase.messaging"

GCE_METADATA_URL = (
    "http://metadata.google.internal/computeMetadata/v1/instance/service-accounts/default/token"
)
METADATA_FLAVOR_HEADER = "Metadata-Flavor"

MAX_TTL_S = 2_419_200  # 4 semanas, tope documentado de FCM
MAX_DATA_BYTES = 4096

Priority = Literal["high", "normal"]

# Claves del payload y su semántica (contrato con la app, ver docs/notificaciones-fcm.md).
MESSAGE_TYPE_ALERT = "alert"
MESSAGE_TYPE_TEST = "test"
CHANNEL_ALERT = "disponibilidad_plazas"
CHANNEL_SERVICE = "resumen_y_servicio"


class FcmPayloadError(ValueError):
    """Payload inválido según las restricciones documentadas de FCM."""


class FcmNotConfiguredError(RuntimeError):
    """Falta configuración (proyecto o credenciales) para poder enviar."""


class FcmRejectedError(NotificationSendError):
    """FCM respondió un error; no se considera entrega ni cancelación local."""

    def __init__(
        self,
        *,
        code: str,
        message: str,
        token_invalid: bool = False,
        retryable: bool = False,
    ) -> None:
        self.code = code
        self.token_invalid = token_invalid
        self.retryable = retryable
        super().__init__(message)


@dataclass(frozen=True, slots=True)
class FcmResult:
    """Resultado de un envío: separa 'aceptado por FCM' de 'entregado visto'."""

    accepted: bool
    message_id: str | None = None
    token_invalid: bool = False
    error_code: str | None = None


@dataclass(frozen=True, slots=True)
class FcmMessage:
    """Mensaje data-only con prioridad y TTL explícitos."""

    token: str
    data: dict[str, str]
    priority: Priority = "high"
    ttl_s: int = 300
    collapse_key: str | None = None
    package: str | None = None

    def validate(self) -> None:
        if self.token and len(self.token) > 4096:
            raise FcmPayloadError("Token FCM demasiado largo")
        if not self.token:
            raise FcmPayloadError("Token FCM vacío")
        if self.ttl_s < 0 or self.ttl_s > MAX_TTL_S:
            raise FcmPayloadError("TTL fuera del rango permitido")
        total = 0
        for key, value in self.data.items():
            if not (1 <= len(key) <= 150):
                raise FcmPayloadError("Clave de datos fuera del rango permitido")
            if not key.replace("-", "").replace("_", "").isalnum():
                raise FcmPayloadError("Clave de datos con caracteres no permitidos")
            if len(value) > 1024:
                raise FcmPayloadError("Valor de datos demasiado largo")
            total += len(key) + len(value)
        if total > MAX_DATA_BYTES:
            raise FcmPayloadError("Payload de datos excede el máximo de FCM")


class FcmTokenProvider(Protocol):
    """Proveedor de access tokens OAuth2 efímeros para la API HTTP v1."""

    async def access_token(self) -> str: ...


class StaticTokenProvider:
    """Token fijo inyectable; ideal para pruebas y desarrollo puntual."""

    def __init__(self, token: str) -> None:
        self._token = token

    async def access_token(self) -> str:
        return self._token


class GceMetadataTokenProvider:
    """Token de identidad de la VM sin credenciales en el repositorio."""

    def __init__(
        self,
        *,
        timeout_s: float = 2.0,
        transport: httpx.AsyncBaseTransport | None = None,
    ) -> None:
        self._timeout_s = timeout_s
        self._transport = transport

    async def access_token(self) -> str:
        try:
            async with httpx.AsyncClient(
                timeout=self._timeout_s, transport=self._transport
            ) as session:
                response = await session.get(
                    GCE_METADATA_URL,
                    headers={METADATA_FLAVOR_HEADER: "Google"},
                )
        except httpx.HTTPError as error:
            raise FcmNotConfiguredError(
                "No se pudo contactar con el servidor de metadatos de GCE"
            ) from error
        if response.status_code != 200:
            raise FcmNotConfiguredError("El servidor de metadatos de GCE no devolvió un token")
        payload = response.json()
        token = payload.get("access_token")
        if not isinstance(token, str) or not token:
            raise FcmNotConfiguredError("El servidor de metadatos de GCE no aportó access_token")
        return token


async def _google_auth_token() -> str:
    """Resuelve ADC (identidad de VM o cuenta de servicio local) vía google-auth."""
    try:
        import google.auth
        import google.auth.transport.requests
    except ImportError:
        raise FcmNotConfiguredError("google-auth no está instalado") from None
    try:
        credentials, _project = google.auth.default(scopes=[FCM_SEND_SCOPE])
        request = google.auth.transport.requests.Request()
        credentials.refresh(request)
        token = getattr(credentials, "token", None)
    except Exception as error:  # noqa: BLE001 - fiabilidad ante ADC ausente en local/otros entornos
        raise FcmNotConfiguredError("Application Default Credentials no disponibles") from error
    if not isinstance(token, str) or not token:
        raise FcmNotConfiguredError("ADC no aportó access token")
    return token


class AutoTokenProvider:
    """Prefiere la identidad de la VM (google-auth/ADC) y cae al metadato GCE."""

    def __init__(self, metadata_provider: FcmTokenProvider | None = None) -> None:
        self._metadata_provider = metadata_provider or GceMetadataTokenProvider()

    async def access_token(self) -> str:
        try:
            return await _google_auth_token()
        except FcmNotConfiguredError:
            pass
        return await self._metadata_provider.access_token()


class FcmNotificationSender:
    """Envía mensajes data-only a la API HTTP v1 de Firebase."""

    def __init__(
        self,
        *,
        project_id: str,
        token_provider: FcmTokenProvider,
        timeout_s: float = 10.0,
        default_ttl_s: int = 300,
        package: str | None = None,
        transport: httpx.AsyncBaseTransport | None = None,
    ) -> None:
        if not project_id or not project_id.strip():
            raise ValueError("project_id es obligatorio")
        if default_ttl_s < 0 or default_ttl_s > MAX_TTL_S:
            raise ValueError("default_ttl_s fuera de rango")
        self._project_id = project_id.strip()
        self._token_provider = token_provider
        self._timeout_s = timeout_s
        self._default_ttl_s = default_ttl_s
        self._package = package
        self._transport = transport

    async def send(self, message: FcmMessage) -> FcmResult:
        """Envía un mensaje; devuelve el resultado sin reintentos internos."""
        message.validate()
        token = await self._token_provider.access_token()
        body = self._build_body(message)
        async with httpx.AsyncClient(timeout=self._timeout_s, transport=self._transport) as session:
            response = await session.post(
                FCM_ENDPOINT.format(project_id=self._project_id),
                json=body,
                headers={
                    "Authorization": f"Bearer {token}",
                    "Content-Type": "application/json",
                },
            )
        return self._interpret(response)

    async def send_alert(self, *, token: str, alert: "AlertMessage") -> FcmResult:
        """Conveniencia para el bucle de entrega: prioridad alta y TTL corto."""
        ttl = alert.ttl_s if alert.ttl_s is not None else self._default_ttl_s
        message = FcmMessage(
            token=token,
            data=alert.as_data(),
            priority=alert.priority,
            ttl_s=ttl,
            collapse_key=alert.collapse_key,
            package=self._package,
        )
        return await self.send(message)

    async def send_test(self, *, fcm_token: str) -> str:
        """Implementa el protocolo de diagnóstico; devuelve el id de FCM."""
        alert = build_test_alert(
            token=fcm_token,
            ttl_s=self._default_ttl_s,
        )
        result = await self.send(
            FcmMessage(
                token=fcm_token,
                data=alert.as_data(),
                priority="high",
                ttl_s=alert.ttl_s or self._default_ttl_s,
                collapse_key=None,
                package=self._package,
            )
        )
        if result.token_invalid:
            raise FcmRejectedError(
                code=result.error_code or "UNREGISTERED",
                message="El token FCM del dispositivo ya no es válido",
                token_invalid=True,
            )
        if result.message_id is None:
            raise FcmRejectedError(
                code="NO_MESSAGE_ID",
                message="FCM aceptó la petición pero no devolvió identificador",
            )
        return result.message_id

    def _build_body(self, message: FcmMessage) -> dict[str, object]:
        android: dict[str, object] = {
            "priority": message.priority,
            "ttl": f"{message.ttl_s}s",
        }
        if message.collapse_key:
            android["collapse_key"] = message.collapse_key
        package = message.package or self._package
        if package:
            android["restricted_package_name"] = package
        return {
            "message": {
                "token": message.token,
                "data": message.data,
                "android": android,
            }
        }

    @staticmethod
    def _interpret(response: httpx.Response) -> FcmResult:
        payload = _json_object(response)
        if response.status_code == 200:
            name = payload.get("name")
            return FcmResult(
                accepted=True,
                message_id=name if isinstance(name, str) else None,
            )
        error = payload.get("error")
        if isinstance(error, dict):
            code = str(error.get("status") or f"HTTP_{response.status_code}")
            detail = str(error.get("message") or "FCM rechazó la petición")
        else:
            code = f"HTTP_{response.status_code}"
            detail = "FCM rechazó la petición"
        token_invalid = (
            response.status_code == 404
            or code == "UNREGISTERED"
            or ("registration token" in detail.lower())
        )
        if response.status_code in (401, 403):
            raise FcmRejectedError(code=code, message=detail, retryable=False)
        if response.status_code == 429 or response.status_code >= 500:
            raise FcmRejectedError(code=code, message=detail, retryable=True)
        if token_invalid:
            return FcmResult(accepted=False, token_invalid=True, error_code=code)
        raise FcmRejectedError(code=code, message=detail)


def _json_object(response: httpx.Response) -> dict[str, object]:
    if not response.text:
        return {}
    try:
        payload = response.json()
    except ValueError:
        return {}
    return payload if isinstance(payload, dict) else {}


@dataclass(frozen=True, slots=True)
class AlertMessage:
    """Payload tipado de aviso de plazas o de prueba, serializable a data."""

    type: str
    event_id: str
    followup_id: str
    episode_id: int
    title: str
    body: str
    origin_code: str
    origin: str
    destination_code: str
    destination: str
    travel_date: str
    observed_at: str
    expires_at: str
    channel_id: str
    departure: str | None = None
    arrival: str | None = None
    price: str | None = None
    priority: Priority = "high"
    ttl_s: int | None = None
    collapse_key: str | None = None

    def as_data(self) -> dict[str, str]:
        data: dict[str, str] = {
            "type": self.type,
            "event_id": self.event_id,
            "followup_id": self.followup_id,
            "episode_id": str(self.episode_id),
            "title": self.title,
            "body": self.body,
            "origin_code": self.origin_code,
            "origin": self.origin,
            "destination_code": self.destination_code,
            "destination": self.destination,
            "travel_date": self.travel_date,
            "observed_at": self.observed_at,
            "expires_at": self.expires_at,
            "channel_id": self.channel_id,
            "priority": self.priority,
        }
        for key, value in (
            ("departure", self.departure),
            ("arrival", self.arrival),
            ("price", self.price),
        ):
            if value:
                data[key] = value
        return data


def build_test_alert(
    *,
    token: str,
    ttl_s: int = 300,
    followup_id: str = "-",
    event_id: str = "test",
) -> AlertMessage:
    """Notificación de prueba visible e identificada como tal."""
    title = "Test de conexión"
    body = "Si ves esta notificación, la app recibe correctamente los avisos de plazas."
    if not token:
        raise FcmPayloadError("Token FCM vacío")
    return AlertMessage(
        type=MESSAGE_TYPE_TEST,
        event_id=event_id,
        followup_id=followup_id,
        episode_id=0,
        title=title,
        body=body,
        origin_code="-",
        origin="-",
        destination_code="-",
        destination="-",
        travel_date="-",
        observed_at="-",
        expires_at="-",
        channel_id=CHANNEL_ALERT,
        priority="high",
        ttl_s=ttl_s,
    )
