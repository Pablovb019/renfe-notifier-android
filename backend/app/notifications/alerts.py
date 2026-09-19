"""Construcción del payload de un aviso de plazas a partir del dominio.

Mapea un evento de la cola de avisos y su seguimiento al contrato ``AlertMessage``
que la app entiende (ver ``docs/notificaciones-fcm.md``).
"""

from app.followups.domain import FollowUp
from app.notifications.fcm import CHANNEL_ALERT, MESSAGE_TYPE_ALERT, AlertMessage
from app.reminders.domain import ReminderEvent
from app.renfe.stations import StationCatalog


def build_alert_message(
    *,
    event: ReminderEvent,
    followup: FollowUp,
    catalog: StationCatalog,
) -> AlertMessage:
    """Payload de alerta real con el contrato completo de la app."""
    origin = catalog.by_code(followup.origin_code)
    destination = catalog.by_code(followup.destination_code)
    origin_name = origin.name if origin else followup.origin_code
    destination_name = destination.name if destination else followup.destination_code
    return AlertMessage(
        type=MESSAGE_TYPE_ALERT,
        event_id=event.event_id,
        followup_id=followup.followup_id,
        episode_id=event.episode_id,
        title="Plazas disponibles",
        body=(
            f"Hay plazas disponibles en {origin_name} → {destination_name} "
            f"para el {followup.travel_date.isoformat()}."
        ),
        origin_code=followup.origin_code,
        origin=origin_name,
        destination_code=followup.destination_code,
        destination=destination_name,
        travel_date=followup.travel_date.isoformat(),
        observed_at=event.observed_at.isoformat(),
        expires_at=event.expires_at.isoformat(),
        channel_id=CHANNEL_ALERT,
        priority="high",
        collapse_key=f"followup:{followup.followup_id}",
    )
