# Procedimientos de Backup y Recuperación

> **Alcance**: Backup y restore de la base de datos SQLite (`renfe-notifier.db`) usada por el backend. **No toca la base del bot ni la VM de producción**. Usa bases temporales para pruebas.

## 1. Principios

- **WAL-aware**: el backup usa la API *online backup* de SQLite (`sqlite3.Connection.backup`), que copia el estado coherente aunque haya cambios pendientes en el WAL. No es una copia ciega del fichero `.db`.
- **Integridad**: antes de restaurar se verifica `PRAGMA integrity_check` y SHA256 del backup.
- **Migraciones controladas**: tras restaurar se ejecutan `apply_migrations()` para asegurar compatibilidad de esquema.
- **Datos fuera de releases**: la BD vive en `data/` (gitignored), no en el paquete desplegable.
- **Versiones por commit**: la tabla `schema_migrations` registra la versión aplicada; el CLI expone `version` para auditoría.
- **Rollback sin pérdida silenciosa**: el restore valida el backup antes de sobrescribir; si falla la verificación no se toca la BD original.

## 2. CLI (ejecutar dentro de la VM o en local contra una BD de prueba)

```bash
# Ver ayuda
python -m app.cli --help

# Backup (crea data/renfe-notifier.db.bak por defecto)
python -m app.cli backup [--output ruta/backup.db]

# Verificar integridad de la BD actual
python -m app.cli verify [--path ruta/bd.db]

# Verificar integridad de un backup antes de restaurar
python -m app.cli verify --path ruta/backup.db

# Ver versión de esquema
python -m app.cli version [--path ruta/bd.db]

# Restaurar (requiere --force si la BD destino ya existe)
python -m app.cli restore --input ruta/backup.db [--force]
```

**Salida de `backup`**: muestra ruta, SHA256 y versión de esquema. Guarda el SHA256 para auditar.

## 3. Pruebas de validación (fuera de producción)

```bash
# 1. Partir de BD limpia
rm -f data/renfe-notifier.db data/renfe-notifier.db-wal data/renfe-notifier.db-shm

# 2. Inicializar esquema (aplica migraciones)
python -m app.cli version   # debe mostrar la versión actual (p.ej. 5)

# 3. Poblar con datos de prueba (via API o tests)
#    ... ejecutar tests que insertan followups, episodios, alert_events, etc.

# 4. Backup
python -m app.cli backup --output /tmp/test_backup.db

# 5. Verificar backup
python -m app.cli verify --path /tmp/test_backup.db

# 6. Simular corrupción / pérdida
rm data/renfe-notifier.db

# 7. Restaurar
python -m app.cli restore --input /tmp/test_backup.db --force

# 8. Verificar integridad post-restore
python -m app.cli verify

# 9. Confirmar versión de esquema
python -m app.cli version
```

## 4. Integración en despliegue (ver `docs/ci-cd.md` §3.3)

En el workflow de despliegue backend, **antes** de actualizar código:

```bash
# Backup previo obligatorio (usa .backup atómico de SQLite)
TIMESTAMP=$(date +%Y%m%d_%H%M%S)
sqlite3 /data/renfe_notifier.db ".backup '/data/backups/backup_${TIMESTAMP}.db'"
```

Si la migración falla o el health check falla → rollback automático:
1. Restaurar BD desde backup (`sqlite3 /data/renfe_notifier.db ".restore '/data/backups/backup_XXXXX.db'"`)
2. Volver al commit anterior
3. Reiniciar servicio previo

## 5. Compatibilidad de esquema

- Las migraciones son **solo aditivas** (ADD COLUMN, nuevas tablas/índices). No se eliminan columnas ni tablas en migraciones normales.
- `apply_migrations` es idempotente: salta versiones ya aplicadas.
- La tabla `schema_migrations` es la fuente de verdad de la versión.
- Rollback de código **sin** rollback de BD es seguro si el código nuevo no requiere columnas que el código viejo no conoce (compatibilidad hacia adelante). Si se requiere rollback de BD, usar un backup previo al despliegue.

## 6. Retención y almacenamiento

- Backups automáticos en VM: `/data/backups/backup_YYYYMMDD_HHMMSS.db` (retención 7 días, cron diario).
- Backups manuales CLI: guardar fuera del servidor (descarga + almacenamiento seguro).
- Tamaño típico: < 5 MB (comprimido ~1 MB).

## 7. Checklist de validación (para PR o release)

- [ ] `python -m app.cli backup` crea archivo y muestra SHA256
- [ ] `python -m app.cli verify --path backup.db` → OK
- [ ] `python -m app.cli restore --input backup.db --force` en BD temporal → OK
- [ ] `python -m app.cli verify` post-restore → OK
- [ ] `python -m app.cli version` coincide con la versión esperada
- [ ] Tests unitarios pasan tras restore (`pytest backend/tests -q`)