"""Paquete de notificaciones push (FCM) del servidor."""

from app.notifications.base import NotificationSender, NotificationSendError

__all__ = ["NotificationSendError", "NotificationSender"]
