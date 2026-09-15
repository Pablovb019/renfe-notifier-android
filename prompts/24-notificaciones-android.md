# Recibir y mostrar notificaciones

Implementa:
- FCM y renovación del token.
- POST_NOTIFICATIONS en Android 13 y denegación.
- Canales, sonido, vibración y enlace a ajustes.
- Payload acordado con el backend.
- Primer y segundo plano sin duplicados.
- Deduplicación persistente adecuada de event_id.
- Caducidad y rechazo de eventos antiguos.
- Acciones: abrir, confirmar y pausar.

Las acciones deben autenticarse y gestionar fallos de red.
Usa trabajo diferido solo si es necesario para completar acciones:
no como vigilancia periódica cada 30 segundos.

No añadas polling, wake locks, WebSocket o servicio permanente.
Prueba lo disponible y compila; marca FCM real como pendiente.