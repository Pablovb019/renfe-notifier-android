# Bloque B - Inventario-3 final (cerrar búsqueda del bot antiguo)

> Documentar que no existen residuos del bot en la VM. Solo lectura.
> Pega este bloque completo y copia aquí la salida íntegra.

```bash
echo "=== A. Busqueda ampliada de repos/virtualenvs del bot en todo el FS ==="
sudo find / \( -path /proc -o -path /sys -o -path /dev -o -path /snap -o -path /usr \) -prune -o \( -iname '*renfe*bot*' -o -iname '*renfe_notifier_bot*' -o -iname '*telegram*' \) -print 2>/dev/null | head -40
echo
echo "=== B. Busqueda ampliada de BDs/volcados en todo el FS ==="
sudo find / \( -path /proc -o -path /sys -o -path /dev -o -path /usr \) -prune -o \( -iname '*.db' -o -iname '*.sqlite*' -o -iname '*.bak' \) -print 2>/dev/null | grep -iv '/renfe-notifier-android/backend' | head -40
echo
echo "=== C. Imagenes docker local (rastro del bot) ==="
sudo docker images 2>&1 | head -20
echo
echo "=== D. Units systemd relevantes (app/personalizadas, sin device) ==="
systemctl list-units --all --type=service --no-pager 2>&1 | grep -iE 'renfe|bot|telegram' || echo "(ninguna)"
ls -la /etc/systemd/system/*.service 2>/dev/null
echo
echo "=== E. /var/log rastros del bot ==="
sudo find /var/log -maxdepth 2 -iname '*renfe*' -o -maxdepth 2 -iname '*bot*' 2>/dev/null | head -20
echo
echo "=== F. Historial de comandos con renfe-bot (solo rutas/instrucciones, sin secretos) ==="
grep -hE 'renfe|bot' /home/pablovb01_gmail_com/.bash_history 2>/dev/null | grep -viE 'pass|token|key|secret' | head -30 || echo "(sin historia o sin coincidencias)"
```

## Qué confirmo yo con la salida

- **Cero residuos del bot** en todo el sistema (repo, venv, BD, volcados, logs, containers, units) → el paso §5bis de limpieza queda desplazado a "no aplica" y el §5 a "verificación sin objetos".
- Confirmar que **nada externo puede relanzar el bot** (único riesgo restante: `deploy.yml` del repo original, intervención del usuario §4/§10).
- Confirmar que la única BD de la VM es `/data/renfe_notifier.db` (backend nuevo) → el solo backup pendiente (P3) es el de §2.3.