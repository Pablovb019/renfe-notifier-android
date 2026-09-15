# Implementar transporte FCM

Implementa envío desde servidor mediante Admin SDK o HTTP v1.

Diseña explícitamente:
- Payload data/notification y comportamiento esperado.
- Aviso visible sin una consulta adicional.
- Prioridad alta solo para contenido urgente visible.
- TTL breve.
- Invalidación de tokens.
- Reintentos y deduplicación mediante la cola existente.
- Separación entre aceptado por FCM y realmente mostrado.

Preferencia: identidad de la VM con permisos mínimos si es viable.
No leas credenciales en el contexto del modelo.

Prueba con un emisor simulado.
Documenta configuración pendiente, sin crear recursos ni enviar mensajes.