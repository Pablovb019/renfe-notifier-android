# Plan de Transición: sustitución del bot por el backend nuevo

> **Estado**: DOCUMENTO DE PLAN. **No autoriza ninguna transición.**
> Ejecutar cualquier punto requiere aprobación explícita del usuario en cada
> paso (regla 01: "No despliegues ni detengas el bot sin autorización").
> No modifica el repositorio original `Pablovb019/renfe-notifier-bot` ni
> deshabilita nada por sí mismo; las acciones sobre el bot antiguo las ejecuta
> el usuario en la VM.

---

## 1. Revisión de resultados y bloqueos (antes de recomendar la transición)

### Qué está implementado y validado
- Backend nuevo desplegado en la VM (`renfe-notifier-backend`, uvicorn + systemd, `active`, salud `/api/v1/diagnostics/health` OK). IP estática `34.26.252.164`, puerto 8000.
  - **Proyecto GCP**: la VM vive en el proyecto **`renfe-notifier-bot`** (no `renfe-notifier-android`). Usar `--project renfe-notifier-bot` en los `gcloud compute` (ver docs/real-config-plan.md §2).
- BBDD nueva en `/data/renfe_notifier.db` (migraciones v1..v5), **vacía de seguimientos**: solo emparejamiento (`1 dispositivo` realme).
- FCM real funcionando end-to-end (canal `disponibilidad_plazas_v2`, prioridad alta, TTL 300 s) y matriz de entrega en realme **completa**: activo, bloqueado/pantalla apagada, Doze, reinicio, sin conexión→recuperación, forzar detención (no entrega: límite Android), ahorro de batería (publica en silencio). CI/CD verdes (backend-ci, android-ci, all-checks-ok, android-release v0.1.7).
- Consulta Renfe real controlada (escenario 1, paso 35): 5 POSTs DWR OK, 14 trenes reales, sin reservar nada.

### Bloqueo crítico que habilita la transición
- **El backend nuevo NO ejecuta aún el planificador ni entrega avisos reales**:
  - `SchedulerService.run_forever()` está implementado (`backend/app/scheduler/service.py:76`) pero **no está instanciado** en `main.py`.
  - `FcmNotificationSender.send_alert()` está implementado (`backend/app/notifications/fcm.py:233`) pero **no tiene llamadores**; solo `send_test` (diagnóstico/CLI).
  - `AlertQueue` existe y está cableado, pero `due_events` no se consume de forma continua y `create_followup` no encola eventos.
  - Coherente con la orden del paso 34 ("no actives sondeo del nuevo backend").
- **Consecuencia**: mientras no se cablee el planificador (tarea del **paso 37**, no de este), la transición de "avisos reales" no es posible. Este plan contempla ese cableado como prerrequisito del arranque, pendiente de aprobación.

---

## 2. Backup recuperable del bot y de SQLite

### 2.1 Inventario previo EN LA VM (requiere sesión del usuario)
```bash
# ¿Qué servicio/contenedor del bot antiguo está corriendo?
systemctl list-units --all | grep -i renfe
docker ps -a
docker compose ls
crontab -l
systemctl list-timers --all | grep -i renfe
ls -la ~/renfe-notifier-bot 2>/dev/null        # repo/bot antiguo en la home del SA
find /data /home -maxdepth 2 -iname '*.db' -o -iname '*.sqlite*' 2>/dev/null
```
Registrar: ubicación del repo antiguo, unidad/cron/timer que lo arranca, ruta de su SQLite y del `.env` antiguo.

> **Dato del usuario (2026-09-19)**: el bot antiguo **ya no está corriendo** en la VM,
> aunque se ejecutó durante un tiempo y es probable que existan **artefactos residuales**
> (repo, venv, compose, cron/timer, SQLite). Consecuencia para el plan: el §5 pasa de
> "detener el bot" a "verificar que nada lo relance", y se añade un paso nuevo de
> **limpieza por archivado** (§5bis) tras los backups.

### 2.2 Backups del bot antiguo (los ejecuta el usuario)
1. Copia **consistente** de su SQLite (parando el servicio o con `sqlite3 ruta.db ".backup backup_bot_YYYYMMDD_HHMMSS.db"`).
2. Verificar `PRAGMA integrity_check` del backup = `ok` y anotar SHA256.
3. Copiar el backup y el `.env` antiguo a almacenamiento externo (fuera de la VM). **El `.env` antiguo puede contener tokens de Telegram: nunca se sube a Git ni se expone en el chat.**

### 2.3 Backups del backend nuevo (ya existentes)
- `deploy_backend.sh` hace backup automático pre-despliegue en `/data/backups/backup_*.db` (`.backup` atómico + `integrity_check` + SHA256).
- CLI en la VM: `cd backend && .venv/bin/python -m app.cli backup` (salida: ruta, SHA256, versión de esquema).
- **Antes del corte (paso 37) se exige un backup nuevo del backend** y su descarga/guardado fuera de la VM.

---

