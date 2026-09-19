"""Bucle de entrega de avisos de plazas ya detectados.

Consume los eventos debidos de ``AlertQueue``, construye el payload ``AlertMessage``
con el catálogo y envía a todos los dispositivos activos con token FCM. Casos:

- Sin sender configurado: el ciclo no procesa nada y queda pendiente.
- Seguimiento inactivo o caducado: el aviso se cancela.
- Token rechazado como inválido: se limpia el token del dispositivo.
- Error retryable de FCM (429/5xx): el aviso permanece pendiente y se reintenta.
- Error no retryable (401/403/400): el aviso se marca como fallido.
"""

import asyncio
import logging
from collections.abc import Awaitable, Callable
from datetime import UTC, datetime

from app.followups.database import FollowUpRepository
from app.followups.domain import Lifecycle
from app.notifications.alerts import build_alert_message
from app.notifications.fcm import FcmNotConfiguredError, FcmNotificationSender, FcmRejectedError
from app.pairing.service import PairingService
from app.reminders.domain import ReminderEvent
from app.reminders.queue import AlertQueue
from app.renfe.stations import StationCatalog

logger = logging.getLogger(__name__)

Clock = Callable[[], datetime]
Sleep = Callable[[float], Awaitable[None]]


class AlertDeliveryService:
    """Entrega avisos debidos mediante FCM; si algo falla, revertir en la cola.

    El lema es *al menos una vez*: un fallo retryable deja el aviso pendiente
    para que la cola lo reenvíe; solo un fallo permanente o un envío aceptado
    consume el intento.
    """

    def __init__(
        self,
        *,
        queue: AlertQueue,
        followups: FollowUpRepository,
        pairing: PairingService,
        catalog: StationCatalog,
        sender: FcmNotificationSender | None,
        clock: Clock = lambda: datetime.now(UTC),
        batch: int = 20,
        interval_s: float = 20.0,
        sleep: Sleep = asyncio.sleep,
    ) -> None:
        self._queue = queue
        self._followups = followups
        self._pairing = pairing
        self._catalog = catalog
        self._sender = sender
        self._clock = clock
        self._batch = batch
        self._interval_s = interval_s
        self._sleep = sleep

    async def run_once(self) -> int:
        """Procesa un lote de avisos debidos; devuelve cuántos se atendieron."""
        if self._sender is None:
            logger.debug("Sin sender FCM configurado; la entrega de avisos está desactivada.")
            return 0
        now = self._clock()
        events = self._queue.due_events(now, self._batch)
        if not events:
            return 0
        sender = self._sender
        assert sender is not None
        addressed = 0
        for event in events:
            if await self._deliver(event, sender, now):
                addressed += 1
        return addressed

    async def run_forever(self) -> None:
        """Bucle infinito idéntico al del planificador: un ciclo y después pausa."""
        while True:
            try:
                await self.run_once()
            except Exception as error:  # noqa: BLE001 - nunca romper el bucle de entrega
                logger.exception("Error en el ciclo de entrega de avisos: %s", error)
            await self._sleep(self._interval_s)

    async def _deliver(
        self,
        event: ReminderEvent,
        sender: FcmNotificationSender,
        now: datetime,
    ) -> bool:
        followup = self._followups.get(event.followup_id)
        if followup is None or followup.lifecycle is not Lifecycle.ACTIVE:
            self._queue.cancel_for_followup(event.followup_id, "stale")
            return True
        if now >= followup.expires_at or now >= event.expires_at:
            self._queue.cancel_for_followup(event.followup_id, "expired")
            return True

        devices = tuple(
            device
            for device in self._pairing.list_devices()
            if device.is_active and device.fcm_token
        )
        if not devices:
            logger.warning(
                "Aviso %s sin dispositivos activos con token FCM; queda pendiente.",
                event.event_id,
            )
            return False

        alert = build_alert_message(event=event, followup=followup, catalog=self._catalog)

        accepted = False
        token_invalid = False
        retryable = False
        for device in devices:
            fcm_token = device.fcm_token
            assert fcm_token is not None
            try:
                result = await sender.send_alert(token=fcm_token, alert=alert)
            except FcmRejectedError as error:
                if error.retryable:
                    retryable = True
                    logger.warning(
                        "FCM temporalmente no disponible para %s: %s", event.event_id, error.code
                    )
                else:
                    logger.error(
                        "FCM rechazó el aviso %s (no retryable): %s", event.event_id, error.code
                    )
                    self._queue.mark_failed(event.event_id, f"rejected:{error.code}")
                    return True
                continue
            except FcmNotConfiguredError as error:
                logger.error(
                    "FCM sin configurar; aviso %s queda pendiente: %s", event.event_id, error
                )
                return False
            if result.token_invalid:
                token_invalid = True
                self._pairing.clear_fcm_token(device.device_id)
                logger.info(
                    "Token FCM de %s invalidado; se limpió de la base de datos.",
                    device.device_id,
                )
            elif result.accepted:
                accepted = True
            elif result.error_code:
                logger.warning(
                    "FCM rechazó el envío del aviso %s: %s", event.event_id, result.error_code
                )

        if accepted:
            self._queue.deliver(event.event_id, now)
            return True
        if token_invalid and not retryable:
            self._queue.mark_failed(event.event_id, "token_invalid")
            return True
        return False
