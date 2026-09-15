# Preparar CI de Android

Crea CI con:
- Versiones compatibles con compilación local.
- Análisis estático, tests unitarios y APK debug.
- Filtros coherentes para cambios Android y compartidos.
- Sin emuladores ni matrices grandes en cada commit.
- Caché y retención corta de APK.
- Permisos mínimos y acciones fijadas por SHA.
- Sin claves de firma de distribución ni credenciales de servidor.

La compilación debug debe funcionar sin secretos de producción.
Documenta cómo se proporciona configuración Firebase cuando corresponda.

Valida y registra pruebas.
No hagas push sin aprobación.