## 3. Base nueva sin importar seguimientos
- **Decisión**: no se importa nada de la base del bot (queries de Telegram, contadores, historial). La base nueva permanece limpia; los seguimientos se recrean desde la app.
- Justificación: separa la semántica nueva (episodios/ávidos por evento, `alert_events`, prioridad/colisiones) del esquema antiguo. Nada de la app depende de datos históricos del bot.
- La app ya está emparejada (`device` en `devices` con token FCM); tras el corte se crean los seguimientos desde el móvil.

---

## 4. Automatizaciones antiguas que podrían sobrescribir el backend
Identificadas por la auditoría y los pasos de CI/CD:
1. **`.github/workflows/deploy.yml` del repositorio ORIGINAL** (`appleboy/ssh-action`, credenciales SSH estáticas `VM_SSH_KEY`): se dispara con cada `push` a `main` del repo antiguo y desplegaría el bot antiguo en la VM, **pisando el despliegue nuevo** (audit.md §3.8). **Requiere intervención del usuario** (ver §10).
2. **Contenedor/compose del bot antiguo** en la VM (si usa `docker compose`): `docker compose up --build` o un `restart` podría reocupar el puerto/proceso. Debe detenerse de forma controlada (§5).
3. **Cron/timer en la VM** que relance el bot o limpie datos. El inventario (§2.1) debe detectarlos.
4. **Credenciales SSH estáticas** (`VM_SSH_KEY`) en secrets del repo ORIGINAL: siguen dando acceso a la VM si el workflow sigue activo; al dejar de usar el repo original conviene revocarlas/guardar las nuevas solo en el repo nuevo.

**Importante**: ninguna de estas automatizaciones del repo original puede ser deshabilitada por este proyecto sin permiso explícito (regla 01/16).

---

## 5. Detención controlada del bot antiguo (EJECUTAR SOLO CON APROBACIÓN)

> **Ajuste 2026-09-19**: el bot antiguo **ya no está corriendo** en la VM. La "detención
> controlada" se interpreta como **verificación de que nada lo relance** (units, cron,
> timers, docker/compose, y el `deploy.yml` del repo original, ver §4). Si el inventario
> (§2.1) revelara todavía un proceso activo, aplicar la orden siguiente de detención real.

Orden propuesto (en la VM, usuario):
1. Verificada la integridad de los backups (§2.2).
2. Detener el servicio/timer/cron identificado (§2.1 y §4). Anotar el comando exacto de **rearme** para restauración rápida (p. ej. `systemctl start <unidad>` o el comando del cron/compose).
3. Confirmar que el proceso del bot ha terminado (`pgrep -f renfe-bot` vacío; contenedor `docker compose ps` sin bot).
4. **No** se elimina ni toca el repositorio del bot antiguo en la VM: se conserva intacto para rollback.
5. Registrar en el aviso de transición la unidad/comando detenido, su estado y el comando de rearme.

## 5bis. Limpieza de artefactos residuales del bot antiguo (NUEVO, SOLO CON APROBACIÓN)

> Derivado del dato del usuario (2026-09-19): el bot ya no corre, pero se ejecutó en la VM
> durante un tiempo. Tras backups verificados y descargados fuera de la VM (P3 cumplido),
> se **archiva** (no se destruye) lo residual para dejar la VM únicamente con el backend nuevo.

1. Con los backups del §2.2 verificados y **fuera de la VM**, identificar residuos del inventario:
   - repo `~/renfe-notifier-bot` (o ruta que indique el inventario) y su `.env` antiguo,
   - unidades/cron/timers/compose que ya no apliquen (a desactivar/comentar, registrando rearme),
   - SQLite y volcados/backups intermedios.
2. Empaquetar/archivar lo residual (p. ej. `tar` del repo y BD) en `/data/archive/` con SHA256 anotado; **el `.env` antiguo nunca se archiva dentro del paquete** (puede contener tokens de Telegram; se guarda por separado fuera de la VM).
3. Desactivar/comentar lo que pudiera relanzar el bot (cron/timer/unit/compose) SOLO con permiso explícito; registrar el comando de rearme de cada uno.
4. No se modifica el repositorio Git original `Pablovb019/renfe-notifier-bot` ni sus workflows/secrets (reglas 01/16); esas intervenciones son del §10.
5. Resultado esperado: `ps aux | grep -i renfe` sin procesos del bot, cron/timer sin entradas renfe salvo las del backend nuevo, y ningún proceso capaz de relanzar el bot.

---

## 6. Arranque del backend nuevo (prerrequisitos y orden)
1. **Cablear el planificador y entrega real de avisos (paso 37, pendiente)**: instanciar `SchedulerService` en el servicio principal (bucle `run_forever` con intervalo configurado), consumir `AlertQueue.due_events` para entregar con `FcmNotificationSender.send_alert`, y encolar en `create_followup`. Sin esto, no hay "avisos reales".
2. `systemctl restart renfe-notifier-backend` y verificar: `systemctl is-active` → `active`, health 200, y el servicio arranca **una única vez** (un solo propietario del sondeo; sin duplicados).
3. Verificación de emparejamiento y token FCM vigente (`app.cli devices --list` en la VM, sin exponer tokens).
4. Desde la app: crear un seguimiento de prueba y confirmar el flujo completo (detección → episodio → `alert_events` → FCM → notificación con acciones en el realme).

