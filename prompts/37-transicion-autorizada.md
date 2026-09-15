# Ejecutar transición aprobada

Antes de actuar, verifica:
- Aprobación explícita del plan de transición.
- Ausencia de bloqueos de seguridad o coste.
- Backup consistente y procedimiento de recuperación comprobado.
- Commit exacto autorizado con CI superada.
- APK instalado y configuración necesaria validada.

Si falta algo, detente.

Ejecuta exclusivamente la transición aprobada.
No destruyas el bot anterior ni elimines backups.
Ante fallo, aplica el rollback previsto y comunica el estado.

Verifica consultas, notificaciones reales y recursos en la VM.
No declares el reemplazo terminado sin validar avisos en el teléfono.