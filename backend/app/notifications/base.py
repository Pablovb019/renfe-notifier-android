"""Interfaz del envío de notificaciones push.

El paso 19 implementará el envío real mediante Firebase Admin SDK / HTTP v1.
Para no acoplar la API de diagnóstico a esa implementación, aquí se define el
contrato mínimo (protocolo) que debe satisfacer el emisor FCM y su sustituto de
prueba.
"""

from __future__ import annotations

from typing import Protocol, runtime_checkable


class NotificationSendError(RuntimeError):
    """El proveedor de mensajería rechazó o no pudo enviar el mensaje."""


@runtime_checkable
class NotificationSender(Protocol):
    """Emisor de notificaciones push hacia un token FCM concreto."""

    async def send_test(self, *, fcm_token: str) -> str:
        """Envía una notificación de prueba y devuelve el identificador del mensaje."""
        ...
