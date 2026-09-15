# Implementar planificación de consultas

Implementa:
- Un único propietario del planificador.
- Ninguna consulta sin seguimientos activos.
- Agrupación por todos los parámetros que afecten la respuesta.
- Una búsqueda para varios trenes compatibles.
- Concurrencia global inicial de 1 o 2.
- Exclusión de comprobaciones simultáneas del mismo grupo.
- Sin acumulación de tareas atrasadas.
- Recuperación tras reinicio.
- Aislamiento de sesiones concurrentes.

Continúa comprobando aunque ya se haya enviado un aviso.
Evalúa compartir búsquedas manuales equivalentes sin alterar semántica.

Prueba con reloj y cliente simulados.
Separa comprobaciones lógicas de peticiones HTTP.