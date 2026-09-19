# Bloque B - Inventario-2 en VM (localizar bot antiguo y residuos)

> Continúa del inventario (§2.1). Solo lectura con `sudo` donde hace falta.
> Pega este bloque completo y copia aquí la salida íntegra.

```bash
echo "=== A. Homes de otros usuarios ==="
for u in ubuntu pablovb01_gmail_com; do
  echo "--- /home/$u ---"
  sudo ls -la /home/$u 2>&1
done
echo
echo "=== B. Busqueda de residuos del bot (db, sqlite, .env, docker-compose, *renfe*) ==="
sudo find /home /root /srv /opt -maxdepth 4 \( -iname '*.db' -o -iname '*.sqlite*' -o -iname '.env*' -o -iname 'docker-compose*.yml' -o -iname '*renfe*' -o -iname '*bot*' \) 2>&1 | grep -v "Permission denied" | head -60
echo
echo "=== C. Docker (con sudo) ==="
sudo docker ps -a 2>&1 | head -20
sudo docker compose ls 2>&1
echo
echo "=== D. Crontabs de todos los usuarios ==="
for u in root ubuntu pablovb01_gmail_com sa_104384329745603569192; do
  echo "--- crontab $u ---"
  sudo crontab -u $u -l 2>&1 || echo "(vacio o sin acceso)"
done
echo
echo "=== E. Timers systemd (todos, ampliando) ==="
systemctl list-timers --all --no-pager 2>&1 | head -30
echo
echo "=== F. Procesos no backend (bot/telegram/python ajeno) ==="
ps aux | grep -iE 'bot|telegram' | grep -v grep || echo "(ninguno)"
ps aux | grep -E 'python3? ' | grep -v uvicorn | grep -v grep || echo "(solo uvicorn)"
echo
echo "=== G. /data y otros mounts relevantes ==="
ls -la /data 2>&1
sudo du -sh /data/. 2>/dev/null
echo
echo "=== H. Grupos de docker del usuario SA ==="
id
```

## Qué confirmo yo con la salida

- **Ubicación del bot antiguo**: home (p. ej. `pablovb01_gmail_com`), repo y SQLite, `.env` antiguo (solo ruta), compose y volcados.
- **Si docker/cron/timers existen o no** para el plan de limpieza/archivado (§5bis).
- **Qué proceso python corre además del backend** (si aparece algo, no tocar: avísame y lo analizamos antes de nada).

## Nota de seguridad (plan §2.2)

Si se localiza `.env` antiguo: no imprimir su contenido nunca. Solo la ruta.