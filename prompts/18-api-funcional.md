# Implementar la API autenticada

Añade endpoints de:
- Estaciones y búsquedas.
- Crear, listar, pausar, reanudar, renovar y eliminar seguimientos.
- Confirmar avisos.
- Registrar y renovar tokens FCM.
- Estadísticas y diagnóstico.
- Solicitud de notificación de prueba identificada como tal.

Valida fechas, estaciones, IDs, límites y permisos.
Impide URLs arbitrarias suministradas por el cliente.
Define respuestas de error coherentes y contratos Android/backend.

Prueba todos los endpoints con dependencias simuladas.
No consultes Renfe ni envíes FCM reales.