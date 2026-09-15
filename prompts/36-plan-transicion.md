# Preparar sustitución y rollback

Revisa resultados y bloqueos antes de recomendar la transición.
Incluye:
- Backup recuperable del bot y SQLite.
- Base nueva sin importar seguimientos.
- Automatizaciones antiguas que podrían sobrescribir el backend.
- Detención controlada del bot.
- Arranque del backend nuevo.
- Consultas, avisos reales y medición de recursos.
- Condiciones de fallo y restauración inmediata del bot.
- Protección de backups y clave de firma.

No mantengas sondeo duplicado durante días.
Documenta qué automatización antigua necesita intervención.
No cambies el repositorio original ni deshabilites nada sin permiso.

Solicita aprobación explícita.
Este archivo NO autoriza la transición.