---

## 7. Consultas, avisos reales y medición de recursos
- **Consulta real controlada**: una búsqueda real autorizada (como el escenario 1 del paso 35) para validar el motor en producción; **sin reservar ni comprar billetes** y con las mismas métricas por fase (número de peticiones, bytes, duración).
- **Aviso real**: solo si se obtiene disponibilidad real; no se fuerza un aviso falso. El aviso se entrega por el canal v2; la app muestra la notificación con acciones.
- **Medición de recursos**: `scripts/measure_resources.py` en la VM (con BD temporal en `/tmp`, nunca `/data/renfe_notifier.db`) para confirmar holgura en la e2-micro (objetivo: RAM < 100 MB, sin picos sostenidos de CPU; texto de referencia `docs/measurements.json`).
- Registrar memoria/CPU con el planificador activo y compararlo con la línea base ya medida (~48 MB, CPU 9–15%).

---

## 8. Condiciones de fallo y restauración inmediata del bot
Definir criterios de fallo (vigilables durante los primeros días):
- Health check `GET /api/v1/diagnostics/health` no 200 en 30 s tras el arranque.
- FCM: sucesión de `token_invalid` o errores 4xx/5xx persistentes en los envíos (revisar `journalctl -u renfe-notifier-backend`).
- Errores 5xx del planificador o saturación de RAM/CPU sostenida en la e2-micro.
- Fallos de consulta a Renfe que deriven en avisos silenciosamente perdidos (check del diario/seguimiento en la app).

**Procedimiento de restauración inmediata (rollback) del bot antiguo**
1. Detener el backend nuevo (`sudo systemctl stop renfe-notifier-backend`).
2. Restablecer el bot antiguo con el comando de rearme registrado (§5).
3. Restaurar la SQLite del bot con el backup verificado (§2.2) **solo si fue modificada durante la ventana** (durante esta transición no se debe tocar).
4. Redesplegar el último commit CI-verde del backend nuevo queda para reintentarlo a demanda.

---

## 9. Protección de backups y clave de firma
- **Backups**: copias fuera de la VM (descarga local/USB encriptado + gestor de contraseñas), con SHA256 anotado. Nunca en el repositorio (`.gitignore` cubre `*.db`, `*.bak`).
- **Clave de firma**: `keystore.jks` + passwords en secrets GitHub (`ANDROID_KEYSTORE_BASE64`, `KEYSTORE_PASSWORD`, `KEY_ALIAS`, `KEY_PASSWORD`) y **backup físico en 2+ ubicaciones** (ver `docs/keystore.md`). La pérdida de la clave impide actualizar el APK sobre la firma actual.
- **`.env` de producción** (contiene `SECRET_KEY`, `FCM_PROJECT_ID`): solo en la VM, nunca en Git; clave regenerable.
- **`google-services.json`**: solo en secret (`ANDROID_GOOGLE_SERVICES_JSON_B64`), decodificado durante el build y eliminado con `shred`; nunca en el repo.

---

## 10. Automatización antigua que necesita intervención (documentar al usuario)
| Automatización | Riesgo si se ignora | Intervención requerida (la ejecuta el usuario) |
|---|---|---|
| `deploy.yml` del repo ORIGINAL (push→SSH→compose) | Pisa el backend nuevo en cada push al repo antiguo | Deshabilitar/archivar el workflow o bloquear el push a `main` en el repo original; revocar `VM_SSH_KEY` al migrar |
| Docker/compose del bot en la VM | Puede relanzar el bot y duplicar sondeo | Detención controlada (§5) antes del corte |
| Cron/timer de la VM que relance el bot | Relanza el bot tras el corte | Identificarlo en el inventario (§2.1) y comentarlo/eliminarlo SOLO con permiso explícito |
| `VM_SSH_KEY` en secrets del repo original | Acceso permanente a la VM por SSH estático | Revocar la clave del usuario del SA y rotar el acceso a IAP exclusivamente |

**No mantener sondeo duplicado durante días**: la orden de arranque exige detener primero el bot antiguo (§5) y arrancar después el planificador del backend nuevo (§6). Durante la ventana de validación (primeros 1–2 días) solo corre el backend nuevo; el bot antiguo se conserva parado y reproducible.

---

## 11. Aprobación explícita
Este documento **no autoriza** la transición. Se requiere aprobación explícita del usuario para:
1. Ejecutar el inventario y backups en la VM (§2).
2. Intervenir en el repositorio original o en sus workflows/secrets (§4, §10).
3. Detener el bot antiguo (§5) y limpiar/archivar residuos de cuando se ejecutó (§5bis).
4. Cablear y arrancar el planificador del backend nuevo (§6) — objeto del paso 37.
5. Consultas/avisos reales y mediciones (§7).

Cada bloque puede aprobarse de forma separada. Tras cada bloque se actualizará `PROGRESS.md` con evidencia.