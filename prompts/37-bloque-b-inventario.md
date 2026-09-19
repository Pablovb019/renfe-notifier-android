# Bloque B - Inventario en VM (§2.1)

> Pega este bloque completo en la terminal de la VM y copia aquí la salida íntegra.
> Los comandos son de SOLO LECTURA: no modifican nada.

```bash
echo "=== sistema ==="
hostname; uname -a; date -u
echo
echo "=== 1. Servicios/units systemd con renfe ==="
systemctl list-units --all | grep -i renfe || echo "(sin units renfe)"
echo "  --- estado servicio backend nuevo ---"
systemctl is-active renfe-notifier-backend 2>&1
systemctl status renfe-notifier-backend --no-pager -n 0 2>&1 | head -5
echo
echo "=== 2. Contenedores docker ==="
docker ps -a 2>&1 || echo "(no docker o sin permisos)"
docker compose ls 2>&1 || echo "(no compose)"
echo
echo "=== 3. Crontab ==="
crontab -l 2>&1 || echo "(sin crontab)"
echo
echo "=== 4. Timers systemd ==="
systemctl list-timers --all | grep -i renfe || echo "(sin timers renfe)"
echo
echo "=== 5. Repo bot antiguo en home del SA ==="
ls -la ~/renfe-notifier-bot 2>/dev/null || echo "(no existe ~/renfe-notifier-bot)"
ls -la /home 2>&1
echo
echo "=== 6. Bases de datos (bot antiguo y sqlite/backups) ==="
find /data /home -maxdepth 2 \( -iname '*.db' -o -iname '*.sqlite*' -o -iname '*.bak' \) 2>/dev/null || echo "(nada encontrado)"
echo
echo "=== 7. Backups del backend nuevo ==="
ls -la /data/backups/ 2>/dev/null || echo "(no existe /data/backups)"
echo
echo "=== 8. .env / compose del bot antiguo (rutas, SIN imprimir contenido) ==="
find /home /data /opt -maxdepth 2 -iname '.env*' -o -maxdepth 2 -iname 'docker-compose*.yml' 2>/dev/null
echo
echo "=== 9. Procesos con renfe en marcha ==="
ps aux | grep -i renfe | grep -v grep || echo "(ninguno)"
```

## Qué registrar de la salida (para PROGRESS)

- **Dato del usuario**: el bot antiguo **ya no está corriendo** en la VM (se ejecutó durante un tiempo). El inventario debe detectar **residuos**: repo, venv, compose, cron/timer, SQLite, y confirmar que `ps` no muestra procesos del bot.
- Unidad/cron/timer que arrancaba el bot antiguo y su **comando de rearme**.
- Ubicación del repo antiguo, de su SQLite y del `.env`/compose antiguos.
- Observación explícita: "el `.env` antiguo puede contener tokens de Telegram; no se copia su contenido aquí ni en chat".
- Estado del backend nuevo (`active` esperado) y listado de `/data/backups/`.
- Cualquier artefacto residual candidato a **archivado** (§5bis del plan) para limpiar la VM.

## Siguiente paso del bloque

Con el inventario identificado, se ejecuta §2.2 (backup consistente del bot antiguo con `.backup` + `integrity_check` + SHA256) y §2.3 (backup nuevo del backend con `app.cli backup`), y **descarga fuera de la VM** de ambos (P3).