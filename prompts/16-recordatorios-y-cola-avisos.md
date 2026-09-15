# Persistir y programar avisos

Implementa una cola persistente en SQLite, sin Redis ni Celery.

Incluye:
- Eventos asociados a episodios concretos.
- event_id, episode_id, observed_at y caducidad.
- Recordatorios configurables y acotados.
- Cancelación al confirmar, pausar, vencer o desaparecer las plazas.
- Suspensión de recordatorios cuando la disponibilidad esté obsoleta.
- Reintentos acotados e idempotencia.
- Recuperación después de reinicio.

No prometas entrega exactamente una vez.
No envíes FCM todavía.
Prueba duplicados, errores y cancelaciones.