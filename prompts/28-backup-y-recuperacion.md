# Implementar herramientas de recuperación

Implementa y prueba fuera de producción:
- Backup coherente de SQLite en uso, considerando WAL.
- Verificación de integridad del backup.
- Restauración.
- Migraciones controladas.
- Versiones identificadas por commit.
- Datos persistentes fuera del directorio de releases.
- Compatibilidad entre esquema y versión anterior.
- Rollback que no pierda datos silenciosamente.

Usa bases temporales.
No toques la base del bot ni la VM.
Documenta instrucciones en docs/recovery.md.