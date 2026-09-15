# Preparar CI del backend

Precondición: cuotas y prevención de gasto revisadas.

Crea el workflow del backend:
- Pull requests y main.
- Runner Linux.
- Análisis estático y tests.
- Detección de cambios relevantes.
- Sin checks obligatorios bloqueados por filtros.
- Timeout, caché controlada y cancelación de CI obsoleta.
- Permisos mínimos y acciones fijadas por SHA.
- Sin secretos, Renfe ni producción.

Valida sintaxis y ejecuta localmente las comprobaciones disponibles.
No hagas push ni habilites ejecución remota sin aprobación.