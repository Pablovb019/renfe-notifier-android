# Progreso del Proyecto

## Paso 13 - Modelo de seguimientos (13-modelo-seguimientos.md) COMPLETADO

### Estado
**Implementado y probado localmente**

### Decisiones adoptadas
- Implementado modelo de dominio puro en backend/app/followups/domain.py sin dependencias del planificador
- Definidos enums: Lifecycle, AvailabilityState, AlertState, FollowUpMode, ObservationKind
- Clases principales: TrainSnapshot, Observation, FollowUp, ObservationResult
- Funciones de utilidad para zona horaria: as_utc, departure_at_utc, arrival_at_utc, end_of_travel_day_utc, initial_expiry
- Reglas implementadas:
  - Nueva aparicion valida de plazas crea nuevo episodio
  - Errores y datos obsoletos no crean episodios
  - Confirmacion (acknowledge) sin borrar el seguimiento
  - Pausa, eliminacion y renovacion como transiciones independientes
  - Caducidad inicial de 30 dias limitada por la salida (o fin de dia para modo ALL)
  - Comportamiento de primero, ultimo y todos
  - Nuevos trenes dentro de todos generan episodios
  - Final del dia y llegadas al dia siguiente manejadas con zona horaria Europe/Madrid
  - ID real preferido, fallback documentado compuesto
  - UTC para instantes, Europe/Madrid para fechas de viaje

### Archivos creados/modificados
- backend/app/followups/__init__.py (existia)
- backend/app/followups/domain.py (implementado completo)
- backend/tests/test_followup_domain.py (tests existentes - 10 tests pasando)

### Pruebas ejecutadas y resultados
- 10 tests en test_followup_domain.py - todos pasan
- 22 tests totales del backend - todos pasan
- Ruff lint - sin errores (corregido lambda a def)
- Tests cubren:
  - Ciclo de vida: disponibilidad -> acknowledge -> reaparece -> nuevo episodio
  - ERROR y STALE no crean episodios
  - Pause, renew, delete son transiciones independientes
  - Modo ALL crea episodio por cada tren nuevo disponible
  - FIRST y LAST seleccionan tren frontera
  - ID real preferido vs fallback documentado
  - Expiracion limitada por salida (30 dias o salida, lo que antes ocurra)
  - Medianoche y cambio horario verano/invierno usan fechas Madrid

### Bloqueos
Ninguno.

### Siguiente paso
Paso 14: SQLite y migraciones (14-sqlite-y-migraciones.md)

## Paso 14 - SQLite y migraciones (14-sqlite-y-migraciones.md) COMPLETADO

### Estado
**Implementado y probado localmente**

### Decisiones adoptadas
- Creada capa `backend/app/db/` (conexión, migraciones y backup) según la arquitectura aprobada.
- `connect()` configura WAL, busy_timeout=5000 y foreign_keys=ON; una conexión efímera por operación, cerrada con `finally`.
- Migraciones versionadas atómicas por versión con `BEGIN/COMMIT` dentro de `executescript`; la tabla `schema_migrations` se bootstrap con `IF NOT EXISTS` y cada versión se registra en la misma transacción.
- `backup_database()` usa la API online backup de SQLite (`source.backup()`), no una copia ciega del `.db`.
- `FollowUpRepository` con transacciones cortas: cada método abre su propia conexión en un context manager que hace commit/rollback y cierra la conexión, garantizando que nunca se mantiene una transacción abierta durante consultas de red.
- Serialización de enums del dominio por su valor textual (`.value`), instantes como epoch UTC (entero), fechas de viaje como `YYYY-MM-DD`, `seen_available_train_ids` como JSON ordenado.
- Tablas persistidas: `followups`, `episodes` (con constraint UNIQUE por followup+episode), `app_config` (KV) y `schema_migrations`. La FK con ON DELETE CASCADE asegura que purgar un seguimiento elimina sus episodios.
- Datos fuera de directorios reemplazables: la ruta por defecto `data/renfe-notifier.db` queda fuera del paquete `app/`; en Docker se monta un volumen dedicado y se configura con `RENFE_NOTIFIER_DATABASE_PATH`.
- `database_path` añadido a `Settings` (prefijo `RENFE_NOTIFIER_`) y documentado en `.env.example`.

### Archivos creados/modificados
- `backend/app/db/__init__.py` (creado)
- `backend/app/db/connection.py` (creado — `connect()` + `backup_database()`)
- `backend/app/db/migrations.py` (creado — `MIGRATIONS` + `apply_migrations()`)
- `backend/app/followups/database.py` (reemplazado placeholder `test` por `FollowUpRepository` completo)
- `backend/app/config.py` (añadido campo `database_path`)
- `backend/.env.example` (añadido comentario de la variable de ruta de DB)
- `backend/tests/test_followup_database.py` (11 tests nuevos)
- `PROGRESS.md` (actualizado)

### Pruebas ejecutadas y resultados
- `ruff check .` → All checks passed
- `mypy app tests` → no issues found in 22 source files
- `pytest` → 33 passed (22 anteriores + 11 nuevos)
- Tests de persistencia cubren:
  - Initialize crea WAL, busy_timeout y foreign_keys correctos, y versión de migración 1
  - Inicialización idempotente al reabrir (migración no se repite)
  - Roundtrip completo create → get → save → get (episodio, acknowledge, train_ids)
  - Persistencia entre reinicios (same file, nueva instancia)
  - Episodios: roundtrip y UNIQUE (duplicado ignorado)
  - Listado con filtro por lifecycle y purgado físico controlado de DELETED
  - Config KV: ausencia → set → sobrescritura → lectura
  - Integridad ante duplicado: IntegrityError y rollback sin corromper datos existentes
  - Backup consistente: copia recuperable independiente y sin dependencia del original
  - FileNotFoundError al intentar backup de un source inexistente
  - Recuperación tras eliminación de archivos → nueva base limpia con migraciones aplicadas
- No se realizó ninguna prueba real contra Renfe ni en dispositivo/VM.

### Bloqueos
Ninguno para el paso 14. Permanece el bloqueo preventivo de despliegue en VM anotado en el paso 06.

### Siguiente paso
Paso 15: Planificador y agrupación (15-planificador-y-agrupacion.md)

## Paso 15 - Planificador y agrupación (15-planificador-y-agrupacion.md) COMPLETADO

### Estado
**Implementado y probado localmente**

### Decisiones adoptadas
- Creado paquete `backend/app/scheduler/` con separación estricta entre lógica pura (`plan.py`) y ciclo asíncrono con IO/persistencia (`service.py`).
- `GroupKey` agrupa por (origen, destino, fecha de viaje, plaza_h): todos los parámetros que afectan la respuesta de Renfe. Cada grupo aparece exactamente una vez por ciclo, garantizando exclusión de comprobaciones simultáneas del mismo grupo.
- `SchedulerService.run_once()`: un único propietario, lee seguimientos `ACTIVE` desde SQLite (recuperación tras reinicio), construye el plan, busca una vez por grupo con semáforo de concurrencia global (por defecto 2), y persiste cada seguimiento.
- `run_forever()`: bucle `run_once()` + `sleep(interval)` sin cola → sin acumulación de tareas atrasadas ni solapamiento de ciclos.
- `Optimización`: se guarda solo cuando `outcome.followup is not followup`; los nuevos episodios se persisten en la tabla `episodes`.
- Cliente y reloj inyectables: `search_fn` (`Callable`, sesión HTTP efímera por llamada → aislamiento de sesiones concurrentes) y `clock` (`Callable[[], datetime]`, por defecto UTC).
- "Continúa comprobando aunque ya se haya enviado un aviso": el dominio `apply()` mantiene `ACTIVE` tras `pending_alert`; `new_episode` solo se produce en la transición unavailable→available o con trenes nuevos en modo ALL (ya cubierto por `test_followup_domain.py` y ahora por `test_scheduler_service.py`).
- Búsquedas manuales equivalentes: se comparte el CÓDIGO puro (`build_plan`, `train_list_to_observation`, `error_observation`) para que una futura búsqueda manual (paso 18) reutilice la misma semántica, pero NO se comparten resultados HTTP del planificador (evita datar obsoleta). Decisión evaluada y documentada.
- `plaza_h` incorporado al dominio `FollowUp`, a la BD (migración v2 `ALTER TABLE ... ADD COLUMN plaza_h INTEGER NOT NULL DEFAULT 0` + índice de agrupación v2) y al repositorio; la v1 ya creada converge con la v2 sin cambios manuales.
- `StationCatalog.by_code(code)` añadido para resolver códigos de estación del seguimiento sin depender de búsqueda por nombre.

### Archivos creados/modificados
- `backend/app/scheduler/__init__.py` (creado)
- `backend/app/scheduler/plan.py` (creado — GroupKey, build_plan, train_list_to_observation, error_observation)
- `backend/app/scheduler/service.py` (creado — SchedulerService)
- `backend/app/followups/domain.py` (añadido campo `plaza_h`)
- `backend/app/db/migrations.py` (añadida migración v2)
- `backend/app/followups/database.py` (serialización de `plaza_h`)
- `backend/app/renfe/stations.py` (añadido `by_code`)
- `backend/tests/test_scheduler_plan.py` (creado)
- `backend/tests/test_scheduler_service.py` (creado)
- `backend/tests/test_stations.py` (tests de `by_code`)
- `backend/tests/test_followup_database.py` (roundtrip `plaza_h` y versiones [1,2])
- `PROGRESS.md` (actualizado)

### Pruebas ejecutadas y resultados
- `ruff check .` → All checks passed
- `mypy app tests` → no issues found in 27 source files
- `pytest` → 50 passed (33 anteriores + 17 nuevos)
- Tests `test_scheduler_plan.py` (puros): agrupación compatible en un grupo; separación por plaza_h y ruta; entrada vacía; mapeo de `TrainList` → `Observation` VALID; mapeo de cada tren (disponible/no disponible); `error_observation` con kind ERROR.
- Tests `test_scheduler_service.py` (reloj y cliente simulados — `FakeSearch` + reloj fijo): sin seguimientos activos no busca; happy path persiste AVAILABLE/PENDING/episodio 1; error `RenfeClientError` mantiene el seguimiento intacto; estación desconocida descarta grupo sin buscar; con `concurrency=1` se procesan dos grupos distintos (plaza_h True/False); sin episodio duplicado mientras sigue disponible; reaparición tras UNAVAILABLE crea episodio 2; `run_forever` ejecuta un ciclo por tick (3 ticks con sleep contador y cancelación).
- `test_followup_database.py`: verificación de migraciones `[1, 2]` y roundtrip de `plaza_h=True`.
- `test_stations.py`: `by_code` resuelve estación existente y devuelve `None` para código desconocido.
- No se realizaron consultas reales contra Renfe ni validaciones en VM/dispositivo.

### Bloqueos
Ninguno para el paso 15. El bloqueo preventivo de despliegue en la VM del paso 06 permanece pendiente de confirmación por el usuario.

### Siguiente paso
Paso 16: Recordatorios y cola de avisos (16-recordatorios-y-cola-avisos.md)

## Paso 16 - Recordatorios y cola de avisos (16-recordatorios-y-cola-avisos.md) COMPLETADO

### Estado
**Implementado y probado localmente**

### Decisiones adoptadas
- Creado paquete `backend/app/reminders/` con separación estricta entre dominio puro (`domain.py`) y repositorio de cola SQLite (`queue.py`).
- Cola persistente directamente en SQLite (sin Redis ni Celery): nueva migración v3 que añade la tabla `alert_events` con FK a `followups` y a `episodes` (cascade). Cada aviso queda ligado a un **episodio concreto** mediante `episode_id`.
- Evento de aviso con `event_id` (PK, idempotencia por duplicado), `followup_id`, `episode_id`, `observed_at` (detección), `expires_at` (caducidad del recordatorio), `created_at`, `remind_at` (próximo recordatorio) y contador `attempts` frente a `max_attempts`.
- Estados persistentes: `pending`, `suspended`, `cancelled`, `delivered`, `failed`.
- Recordatorios configurables y acotados: retraso inicial (`initial_delay_s`), separación entre recordatorios (`interval_s`, inyectada en `AlertQueue`) y tope de recordatorios por evento (`max_attempts`). Añadidas las opciones `RENFE_NOTIFIER_REMINDER_INITIAL_DELAY_S`, `RENFE_NOTIFIER_REMINDER_INTERVAL_S` y `RENFE_NOTIFIER_REMINDER_MAX_ATTEMPTS` a `Settings` y `.env.example`.
- Cancelación al confirmar (`acknowledged`), pausar (`paused`), vencer (`expired`) y desaparecer las plazas (`unavailable`) mediante `cancel_for_followup(reason)`.
- Suspensión por disponibilidad obsoleta: `suspend_for_followup()` marca los `pending` como `suspended`; la observación STALE puede cancelarlos después (`cancelled_reason=STALE`), constante compartida en el dominio.
- Reemplazo: al encolar un episodio más reciente del mismo seguimiento se invalidan los avisos previos pendientes/suspendidos (`cancelled_reason=replaced`); un único aviso activo por seguimiento.
- Entrega *al menos una vez* con reintentos acotados e idempotencia: `enqueue` es un no-op si el `event_id` ya existe; `deliver`/`mark_failed` son no-ops sobre eventos no pendientes; tras el último intento el evento pasa a `delivered`. **No se promete entrega exactamente una vez** (documentado en docstrings).
- Recuperación tras reinicio: todo el estado (incluidos `attempts` y `remind_at`) vive en SQLite; `due_events(now, limit)` selecciona los que vencen y persistencia tras "reinicio" cubierta por test.
- `add_episode` ahora devuelve la fila `Episode` persistida (con `episode_id` interno) de forma idempotente, permitiendo asociar eventos a episodios concretos.
- No se introduce ningún envío FCM ni salida de red en este paso.

### Archivos creados/modificados
- `backend/app/reminders/__init__.py` (creado)
- `backend/app/reminders/domain.py` (creado — ReminderStatus, ReminderEvent.create, first_remind_at, next_remind_at, to_epoch, from_epoch, constantes REPLACED/STALE)
- `backend/app/reminders/queue.py` (creado — AlertQueue: enqueue/get/due_events/deliver/mark_failed/cancel_for_followup/suspend_for_followup/count_pending)
- `backend/app/db/migrations.py` (añadida migración v3 con `alert_events` e índices de "vencidos" y por followup)
- `backend/app/followups/database.py` (add_episode devuelve `Episode` con `episode_id`; `Episode` ampliada con `episode_id`; list_episodes lo incluye)
- `backend/app/config.py` (opciones `reminder_initial_delay_s`, `reminder_interval_s`, `reminder_max_attempts`)
- `backend/.env.example` (documentadas las tres variables de recordatorios)
- `backend/tests/test_reminders_domain.py` (creado — 7 tests)
- `backend/tests/test_reminders_queue.py` (creado — 17 tests)
- `backend/tests/test_followup_database.py` (versiones de esquema `[1, 2, 3]` y asserción del `episode_id` devuelto por add_episode)
- `PROGRESS.md` (actualizado)

### Pruebas ejecutadas y resultados
- `ruff check .` → All checks passed
- `mypy app tests` → no issues found in 32 source files
- `pytest` → 74 passed (50 anteriores + 7 dominio + 17 cola)
- Tests `test_reminders_domain.py` (puros): primer recordatorio tras el retraso configurado; rechazo de instantes sin zona horaria; `first_remind_at`/`next_remind_at` conservan UTC; roundtrip epoch; constantes REPLACED/STALE; valores textuales de estados.
- Tests `test_reminders_queue.py` (SQLite + reloj simulado): roundtrip enqueue/get; **duplicado idempotente** (mismo `event_id` → no-op); reemplazo de avisos anteriores del mismo seguimiento; los avisos de otros seguimientos se conservan; respeta el retraso inicial en `due_events`; `deliver` consume intentos y reprograma el siguiente; **tope acotado de intentos** (deliver final → `delivered` y deja de entregar); `deliver` de evento desconocido/entregado es no-op; **errores** (`mark_failed` una sola vez con razón); **cancelaciones** por acknowledged/paused/expired/unavailable; suspensión por obsolescencia oculta los `pending` y sigue siendo cancelable; **recuperación tras reinicio** (nueva instancia conserva attempts y remind_at); eventos ligados a episodios persistidos.
- No se realizaron consultas reales contra Renfe, envíos de FCM ni validaciones en VM/dispositivo.

### Bloqueos
Ninguno para el paso 16. El bloqueo preventivo de despliegue en la VM del paso 06 permanece pendiente de confirmación por el usuario.

### Siguiente paso
Paso 18 (según `prompts/`)

## Paso 17 - Emparejamiento y seguridad (17-emparejamiento-y-seguridad.md) COMPLETADO

### Estado
**Implementado y probado localmente**

### Decisiones adoptadas
- Mecanismo aprobado en `docs/security.md`: CLI genera código OTP temporal `RF-XXXXXX`; la app lo reclama con `POST /api/v1/pairing/claim {code, device_id, device_name}` y recibe un `device_token = secrets.token_urlsafe(48)`. La BD almacena **solo hashes SHA-256** de códigos y tokens.
- Migración v4: tablas `pairing_codes` (code_hash, expires_at, max_attempts, attempts, claimed_at, created_at) y `devices` (device_id PK UNIQUE, device_name, token_hash UNIQUE, created_at, revoked_at).
- `PairingError` unifica los motivos inválido/caducado/agotado/usado → la API responde siempre 401 genérico sin revelar la causa al cliente.
- Re-claim del mismo `device_id` revoca el token previo: se usa `INSERT OR REPLACE` en `add_device` para que un único dispositivo exista por `device_id`.
- Límites de intentos: `can_try()` y `max_attempts` en el dominio + rate limit por IP en memoria (`InMemoryRateLimiter`, ventana deslizante configurable, por defecto 10 req / 60 s).
- API protegida: `GET /api/v1/pairing/devices` requiere `Authorization: Bearer <token>` validado por SHA-256; sin token o token revocado → 401 genérico.
- Medidas de seguridad: `SensitiveFormatter` y `redact()` enmascaran `Authorization: Bearer X`, `RF-XXXXXX`, tokens largos (>20 chars) y cookies. Servidor solo escucha en `127.0.0.1`, CORS restringido.
- CLI local no expuesta públicamente: comandos `pairing-code --generate`, `devices --list/--revoke/--revoke-all`.
- Dependencias de FastAPI con patrón `Annotated[Type, Depends(...)]` (sin `Depends()` en defaults de parámetros) para complir ruff B008.
- Clock inyectable (`Clock = Callable[[], datetime]`) en `PairingService` para tests deterministas de caducidad.

### Archivos creados/modificados
- `backend/app/pairing/__init__.py` (creado)
- `backend/app/pairing/domain.py` (creado — PairingCode, Device, generate_pairing_code, hash_secret, as_utc)
- `backend/app/pairing/database.py` (creado — PairingRepository: create_code, get_by_code_hash, save_code, purge_expired_codes, add_device UPSERT, get_device_by_token_hash, get_device_by_id, list_devices, save_device)
- `backend/app/pairing/service.py` (creado — PairingService, PairingError)
- `backend/app/api/deps.py` (creado — get_pairing_service, require_device Bearer con patrón Annotated)
- `backend/app/api/pairing.py` (creado — POST /api/v1/pairing/claim, GET /api/v1/pairing/devices)
- `backend/app/middleware/rate_limit.py` (creado — InMemoryRateLimiter)
- `backend/app/middleware/logging.py` (creado — redact, SensitiveFormatter)
- `backend/app/cli.py` (creado — pairing-code --generate, devices --list/--revoke/--revoke-all)
- `backend/app/config.py` (añadidos pairing_code_ttl_s, pairing_max_attempts, rate_limit_window_s, rate_limit_max_requests)
- `backend/.env.example` (añadidas variables de emparejamiento y rate limit)
- `backend/app/db/migrations.py` (añadida migración v4 con pairing_codes y devices)
- `backend/app/main.py` (lifespan con PairingService, routers pairing+rate limit, LOG_FORMAT, CORS ampliado)
- `backend/tests/test_pairing_domain.py` (creado — 7 tests)
- `backend/tests/test_pairing_database.py` (creado — 8 tests)
- `backend/tests/test_pairing_service.py` (creado — 9 tests, reloj falso)
- `backend/tests/test_pairing_api.py` (creado — 9 tests con TestClient)
- `backend/tests/test_followup_database.py` (aserciones de esquema actualizadas a [1,2,3,4])

### Pruebas ejecutadas y resultados
- `ruff check .` → All checks passed
- `mypy app tests` → no issues found in 46 source files
- `pytest` → 107 passed (74 anteriores + 7 dominio + 8 database + 9 service + 9 API)
- Tests `test_pairing_domain.py` (puros): generate genera código RF-\d{6}; hash_securo produce hex de 64 chars; PairingCode sin claim ni revocación; expiración; agotamiento de intentos; revocación, is_active.
- Tests `test_pairing_database.py` (SQLite): migración aplica versiones [1,2,3,4]; roundtrip completo; persiste attempts y claimed_at; purge de caducados sin tocar válidos; dispositivos roundtrip; revocación; list_devices en orden de creación; solo hash almacenado (no valor en claro).
- Tests `test_pairing_service.py` (reloj falso): crea código y almacena solo hash; claim entrega token; código inválido → PairingError; código caducado → PairingError; reuso → PairingError; re-claim del mismo device_id revoca el token previo; revoke y revoke_all funcionan; token no emparejado → None.
- Tests `test_pairing_api.py` (TestClient): claim válido 200; código inválido 401; payload malformado 422; acceso sin token 401; token inválido 401; token válido 200 con device_id; token revocado 401; rate limit 429 tras umbral; redacción de secretos en logs; el código OTP no aparece en la respuesta.
- No se realizaron consultas reales contra Renfe, envíos de FCM ni validaciones en VM/dispositivo.

### Bloqueos
Ninguno para el paso 17. El bloqueo preventivo de despliegue en la VM del paso 06 permanece pendiente de confirmación por el usuario.

### Siguiente paso
Paso 19 (según `prompts/`)

## Paso 18 - API funcional (18-api-funcional.md) COMPLETADO

### Estado
**Implementado y probado localmente**

### Decisiones adoptadas
- **Anti-SSRF**: los endpoints de búsqueda solo aceptan códigos de estación validados contra el catálogo local (`StationCatalog.by_code`); ningún campo admite URLs del cliente (cumple `docs/security.md` §3).
- `app/renfe/search.py` (`TrainSearchEngine`): lógica pura fuera de FastAPI para evitar import circular con `deps.py`; resuelve origen/destino y delega en `RenfeDwrClient.search`.
- `app/api/search.py`: `GET /api/v1/search/stations?q=` (búsqueda por nombre normalizado, dedupe por código, límite) y `POST /api/v1/search/trains` (valida fecha en `[hoy, hoy+62]` Europe/Madrid, origen≠destino, mapea cada tren a `TrainOut` con `identity` en formato del dominio, p.ej. `real:...`). Errores: estaciones desconocidas → 400, `RenfeClientError` → 503.
- `app/api/followups.py`: CRUD y acciones sobre `/api/v1/followups`. `followup_id` se genera en el servidor (`secrets.token_urlsafe(12)`). Validación de mode: `specific` exige `specific_train_id` (y no-specific lo prohíbe). Pausa/resume/renew/acknowledge/delete con semántica coherente: 404 si no existe, 409 si el estado no lo permite; delete es lógico e idempotente; pause/delete/acknowledge cancelan los recordatorios (`queue.cancel_for_followup`). Confirmar no borra ni pausa el seguimiento (REQ-07.4).
- `app/api/fcm.py`: `PUT /api/v1/fcm/token` registra/renueva el token FCM del dispositivo autenticado (patrón `^[A-Za-z0-9_:-]{20,4096}$`).
- `app/api/diagnostics.py`: `GET /api/v1/diagnostics/health` (público, alias del health check de CD), `GET /api/v1/diagnostics` (cifras no sensibles: versión, contadores de seguimientos por lifecycle, dispositivos, episodios, avisos pendientes) y `POST /api/v1/diagnostics/test-notification` (usa un emisor inyectado por protocolo; 503 si no hay emisor, 409 si el dispositivo no tiene token FCM).
- `app/notifications/base.py`: protocolo `NotificationSender` con `send_test` (implementación real FCM en el paso 19); inyección vía `app.state.test_sender`.
- Migración v5: `ALTER TABLE devices ADD COLUMN fcm_token TEXT`. `Device` incorpora `fcm_token`; `PairingRepository.update_fcm_token(device_id, token)` (solo activos) y `count_devices()`; `PairingService.register_fcm_token`. Re-claim del mismo `device_id` limpia el token FCM (INSERT OR REPLACE).
- `FollowUpRepository.count(lifecycle)` y `count_episodes()` añadidos para los contadores de diagnóstico.
- `main.py`: lifespan construye repositorios/cola/catálogo/engine y los cuelga de `app.state`; `test_sender` por defecto `None`. CORS ampliado a PUT/DELETE (solo debug). Versión unificada en `app/__init__.py` (`APP_VERSION`).
- `redact()` ampliado para enmascarar tokens FCM largos con `:` (`[A-Za-z0-9_:-]{20,}`).
- Tests de la API con dependencias simuladas: `httpx.MockTransport` para `RenfeDwrClient` (5 POSTs sintéticos), `FakeSender` para FCM y helpers compartidos (`tests/helpers.py`); `tests/__init__.py` añadido para mypy.

### Archivos creados/modificados
- `backend/app/renfe/search.py` (creado — TrainSearchEngine, StationNotFoundError)
- `backend/app/api/search.py` (creado — estaciones y búsqueda de trenes)
- `backend/app/api/followups.py` (creado — CRUD + pause/resume/renew/acknowledge/delete)
- `backend/app/api/fcm.py` (creado — PUT /api/v1/fcm/token)
- `backend/app/api/diagnostics.py` (creado — health, cifras, test-notification)
- `backend/app/notifications/base.py` + `__init__.py` (creados — protocolo NotificationSender)
- `backend/app/api/deps.py` (añadidas dependencias de repositorio/cola/catálogo/engine/sender)
- `backend/app/main.py` (lifespan ampliado, routers nuevos, CORS PUT/DELETE, APP_VERSION)
- `backend/app/__init__.py` (creado APP_VERSION)
- `backend/app/db/migrations.py` (migración v5)
- `backend/app/pairing/domain.py`, `database.py`, `service.py` (fcm_token, update_fcm_token, count_devices, register_fcm_token)
- `backend/app/followups/database.py` (count, count_episodes)
- `backend/app/middleware/logging.py` (redact con `:` para tokens FCM)
- `backend/tests/__init__.py` (creado) y `backend/tests/helpers.py` (creado — make_app, claim_device, auth_headers, FakeSender)
- `backend/tests/test_search_api.py` (creado), `test_followups_api.py` (creado), `test_fcm_api.py` (creado), `test_diagnostics_api.py` (creado)
- `backend/tests/test_followup_database.py` y `test_pairing_database.py` (aserciones de esquema [1,2,3,4,5] + tests de fcm_token/count)
- `PROGRESS.md` (actualizado)

### Pruebas ejecutadas y resultados
- `ruff check .` → All checks passed
- `mypy app tests` → no issues found in 59 source files
- `pytest` → 134 passed (107 anteriores + 27 nuevos)
- Tests `test_search_api.py` (MockTransport): estaciones requieren auth; búsqueda por nombre y límites; búsqueda de trenes devuelve `TrainOut` tipado (identifier `AVE-001|08:00:00|09:30:00`, identity `real:...`, precios y disponibilidad); estación desconocida → 400; fechas pasada/horizonte/origen=destino → 400; auth obligatoria.
- Tests `test_followups_api.py`: crear con id generado en servidor y nombres resueltos; mode específico requiere/veda `specific_train_id`; estación desconocida, fecha pasada y origen=destino → 400; listado con filtro; detalle con episodios vacíos; 404 para id inexistente; pausa/reanudación independientes y reversibles con 409 en estado inadecuado; renew revive vencidos y 409 tras delete; acknowledge confirma sin borrar (pending_alert→acknowledged), 409 sin aviso pendiente; delete lógico idempotente visible en listado; 404 en acciones de id inexistente.
- Tests `test_fcm_api.py`: PUT exige auth; registro y renovación idempotentes (200 registered=true); tokens cortos o con caracteres inválidos → 422.
- Tests `test_diagnostics_api.py`: health público 200; estadísticas requieren auth y con device+followup reportan contadores y versión; test-notification: 401 sin auth, 503 sin emisor, 409 sin token FCM y 200 con `message_id` usando FakeSender (verifica el envío al token registrado).
- `test_pairing_database.py` añadidos: roundtrip fcm_token + count_devices, `update_fcm_token` falla para id inexistente y re-claim resetea el token FCM.
- No se realizaron consultas reales contra Renfe, envíos FCM reales ni validaciones en VM/dispositivo.

### Bloqueos
Ninguno para el paso 18. El bloqueo preventivo de despliegue en la VM del paso 06 permanece pendiente de confirmación por el usuario.

### Siguiente paso
Paso 19 (según `prompts/`)

## Paso 19 - Transporte FCM (19-transporte-fcm.md) COMPLETADO

### Estado
**Implementado y probado localmente** (sin envíos FCM reales).

### Decisiones adoptadas
- **HTTP v1** (`https://fcm.googleapis.com/v1/projects/{project_id}/messages:send`) en lugar de Admin SDK; sin Cloud Functions ni dependencias nuevas de producción. `google-auth` es opcional y solo para local (import lazy); la VM usa la **identidad de la instancia** vía metadatos GCE (permisos mínimos, sin credenciales en el repo). Ninguna credencial se lee en el contexto del modelo ni viaja por la API.
- **Payload data-only** (sin bloque `notification`), según REQ-08.5: la app muestra la notificación nativa sin consulta adicional. Campos: `type`, `event_id`, `followup_id`, `episode_id`, `title`, `body`, `origin_code`/`origin`, `destination_code`/`destination`, `travel_date`, `departure`, `arrival`, `price`, `observed_at`, `expires_at`, `channel_id`, `priority`. `AlertMessage` (payload tipado) valida claves/valores/tamaño (≤4096 bytes) antes de enviar (`FcmMessage.validate()`).
- **Prioridad y TTL**: `android.priority="high"` solo para alertas de plazas y test (visibles por definición); `resumen_y_servicio` usará `normal` en el futuro. TTL corto por defecto (300 s) como declaración, no garantía; `expires_at` permite descartar entregas fuera de plazo. `collapse_key="followup:{followup_id}"` sustituye avisos antiguos del mismo seguimiento.
- **Reintentos y deduplicación**: el transporte es idempotente por mensaje y **no reintenta internamente**; los intentos los acota la `AlertQueue` existente y la deduplicación la garantiza `event_id` (servidor y cliente). HTTP v1 no admite claves de idempotencia para mensajes individuales → "al menos una vez" con intentos acotados.
- **Separación aceptado vs mostrado**: 200+`name` → `FcmResult.accepted=True` (no implica visualización; el cliente confirma en el paso de episodios `acknowledged`); 404/`UNREGISTERED`/"registration token" → `token_invalid` (permite invalidar el token y seguir con el siguiente dispositivo); 401/403/400 genérico → `FcmRejectedError(retryable=False)`; 429/5xx → `retryable=True`; sin credenciales → `FcmNotConfiguredError`.
- **Proveedores de token**: `FcmTokenProvider` (protocolo), `StaticTokenProvider` (tests), `GceMetadataTokenProvider` (metadatos GCE) y `AutoTokenProvider` (google-auth ADC primero, metadato GCE como fallback; ambos fallidos → 503).
- **Invalidación de tokens**: `PairingRepository.clear_fcm_token(device_id)`, `get_device_by_fcm_token(token)` y `PairingService.clear_fcm_token` / `invalidate_fcm_token` (sin revocar el emparejamiento).
- **API**: en diagnóstico, token inválido → **410 Gone**; `FcmNotConfiguredError` → **503**; otros errores de envío → 502. `main.py` construye el sender solo si `RENFE_NOTIFIER_FCM_PROJECT_ID` está definido y lo expone en `app.state.fcm_sender`/`app.state.test_sender`.
- **Settings** nuevas (prefijo `RENFE_NOTIFIER_`): `fcm_project_id`, `fcm_timeout_s` (10), `fcm_default_ttl_s` (300), `fcm_app_package`. Documentadas en `.env.example`. `docs/notificaciones-fcm.md` recoge el contrato (payload, prioridad, TTL, invalidación, reintentos, aceptado-vs-mostrado y configuración pendiente).
- Dependencia `tzdata` (solo Windows) porque el `python` local (3.14, Windows) no dispone de base de datos IANA para `zoneinfo`; requerida para la suite en este entorno.

### Archivos creados/modificados
- `backend/app/notifications/fcm.py` (creado — transporte HTTP v1, proveedores de token, payload tipado)
- `backend/app/api/diagnostics.py` (mapeos 410/503/502 en test-notification)
- `backend/app/main.py` (wiring del sender condicionado a `fcm_project_id`)
- `backend/app/config.py` y `backend/.env.example` (settings `fcm_*`)
- `backend/app/pairing/database.py` y `backend/app/pairing/service.py` (invalidación de tokens FCM)
- `backend/pyproject.toml` (`tzdata; platform_system == 'Windows'`)
- `docs/notificaciones-fcm.md` (creado — contrato y configuración pendiente)
- `backend/tests/test_fcm_transport.py` (creado), `backend/tests/test_fcm_providers.py` (creado)
- `backend/tests/test_diagnostics_api.py` (410/503), `backend/tests/test_pairing_database.py` (clear/lookup por token), `backend/tests/test_pairing_service.py` (invalidate/clear)
- `prompts/PROGRESS.md` y `PROGRESS.md` (actualizados)

### Pruebas ejecutadas y resultados
- `ruff check . --fix` → 6 auto-fixes (imports, newline) y `ruff check .` → All checks passed
- `mypy app tests` → Success, no issues found in 62 source files
- `pytest` → **163 passed** (134 anteriores + 29 nuevos) en ~8 s
- `test_fcm_transport.py` (MockTransport, sin red): cuerpo completo (URL del endpoint, `Authorization: Bearer <token>`, token/priority/ttl/collapse/package/data); 200+name → accepted+message_id (nombre completo del recurso, formato real de FCM); 404/`UNREGISTERED` → `token_invalid`; 400 genérico → retryable=False; 401 → no retryable; 429/500/503 → retryable; body vacío aceptado; defaults del sender; validación de payload (valores, total, token vacío, claves inválidas, TTL); contrato de `build_test_alert` y `AlertMessage`; `send_test` devuelve id y eleva token inválido; `StaticTokenProvider`; constructor valida project_id/TTL.
- `test_fcm_providers.py`: GCE metadatos OK (header `Metadata-Flavor: Google` correcto), fallos HTTP/transporte → `FcmNotConfiguredError`; `AutoTokenProvider` sin google-auth (monkeypatch de sys.modules) cae al metadato y, sin ambos, eleva `FcmNotConfiguredError`.
- `test_diagnostics_api.py` añadidos: token rechazado por FCM → **410**; provedor sin credenciales (metadato 500) → **503**.
- `test_pairing_database.py`: `clear_fcm_token`/`get_device_by_fcm_token` (incluye token desconocido → None).
- `test_pairing_service.py`: `invalidate_fcm_token` invisibiliza solo el token afectado y `clear_fcm_token` mantiene el emparejamiento (nota: `token_hash` es único por dispositivo, por eso el helper de test usa hashes distintos).
- **No se enviaron mensajes FCM reales, no se crearon recursos ni se tocó la VM.** La validación en dispositivo/VM con Firebase real queda pendiente (pasos 33-35).

### Bloqueos
- Ninguno nuevo. Despliegue en VM pendiente de confirmación del usuario (bloqueo preventivo del paso 06).
- Configuración real de FCM **pendiente y documentada** (paso 33): proyecto Firebase Spark sin facturación, identidad de la VM con permisos mínimos `firebase.messaging` y, opcional en local, `pip install google-auth` + cuenta de servicio.

### Siguiente paso
No avanzar: siguiente archivo `20-base-android.md` (esperar instrucciones).

## Paso 20 - Base Android (20-base-android.md) COMPLETADO

### Estado
**Implementado y compilado el APK debug** (sin validación en dispositivo real; requiere aprobación e instalación en el realme GT Neo 2).

### Decisiones adoptadas
- **Proyecto** en `android/` con módulo único `:app` (evitar multi-módulo sobre-ingeniería según `docs/architecture.md` §4.2) y paquete `com.pablovb019.renfenotifier`, organizado por funcionalidades (`feature/home` por ahora; `search`, `followups`, `settings` y `common` llegarán en pasos posteriores sin paquetes vacíos).
- **Stack estable y compatible** (documentado en `docs/ci-cd.md` y `docs/android-build.md`): Gradle 8.9, AGP 8.5.2, Kotlin 2.0.21 (plugin `org.jetbrains.kotlin.plugin.compose`), Compose BOM 2024.12.01, material3, activity-compose 1.9.3, lifecycle 2.8.7 (runtime/compose/viewmodel), navigation-compose 2.8.5, core-ktx 1.13.1 (la 1.15.0 exige compileSdk 35, incompatible con AGP 8.5.2). JDK 17 + `jvmTarget` 17.
- **SDKs**: compileSdk 34, targetSdk 34 (nunca un target antiguo para eludir restricciones), minSdk 26 (el realme GT Neo 2 es Android 13). Version catalog en `gradle/libs.versions.toml`.
- **Activity única + navegación**: `MainActivity` con `enableEdgeToEdge()` y `setContent`; `AppNavHost` con `NavHost`/`rememberNavController` y un único destino `home` con `Destinations`.
- **Tema claro/oscuro**: `RenfeNotifierTheme` con color dinámico (Android 12+) y paleta fija Material 3 (azul Renfe) de respaldo; tipografía M3 por defecto.
- **ViewModel + coroutines/Flow**: `HomeViewModel` inmutable con `StateFlow<HomeUiState>` (`asStateFlow`, `update`) y un bucle en `viewModelScope` que refresca la hora local cada segundo; la pantalla consume el estado con `collectAsStateWithLifecycle()` y delega en un `HomeContent` sin estado (solo datos y eventos).
- **Recursos en español**: `res/values/strings.xml` en español como idioma por defecto.
- **Accesibilidad básica**: textos con descripción semántica en el botón de acción (`contentDescription`), contraste del esquema comprobado y soporte RTL.
- **Toolchain local a 0 € (Windows, sin Android Studio)**: JDK 17 Temurin y Gradle 8.9 en `%LOCALAPPDATA%`, cmdline-tools en `%LOCALAPPDATA%\Android\Sdk\cmdline-tools\latest`, plataforma 34 y build-tools 34.0.0 vía `sdkmanager`; wrapper de Gradle versionado (`gradlew.bat`). `local.properties` con `sdk.dir` queda excluido de Git.
- Documentación de compilación local en Windows: `docs/android-build.md` (paso a paso, comandos y notas).

### Archivos creados/modificados
- `android/settings.gradle.kts`, `android/build.gradle.kts`, `android/gradle.properties`, `android/gradle/libs.versions.toml` (creados)
- `android/gradlew`, `android/gradlew.bat`, `android/gradle/wrapper/*` (generados y versionados)
- `android/local.properties` (creado, gitignored)
- `android/app/build.gradle.kts`, `android/app/proguard-rules.pro` (creados)
- `android/app/src/main/AndroidManifest.xml` (creado)
- `android/app/src/main/res/values/themes.xml`, `values/strings.xml` (creados, español)
- `android/app/src/main/java/com/pablovb019/renfenotifier/MainActivity.kt` (creado)
- `android/app/src/main/java/com/pablovb019/renfenotifier/navigation/AppNavHost.kt` (creado)
- `android/app/src/main/java/com/pablovb019/renfenotifier/ui/theme/{Theme,Type}.kt` (creados)
- `android/app/src/main/java/com/pablovb019/renfenotifier/feature/home/{HomeUiState,HomeViewModel,HomeScreen}.kt` (creados)
- `docs/android-build.md` (creado)
- `prompts/PROGRESS.md` y `PROGRESS.md` (actualizados)

### Pruebas ejecutadas y resultados
- Provisionado el toolchain completo (evidencia: `sdkmanager --version` → 12.0; plataforma android-34 y build-tools 34.0.0 instaladas).
- `gradle wrapper --gradle-version 8.9 --no-daemon` → **BUILD SUCCESSFUL** (1m39s).
- `.\gradlew.bat :app:assembleDebug --no-daemon` → **BUILD SUCCESSFUL in 43s**. El primer intento falló y se corrigió:
  1. `core-ktx 1.15.0` exige compileSdk 35 → bajado a 1.13.1.
  2. `stringResource` dentro del lambda `semantics { }` no es composable → extraído el string antes del modificador.
- Artefacto verificado: `android/app/build/outputs/apk/debug/app-debug.apk` (9.844.466 bytes) y `output-metadata.json` confirma `applicationId com.pablovb019.renfenotifier`, `versionName 0.1.0`, `versionCode 1`, `minSdk(para dexing) 26`, variante `debug`.
- **No se validó en dispositivo real**: instalar el APK en el realme GT Neo 2 y su verificación visual requieren paso posterior con aprobación.

### Bloqueos
- Ninguno para el paso 20. Sigue pendiente el bloqueo preventivo de despliegue en VM del paso 06, y la validación en dispositivo real (realme GT Neo 2) que requiere aprobación e instalación del APK.

### Siguiente paso
No avanzar: siguiente archivo `21-cliente-android-y-emparejamiento.md` (esperar instrucciones).

## Paso 21 - Cliente Android y emparejamiento (21-cliente-android-y-emparejamiento.md) COMPLETADO

> Coordinación con el usuario: pidió "paso 22", pero tras aclaración autorizó
> ejecutar primero el paso 21 (el 22 depende de su cliente de red y emparejamiento).

### Estado
**Implementado + probado localmente (servidor simulado + tests unitarios) + APK debug compilado.** Sin validación en dispositivo real (pendiente de aprobación).

### Decisiones adoptadas
- **Stack de red estable**: Retrofit 2.11.0, OkHttp 4.12.0, converter-gson, DataStore 1.1.1, coroutines-test 1.9.0, JUnit 4, MockWebServer 4.12.0. Gson con `FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES` (mapeo automático `snake_case` del backend <-> `camelCase` Kotlin).
- **Contratos de API** espejo exacto de los Pydantic del backend (`core/network/model/ApiModels.kt`): health, pairing (claim/devices), search (stations/trains) y followups (create/list/detail/pause/resume/renew/acknowledge/delete), y la interfaz `RenfeApi` (Retrofit) con esas 14 rutas.
- **TLS normal (nunca lenient)**: OkHttp con verificación por defecto; el cliente solo acepta HTTPS salvo hosts de bucle local (`localhost`, `127.0.0.1`, `10.0.2.2`) mediante `ApiModule.validateBaseUrl`; `network_security_config.xml` exige `cleartextTrafficPermitted=false` y solo lo permite en los 3 hosts de test; INTERNET declarado en el manifest. `BACKEND_URL` en BuildConfig (debug: `http://10.0.2.2:8000/`).
- **Emparejamiento**: pantalla `feature/pairing` (campo código OTP, botón, estados loading/error). `PairingViewModel` con dependencias inyectadas (probable); solo `vault.save()` + `sessionStore.setPaired()` cuando el backend responde 200 (nunca se marca como guardada una operación remota fallida). `device_id` = ANDROID_ID + nombre = fabricante/modelo.
- **Credenciales en Android Keystore**: `KeystoreTokenVault` cifra el token AES/GCM (clave no exportable en AndroidKeyStore) y guarda IV+blob Base64 en SharedPreferences. Interfaz `TokenVault` + `InMemoryTokenVault` para tests. Lectura síncrona (la usa el interceptor OkHttp).
- **DataStore para preferencias no secretas**: `PreferencesRepository` implementa `SessionStore` (`isPaired`, `setPaired`, `clearAll`) + `backendUrl`/`deviceId`/`deviceName`; interfaz `SessionStore` permite fake en tests.
- **Manejo de desconexión/errores**: `AuthInterceptor` añade `Authorization: Bearer` solo a endpoints protegidos (omite `/health` y `/api/v1/pairing/claim`); ante 401 limpia el token y emite `credentialsRevoked` (HomeViewModel limpia la sesión y cae a estado "no emparejado"). Errores mapeados en UI: código no válido (401), límite de intentos (429), error de servidor (5xx), sin conexión/timeout (network). Estados carga/vacío/error en las pantallas.
- **Navegación**: `Destinations.HOME` + `Destinations.PAIRING`; HomeScreen muestra CTA de emparejamiento si `!isPaired`; tras emparejar vuelve a HOME con `popUpTo` limpio. `HomeViewModel` (AndroidViewModel) consume `SessionStore.isPaired` + `credentialsRevoked`.
- **Creación del ViewModel con fábrica**: `viewModel { }` con `CreationExtras` para obtener la `Application` (evita `LocalContext` en contexto no composable).

### Archivos creados/modificados
- Creados (main):
  - `core/network/model/ApiModels.kt` (DTOs: health, pairing, station, train, followups)
  - `core/network/RenfeApi.kt` (contrato Retrofit)
  - `core/network/ApiModule.kt` (fábrica + validación URL/TLS)
  - `core/network/AuthInterceptor.kt` (Bearer + limpieza 401 + `credentialsRevoked`)
  - `core/security/TokenVault.kt` (`TokenVault`, `KeystoreTokenVault`, `InMemoryTokenVault`)
  - `core/security/PreferencesRepository.kt` (`SessionStore` + DataStore)
  - `feature/pairing/PairingViewModel.kt` (+ `PairingUiState`)
  - `feature/pairing/PairingScreen.kt`
  - `res/xml/network_security_config.xml`
- Modificados: `MainActivity.kt`, `navigation/AppNavHost.kt`, `feature/home/{HomeUiState,HomeViewModel,HomeScreen}.kt`, `res/values/strings.xml`, `AndroidManifest.xml`, `gradle/libs.versions.toml`, `app/build.gradle.kts`, `ui/theme/Theme.kt` (fix lint `NewApi` dinámico)
- Creados (test): `core/network/FakeRenfeApi.kt`, `core/network/ApiContractTest.kt`, `core/network/AuthInterceptorTest.kt`, `feature/pairing/PairingViewModelTest.kt`

### Pruebas ejecutadas y resultados (evidencia)
- `.\gradlew.bat :app:testDebugUnitTest --no-daemon` → **BUILD SUCCESSFUL**; **12 tests, 0 fallos** en 3 suites (contratos API, interceptor, ViewModel de emparejamiento) contra MockWebServer y fakes.
  - Contratos: `/health`, `POST /pairing/claim` (mapeo `device_token`), `/api/v1/search/stations` (`is_group`, query `q`/`limit`).
  - Interceptor: Bearer en protegido, ausente en `/health`, limpieza de credencial ante 401.
  - ViewModel: éxito solo tras 200 (guarda token + session), código vacío sin red, 401 "no válido" sin marcar éxito, 429, error de red, sanitización de entrada.
- Corrección de compilación: falta `override` en `SessionStore` (identificado por compilador Kotlin).
- `.\gradlew.bat :app:assembleDebug --no-daemon` → **BUILD SUCCESSFUL**, APK `app-debug.apk` (11.054.386 bytes), versionName 0.1.0.
- `.\gradlew.bat :app:lintDebug --no-daemon` → **BUILD SUCCESSFUL** tras corregir `NewApi` en `Theme.kt` (color dinámico sin guarda de API 31; era bug latente del paso 20).
- **No validado en dispositivo real** (requiere aprobación e instalación en realme GT Neo 2). El flujo puede verificarse localmente `uv run uvicorn` + emulador.

### Bloqueos
- Ninguno para el paso 21. Siguen pendientes de aprobación externa: despliegue en VM (paso 06) y validación en dispositivo real. El emparejamiento real requiere backend desplegado o local (`uvicorn`) con código CLI `python -m app.cli pairing-code --generate`.

### Siguiente paso
No avanzar: siguiente archivo `22-pantalla-busqueda.md` (esperar instrucciones).

## Paso 22 - Pantalla de búsqueda de viajes (22-pantalla-busqueda.md) COMPLETADO

### Estado
**Implementado + probado localmente (tests unitarios + MockWebServer) + APK debug compilado.** Sin validación en dispositivo real (pendiente de aprobación).

### Decisiones adoptadas
- **Estaciones con alias/tildes/grupos** resueltas por el backend (`GET /api/v1/search/stations?q=&limit=`); el cliente solo muestra resultados y distingue visualmente los grupos `(grupo)`.
- **Debounce 300 ms** en sugerencias de estaciones (cancela Job previo al tipear; vacío resuelve inmediatamente sin llamada).
- **Validación cliente**: origen y destino deben ser `StationOut` seleccionados de la lista (no texto libre), código origen ≠ destino, fecha ≥ hoy y ≤ hoy+62 días.
- **Ausencia de plazas NO es error**: `TrainSearchResponse.status` se representa como información contextual; `trains` vacío se muestra como "No hay trenes para esta ruta y fecha." (estado vacío). `searchError` solo se activa ante errores de red/servidor (HTTP 5xx, IOException). Se evita confundir error con ausencia de plazas.
- **Precio desconocido representado como tal** (`train.price == null` → "No disponible", nunca 0 €), coherente con la semántica del parser del backend (`Availability.UNKNOWN`).
- **Modos de seguimiento** mapeados al backend: `FIRST`/`LAST`/`ALL`/`SPECIFIC` (enum `FollowUpMode` en `core/model/FollowUpMode.kt` con valor serial `apiValue` para la API). `SPECIFIC` requiere selección explícita de un tren de la lista.
- **Crear seguimiento reutiliza ruta y fecha**: tras crear con éxito, el formulario conserva origen, destino y fecha vigentes; se muestra diálogo de confirmación con el ID. Nunca se marca como éxito si la llamada falla (misma filosofía que el emparejamiento).
- **Material Icons Core añadido** (`libs.versions.toml` + `build.gradle.kts`) para `Icons.AutoMirrored.Filled.ArrowBack` en la flecha de volver de SearchScreen.
- **Patrón de ViewModel consistente** con los pasos anteriores: `viewModel { }` initializer con `ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY`, sin `LocalContext` en lambdas no composable.
- **Date picker M3**: `DatePickerDialog` aislado en `SearchDatePickerDialog` privado para que `rememberDatePickerState` comparta estado con el botón de confirmar; primerDatePickerState es nullable para evitar caution.

### Archivos creados/modificados
- Creados (main):
  - `core/model/FollowUpMode.kt` (enum FIRST/LAST/ALL/SPECIFIC + object Availability)
  - `feature/search/SearchViewModel.kt` (búsqueda, debounce, validación, creación de seguimiento)
  - `feature/search/SearchScreen.kt` (SearchScreen stateful + SearchContent stateless + SearchDatePickerDialog, autocompletado de estaciones, date picker, resultados con precio nulo, selector de modo, creación de seguimiento)
- Modificados:
  - `gradle/libs.versions.toml` (añadido `androidx-compose-material-icons-core`)
  - `app/build.gradle.kts` (añadido `implementation(libs.androidx.compose.material.icons.core)`)
  - `navigation/AppNavHost.kt` (`Destinations.SEARCH`, SearchScreen con `onBack`)
  - `feature/home/HomeScreen.kt` (botón "Buscar viajes", `onNavigateToSearch`)
  - `res/values/strings.xml` (search_*, av_*, cd_*, dialog_* strings)
  - `res/values-es/strings.xml` — hereda valores por defecto (strings.xml ya es español)
- Creados (test):
  - `feature/search/SearchViewModelTest.kt` (12 tests: debounce, selección estación, éxito/búsqueda vacía/validaciones/same-origin-dest/fecha-pasada/503/IOException/mode-specific-requires-train/create-succeeds-reusing-route-and-date/error-409)
  - `core/network/FakeRenfeApi.kt` (añadidos `searchStationsHandler`, `searchTrainsHandler`, `createFollowUpHandler`)
  - `core/network/ApiContractTest.kt` (añadidos 2 tests: search/trains con POST body snake_case, create/followups con 201 y followup_id)

### Pruebas ejecutadas y resultados (evidencia)
- `.\gradlew.bat :app:testDebugUnitTest --no-daemon` → **BUILD SUCCESSFUL**; **26 tests, 0 fallos** en 4 suites:
  - `ApiContractTest` (5): health, claim, stations, trains, followups.
  - `AuthInterceptorTest` (3): Bearer, omitido en `/health`, 401 limpia.
  - `PairingViewModelTest` (6): éxito solo 200, código vacío, 401, 429, error red, sanitización.
  - `SearchViewModelTest` (12): debounce, selección, éxito, vacío, validaciones, fecha pasada, 503, IOException, specific-requires-train, create OK reutilizando ruta/fecha, create error 409.
- `.\gradlew.bat :app:assembleDebug --no-daemon` → **BUILD SUCCESSFUL**; APK `app-debug.apk` (11.129.732 bytes), `output-metadata.json` confirma `applicationId com.pablovb019.renfenotifier`, versionName 0.1.0, versionCode 1.
- `.\gradlew.bat :app:lintDebug --no-daemon` → **BUILD SUCCESSFUL** (sin errores NewApi ni strings; únicamente deprecation warning cosmético en `Modifier.menuAnchor()` sin argumentos, aceptado).

### Bloqueos
- Ninguno para el paso 22. Siguen pendientes de aprobación externa: despliegue en VM (paso 06) y validación en dispositivo real (realme GT Neo 2). El emparejamiento real requiere backend desplegado o local (`uvicorn`) con código CLI `python -m app.cli pairing-code --generate`.

### Siguiente paso
No avanzar: siguiente archivo `23-*.md` (esperar instrucciones).

## Paso 23 - Gestión de seguimientos (23-pantalla-seguimientos.md) COMPLETADO

### Estado
**Implementado + probado localmente (tests unitarios + MockWebServer) + APK debug compilado.** Sin validación en dispositivo real (pendiente de aprobación).

### Decisiones adoptadas
- **Solo cambios en Android**: el backend ya expone exactamente lo necesario (listar con `?lifecycle=`, detalle con episodios, pause/resume/renew/acknowledge/delete). Única modificación de contrato: `RenfeApi.listFollowUps` acepta el query `lifecycle` opcional, espejo del backend.
- **Listado con filtros de ciclo de vida**: `LifecycleFilter` (todos/activos/pausados/vencidos/eliminados) mapea al endpoint con `?lifecycle=<valor>`; `FollowUpsViewModel` recarga al cambiar filtro. El listado se recarga al volver del detalle (LaunchedEffect en composición, fuente de verdad = servidor).
- **Detalle con fuente de verdad en servidor**: `FollowUpDetailViewModel` recarga el detalle (GET) **después de cada acción exitosa**; nunca actualiza localmente "a optimista". Si la acción remota falla (HTTP 409/404/5xx o red) se muestra error y el estado queda intacto (sin éxito falso).
- **Diferencia clara entre pausar, confirmar y eliminar**: botones separados y distintos — Pausar/Reanudar (solo según ciclo de vida), Renovar, Confirmar aviso (solo si `alert_state == pending_alert`, con nota "confirmar no borra el seguimiento"), y Eliminar (solo con diálogo de confirmación explícito, color error). El backend también refuerza las reglas con 409.
- **Disponibilidad y última comprobación válida**: la API no expone un campo `last_check`; se deriva del episodio más reciente (`episodes[].observed_at`) como última detección válida. Si no hay episodios se muestra "Sin comprobaciones válidas registradas todavía".
- **Caducidad y horarios en Europe/Madrid**: `MadridFormat` convierte instantes UTC (ISO-8601 con offset) a `Europe/Madrid` (`dd/MM/yyyy HH:mm`) y fechas de viaje `YYYY-MM-DD` → `dd/MM/yyyy` (`Instant.from(OffsetDateTime.parse(...))`, no `Instant.parse` por el sufijo `+00:00` del backend).
- **Estado obsoleto y errores**: el ciclo de vida `expired` se muestra como "Vencido" con color error; errores de servidor (401/404/409/5xx) y de red se muestran sin marcar éxito. 503 de Renfe no se confunde con listado vacío.
- **Configuración acotada de recordatorios**: los recordatorios son globales del backend (env), sin API por seguimiento; la app muestra una nota acotada ("Recordatorios acotados: se avisará un número limitado de veces hasta confirmar o hasta que desaparezca la disponibilidad") y el estado del aviso (pendiente/confirmado) en lista y detalle.
- **Confirmar un episodio no borra**: `acknowledge` solo cambia `alert_state`; el seguimiento sigue activo. Eliminar es lógico, idempotente (DELETE 204) y cancela avisos.
- **Mensajes de error en el mismo estilo** que pasos previos (español, mapeo 401/404/409/5xx/red).
- **Navegación**: nuevos destinos `FOLLOWUPS` y `FOLLOWUP_DETAIL` con argumento `followupId` (`navArgument`); Home gana el botón "Mis seguimientos".

### Archivos creados/modificados
- Creados (main):
  - `feature/followups/FollowUpsViewModel.kt` (listado + `LifecycleFilter`)
  - `feature/followups/FollowUpDetailViewModel.kt` (detalle + acciones + `lastValidObservedAt`)
  - `feature/followups/FollowUpsScreen.kt` (lista + filtros + estados)
  - `feature/followups/FollowUpDetailScreen.kt` (detalle + acciones + diálogo de eliminación)
  - `feature/followups/MadridFormat.kt` (formato European/Madrid para instantes y fechas)
- Modificados:
  - `core/model/FollowUpMode.kt` (añadidos `AvailabilityStateValue`, `FollowUpLifecycle`, `FollowUpAlertState`)
  - `core/network/RenfeApi.kt` (`listFollowUps(lifecycle: String? = null)` con `@Query`)
  - `navigation/AppNavHost.kt` (`FOLLOWUPS`, `FOLLOWUP_DETAIL/{followupId}`)
  - `feature/home/HomeScreen.kt` (botón "Mis seguimientos", `onNavigateToFollowUps`)
  - `res/values/strings.xml` (followups_*, av_*, diálogos)
- Creados/modificados (test):
  - `feature/followups/FollowUpsViewModelTest.kt` (5 tests)
  - `feature/followups/FollowUpDetailViewModelTest.kt` (8 tests)
  - `core/network/FakeRenfeApi.kt` (handlers nuevos: list/get/pause/resume/renew/acknowledge/delete)
  - `core/network/ApiContractTest.kt` (añadidos 4 tests: listado con filtro, detalle con episodios, acción pause con ruta/verbo, delete 204)

### Pruebas ejecutadas y resultados (evidencia)
- `.\gradlew.bat :app:testDebugUnitTest --no-daemon` → **BUILD SUCCESSFUL**; **43 tests, 0 fallos** en 6 suites:
  - `ApiContractTest` (9): health, claim, stations, trains, followups create, listado filtrado, detalle con episodios, pause (ruta+POST), delete (204).
  - `AuthInterceptorTest` (3). `PairingViewModelTest` (6). `SearchViewModelTest` (12).
  - `FollowUpsViewModelTest` (5): carga con filtro todos, cambio de filtro consulta lifecycle, vacío ≠ error, 503 ≠ vacío, error de red.
  - `FollowUpDetailViewModelTest` (8): última comprobación válida desde el episodio más reciente, 404, pausa recarga desde el servidor, 409 sin éxito falso, acknowledge sin borrar + refresco, error de red, delete OK (1 llamada) y 409 sin borrado.
- `.\gradlew.bat :app:assembleDebug :app:lintDebug --no-daemon` → **BUILD SUCCESSFUL**; APK `app-debug.apk` (11.249.604 bytes), versionName 0.1.0; lint sin errores (solo deprecation warning cosmético heredado de `menuAnchor()`).
- **No validado en dispositivo real** (requiere aprobación e instalación en realme GT Neo 2; para flujo completo: backend local `uvicorn`).

### Bloqueos
- Ninguno para el paso 23. Siguen pendientes de aprobación externa: despliegue en VM (paso 06) y validación en dispositivo real. Nota: el backend no expone `last_check`/`updated_at`; la "última comprobación válida" usa el episodio más reciente. Si se necesita el instante real de la última consulta válida sin plazas, habría que exponer `updated_at` desde la API (cambio de backend, fuera del alcance de este paso).

### Siguiente paso
No avanzar: siguiente archivo `24-*.md` (esperar instrucciones).

## Paso 24 - Notificaciones locales y FCM en Android (24-notificaciones-android.md) COMPLETADO

### Estado
**Implementado + probado localmente (tests unitarios + MockWebServer) + APK debug compilado.** Sin validación en dispositivo real (pendiente de aprobación). FCM real marcado como pendiente (paso 33: google-services.json / proyecto Firebase).

### Decisiones adoptadas
- **FCM sin google-services**. No hay plugin de Google Services ni `google-services.json` (Firebase real pendiente, paso 33). Toda interacción con Firebase/Messaging va protegida con `FirebaseApp.getApps(context).isNotEmpty()` y try/catch: la app funciona y no crashea sin configuración; el registro de token y el servicio `RenfeMessagingService` se activan solo cuando haya configuración real.
- **Contrato con el backend respetado**: `PUT /api/v1/fcm/token` (endpoint autenticado, no es ruta pública del AuthInterceptor) con body `{"fcm_token": "..."}` (patrón `^[A-Za-z0-9_:-]{20,4096}$`) y respuesta `{"registered": bool}`; 409 si el dispositivo está inactivo. `docs/notificaciones-fcm.md` y `backend/app/api/fcm.py` son la referencia (no se ha tocado backend).
- **Payload data-only** (`AlertPayload`): claves `type` (`alert`|`test`), `event_id`, `followup_id`, `episode_id`, `title`, `body`, `channel_id` (`disponibilidad_plazas`|`resumen_y_servicio`), `priority`, `origin_code`/`origin`/`destination_code`/`destination`/`travel_date`/`departure`/`arrival`/`price`/`observed_at`/`expires_at`. Los `test` usan `"-"` como placeholder. `observed_at`/`expires_at` se parsean como instantes ISO-8601 con offset (`Instant.from(OffsetDateTime.parse(...))`); `"-"`/ausentes → null.
- **Canales**: `disponibilidad_plazas` (IMPORTANCE_HIGH, sonido + vibración, `AudioAttributes` USAGE_ALARM/CONTENT_TYPE_SONIFICATION) y `resumen_y_servicio` (IMPORTANCE_LOW, silencioso). Se crean en `RenfeNotifierApp.onCreate`. `NotificationChannels.openSystemSettings` abre los ajustes de notificaciones de la app (banner de Home).
- **Dedupe persistente del evento**: `PrefsEventIdStore` guarda `event_id` entregados en SharedPreferences (`delivered_event_ids`/`ledger`, líneas `id|epoch`); `EventLedger` puro con capacidad 500 y ventana de 30 días. Un episodio con `observed_at` más antiguo que 30 min (`STALE_WINDOW`) o con `expires_at` pasado se rechaza; los `test` (sin timestamps) se entregan siempre.
- **Acciones en la notificación**:
  - "Abrir" → `MainActivity` recoge `EXTRA_FOLLOWUP_ID` y `pendingFollowupId` (StateFlow) → `AppNavHost` navega a `followup/{id}`.
  - "Confirmar" y "Pausar" → `NotificationActionReceiver` (BroadcastReceiver) encola `FollowUpActionWorker` (WorkManager único `followup-action:<acción>:<id>`, KEEP, backoff 15 s) que llama a `acknowledge`/`pause` del backend con autenticación. Reintentos acotados: `NetworkPolicy` (reintentable ante IOException/429/5xx, máx. 3 intentos; 404/409 = éxito idempotente; 401 = revocación de credencial). Si se agota, `NotificationDisplayer.showActionFailed` lo avisa en el canal de servicio.
  - "Cancelar" no: sin acción explícita definida; "Pausar" cubre dejar de avisar sin borrar el seguimiento.
- **Registro y renovación del token**: `FcmTokenRegistrationWorker` (único `fcm-token-registration`, REPLACE, backoff exponencial 30 s) se encola al arrancar emparejado, al detectar emparejamiento (`Application` observa `isPaired` con `distinctUntilChanged`) y desde `onNewToken`. Sin polling ni Workers periódicos.
- **POST_NOTIFICATIONS (Android 13+/API 33)**: solicitud única al arranque (flag `permission_state`/`notifications_permission_asked` en prefs); si se deniega, Home muestra banner con botón "Abrir ajustes" y refresca al volver (`LifecycleResumeEffect`).
- **Sin duplicados en primer/segundo plano**: el dedupe vive en el servicio de mensajería (ruta única) tanto si entran por FCM foreground (aunque no se muestra) como background; si Firebase no está configurado y entra un mensaje, se ignora sin aviso.
- **Sin polling/wake locks/WebSocket/servicio permanente**: cumplido por construcción (mensajería por FCM + WorkManager de un solo disparo para acciones/registro).

### Archivos creados/modificados
- Creados (main):
  - `core/notifications/AlertPayload.kt` (payload data-only + `AlertPayloadParser` con frescura/caducidad)
  - `core/notifications/EventIdStore.kt` (`EventLedger` puro + `PrefsEventIdStore`)
  - `core/notifications/NotificationChannels.kt` (canales + `openSystemSettings`)
  - `core/notifications/NotificationDisplayer.kt` (notificación alert + `showActionFailed`)
  - `core/notifications/FcmTokenGateway.kt` (`isConfigured`, `obtainToken`, `onMessage` con dedupe)
  - `core/notifications/NetworkPolicy.kt` (reintentos puro)
  - `core/notifications/FcmTokenRegistrationWorker.kt` y `core/notifications/FollowUpActionWorker.kt` (WorkManager)
  - `core/notifications/NotificationActionReceiver.kt` (BroadcastReceiver de acciones)
  - `core/notifications/RenfeMessagingService.kt` (FirebaseMessagingService, con guards sin Firebase)
- Modificados:
  - `RenfeNotifierApp.kt` (Application: canales, `ApiModule.init`, observa emparejamiento → registra token)
  - `MainActivity.kt` (launcher POST_NOTIFICATIONS + `pendingFollowupId` desde intent)
  - `navigation/AppNavHost.kt` (`pendingFollowupId: StateFlow<String?>?` → navega a detalle)
  - `feature/home/HomeScreen.kt` (banner denegación + "Abrir ajustes" + refresh on resume)
  - `core/network/RenfeApi.kt` (`registerFcmToken` PUT), `core/network/model/ApiModels.kt` (`FcmTokenRequest`/`FcmTokenResponse`)
  - `AndroidManifest.xml` (permiso POST_NOTIFICATIONS, `android:name=".RenfeNotifierApp"`, servicio MESSAGING_EVENT `exported=false`, receiver de acciones `exported=false`)
  - `res/values/strings.xml` (`home_notifs_*`, `notif_*`), `gradle/libs.versions.toml` + `app/build.gradle.kts` (firebase-bom 33.7.0, firebase-messaging, work-runtime-ktx 2.9.1, coroutines-play-services, fragment-ktx 1.8.5)
- Creados/modificados (test):
  - `core/notifications/AlertPayloadParserTest.kt` (11 tests)
  - `core/notifications/EventLedgerTest.kt` (6 tests)
  - `core/notifications/NetworkPolicyTest.kt` (6 tests)
  - `core/network/FakeRenfeApi.kt` (`registerFcmTokenHandler` + override)
  - `core/network/ApiContractTest.kt` (+2: PUT fcm_token snake_case + registered; 409 ≠ éxito)

### Pruebas ejecutadas y resultados (evidencia)
- `.\gradlew.bat :app:testDebugUnitTest --no-daemon` → **BUILD SUCCESSFUL**; **68 tests, 0 fallos** en 9 suites:
  - `ApiContractTest` (11): +2 de registro de token FCM (PUT `/api/v1/fcm/token`, cuerpo `fcm_token` + `registered:true`; 409 lanza `HttpException`).
  - `AuthInterceptorTest` (3). `PairingViewModelTest` (6). `SearchViewModelTest` (12). `FollowUpsViewModelTest` (5). `FollowUpDetailViewModelTest` (8).
  - `AlertPayloadParserTest` (11): versión con todas las claves, origin/destination placeholders, test "con placeholder ignorado", timestamp ISO con offset parseado, observed_at nulo/ausente, `"-"`→null, caducidad pasada, stale 30 min, alert ≠ test siempre entregado, channel_id ausente → canal por defecto.
  - `EventLedgerTest` (6): entrega duplicada detectada, corte de la ventana de 30 días, poda por `remember` y `prune` explícito, restauración desde snapshot, capacidad 500.
  - `NetworkPolicyTest` (6): IOException reintentable; 429/5xx reintentables; 4xx no; 404/409 éxito idempotente; 401 revocación; corte tras 3 intentos.
- `.\gradlew.bat :app:assembleDebug :app:lintDebug --no-daemon` → **BUILD SUCCESSFUL**; APK `app-debug.apk` (12.619.098 bytes), versionName 0.1.0; lint **sin errores** (47 warnings heredados de "hay versión más nueva" + `menuAnchor()` deprecado + recursos sin uso; se corrigió además `InvalidFragmentVersionForActivityResult` añadiendo `fragment-ktx` y el guard `ObsoleteSdkInt` en canales).
- Correcciones durante el paso: constantes requestCode (`REQUEST_OPEN`/`REQUEST_OPEN_HOME`) en `NotificationDisplayer`; test de ApiContract con cuerpo expresión (JUnit exige `void`); test de poda de `EventLedger` (el `remember("reciente")` ya podaba "viejo", se usa `restore` para el caso explícito).
- **No validado en dispositivo real** (requiere aprobación e instalación en realme GT Neo 2). FCM real (proyecto Firebase + `google-services.json`) pendiente del paso 33.

### Bloqueos
- **FCM real pendiente**: infraestructura Firebase (proyecto, `google-services.json`) necesaria para obtener token y recibir mensajes reales; la app no crashea sin ella (guards + try/catch). Sin Firebase no es posible validar recepción en primer/segundo plano ni la renovación de token contra Google; sí se revisó que el manifiesto/servicio/receiver compilan y el registro HTTP cumple el contrato con el backend (MockWebServer).
- Siguen pendientes de aprobación externa: despliegue en VM (paso 06) y validación en dispositivo real.

### Siguiente paso
No avanzar: siguiente archivo `25-*.md` (esperar instrucciones).

## Paso 25 - Diagnóstico y ajustes (25-diagnostico-y-ajustes.md) COMPLETADO

### Estado
**Implementado y probado localmente** (backend + JVM). No validado en dispositivo real.

### Decisiones adoptadas
- **Backend, mínimo necesario**: nuevo `backend/app/monitoring.py` (`Monitoring` con `threading.Lock` + instancia module-level `monitoring`) con `record_logical_query()` y `record_search(http_requests, bytes_received)`; el planificador registra cada grupo comprobado y las peticiones de cada búsqueda exitosa (`SchedulerService._check_group`). `GET /api/v1/diagnostics` amplía su respuesta con `search_stats{logical_queries, http_requests, bytes_received}` (desde el reinicio del proceso). No se crearon endpoints adicionales ni capas innecesarias.
- **Android - diagnóstico**: nuevos `core/diagnostics/ServerContactStore.kt` (SharedPreferences `server_contact` con espejo `AtomicLong`) y `HeartbeatInterceptor.kt` (OkHttp) que registran la última respuesta 2xx del backend; conectados al cliente por defecto de `ApiModule`. `core/diagnostics/DeviceEnvironment.kt`: `interface DeviceEnvironment` + `AndroidDeviceEnvironment(context)` para Google Play services (`GoogleApiAvailability`, requiere `com.google.android.gms:play-services-base:18.5.0`), permiso POST_NOTIFICATIONS (API<33 true), canal de avisos activo (`importance != IMPORTANCE_NONE`), FCM configurado (`FcmTokenGateway.isConfigured`) y última comprobación válida. Sin Firebase real, la etapa de "Entrega" se muestra con atención (honesto, no inventa verificación).
- **Android - pantalla**: `feature/diagnostics/` con `DiagnosticsViewModel` (loading inicial `false`, recarga que no hace nada si ya carga), `computeStages()` puro (5 etapas QUERY/DETECTION/SEND/RECEIVE/NOTIFY con `StageStatus {OK, ATTENTION, BLOCKED, UNKNOWN}` y detalle en español) y `DiagnosticsScreen` (etapas con badge de color, servidor, cifras diferenciando consultas lógicas vs peticiones HTTP, notificación de prueba identificada con su `message_id`, preferencias de tema y avisos, nota de force-stop). Nuevo destino `diagnostics` en `AppNavHost` + botón en `HomeScreen`. Nunca muestra tokens ni credenciales.
- **Android - ajustes**: `PreferencesRepository` implementa además `SettingsSource` (`theme` system/light/dark y `alertsEnabled`, setters, mismo DataStore `app_preferences`, un solo companion object). `MainActivity` aplica el tema (light→false, dark→true, system→`isSystemInDarkTheme()`) y engancha `FcmTokenGateway.alertsEnabledProvider`; `FcmTokenGateway.onMessage` silencia sin marcar como entregado si los avisos están desactivados.
- **No es un endpoint de seguimientos**: la pantalla llama `GET /api/v1/diagnostics` y `POST /api/v1/diagnostics/test-notification` (auth). Comporta estado renovado del backend con `FakeSender` (`message-test-1`).

### Archivos creados/modificados
- Backend (creado `backend/app/monitoring.py`, `backend/tests/test_monitoring.py`; modificado `scheduler/service.py`, `api/diagnostics.py` con `SearchStats`, `tests/test_diagnostics_api.py` con `search_stats`).
- Android (creado): `core/diagnostics/{ServerContactStore,HeartbeatInterceptor,DeviceEnvironment}.kt`; `feature/diagnostics/{DiagnosticsViewModel,DiagnosticsScreen}.kt`.
- Android (modificado): `core/network/model/ApiModels.kt` (`FollowUpCountsOut`, `SearchStatsOut`, `DiagnosticsOut`, `TestNotificationOut`), `core/network/RenfeApi.kt` (`diagnostics()`, `sendTestNotification()`), `core/network/ApiModule.kt` (heartbeat en `httpClient` + `serverContactStore`), `core/security/PreferencesRepository.kt` (`SettingsSource` + theme/alertsEnabled), `MainActivity.kt` (tema + `alertsEnabledProvider`), `core/notifications/FcmTokenGateway.kt` (gate de avisos), `feature/home/HomeScreen.kt` (botón), `navigation/AppNavHost.kt` (destino), `res/values/strings.xml` (diag_* + `home_diagnostics_button`), `gradle/libs.versions.toml` + `app/build.gradle.kts` (`google-play-services-base` 18.5.0).
- Tests Android: `core/network/FakeRenfeApi.kt` (+`diagnosticsHandler`/`sendTestNotificationHandler`), `core/network/ApiContractTest.kt` (+2), `feature/diagnostics/DiagnosticsViewModelTest.kt` (12, con $FakeDeviceEnvironment$/$FakeSettingsSource$).

### Pruebas ejecutadas y resultados (evidencia)
- Backend: `.\\.venv\\Scripts\\python.exe -m pytest tests -q` → **166 passed** (3 nuevos de `monitoring`, `search_stats` en diagnostics). Se corrigió el import (la instancia module-level, no el módulo).
- Android: `.\gradlew.bat :app:testDebugUnitTest --no-daemon` → **82 tests, 0 fallos** en 10 suites (nuevas: DiagnosticsViewModelTest 12; ApiContractTest 13). `assembleDebug` → APK `app-debug.apk` 12.685.305 bytes. `lintDebug` → **sin errores** (warnings de "hay versión más nueva", incluido `play-services-base 18.5.0`→18.10.1, + `ModifierParameter` en HomeContent y recursos sin uso, todos no bloqueantes).
- Correcciones durante el paso: rol del flag `loading` (inicial `true` impedía la primera carga), `environment` como propiedad del ViewModel, un único `companion object` en `PreferencesRepository`.
- **No validado en dispositivo real** ni en VM. FCM real pendiente del paso 33: la pantalla refleja la entrega como no verificable sin Firebase.

### Bloqueos
- FCM real (Firebase + `google-services.json`) pendiente del paso 33: sin él la entrega/visualización reales no se pueden validar ni la "notificación de prueba" puede verificarse extremo a extremo (el registro HTTP sí cumple el contrato; `FakeSender` responde `message_id` de prueba).
- Despliegue VM (paso 06) y validación en dispositivo real (realme GT Neo 2) pendientes de aprobación externa.

### Siguiente paso
No avanzar: siguiente archivo `26-*.md` (esperar instrucciones).

## Paso 26 - CI del backend (26-ci-backend.md) COMPLETADO

### Estado
**Workflow creado y validado localmente (sintaxis YAML + las cuatro comprobaciones del workflow replicadas en local).** Sin push ni habilitación de ejecución remota en GitHub (requieren aprobación explícita, regla 4.2).

### Decisiones adoptadas
- **Workflow único `backend-ci`** en `.github/workflows/backend-ci.yml`, dedicado al backend (el orquestador unificado con CI Android de `docs/ci-cd.md` se materializará cuando exista el workflow Android).
- **Disparo y filtros**: `push` y `pull_request` sobre `main`. Detección de cambios con `dorny/paths-filter` sobre paths `backend/**` y `.github/workflows/backend-ci.yml`. No existe `data/stations.json` (el catálogo de estaciones vive en el código/API), así que no se referencia un path inexistente que hubiera "activado" el build sin motivo o roto el filtrado.
- **Sin checks bloqueados por filtros**: job `all-checks-ok` con `needs: backend-ci` y `if: always()` que aprueba si backend-ci terminó en `success` o `skipped` y falla si terminó en `failure`/`cancelled`.
- **Python 3.11**: `requirements.lock` está resuelto en 3.11 (cabecera del lock y pruebas locales). Desviación deliberada del borrador de `docs/ci-cd.md` que citaba 3.12.
- **Instalación reproducible solo desde `requirements.lock`** (incluye pytest, ruff, mypy). No hace falta hatchling ni `pip install -e .` porque los tests se ejecutan desde `backend/` con `python -m pytest` (cwd en sys.path).
- **Acciones fijadas por SHA** con comentario de versión: `actions/checkout@11d5960a326750d5838078e36cf38b85af677262` (v4.4.0), `actions/setup-python@a26af69be951a213d495a4c3e4e4022e16d87065` (v5.6.0, cache pip con `cache-dependency-path: backend/requirements.lock`), `dorny/paths-filter@0e4a8c6effa4802afeda77dc8d303f8176d7dfad` (v3.0.4).
- **Recurso mínimo y control de coste**: `permissions: contents: read` (GITHUB_TOKEN mínimo), `concurrency` agrupado por `github.ref` con `cancel-in-progress: true` (commits nuevos cancelan el CI obsoleto, ahorra cuota gratuita), `timeout-minutes: 10`, runner `ubuntu-latest`.
- **Sin secretos, sin Renfe real ni producción**: el workflow solo instala del lock y ejecuta ruff/mypy/pytest; los tests usan fixtures y mocks (no hay consultas reales a Renfe).
- **Higiene de formato**: el backend nunca se había pasado por `ruff format` (32 ficheros no formateados). Como el CI diseñado exige `ruff format --check`, se ejecutó `ruff format` sobre `backend/` (cambio mecánico, sin tocar lógica) para que el check sea verde desde el primer run.

### Archivos creados/modificados
- Creado: `.github/workflows/backend-ci.yml` (jobs `detect-changes` → `backend-ci` → `all-checks-ok`).
- Modificados: 32 ficheros de `backend/app` y `backend/tests` reformateados por `ruff format .` (solo formato, sin cambios de comportamiento).

### Pruebas ejecutadas y resultados (evidencia)
- **Sintaxis YAML**: parseado con PyYAML y validación programática (triggers, jobs, gate `if: always()`, pinning de SHAs) → **válido**. Nota: PyYAML es YAML 1.1 y mapea la clave `on` a booleano `True`; GitHub Actions usa YAML 1.2 y lo interpreta como string, por lo que un "parseo plano" engaña y conviene validar también el `if` y los `needs`.
- **Comprobaciones del workflow replicadas en local** (venv Python 3.11 del proyecto):
  - `python -m ruff check .` → **All checks passed**.
  - `python -m ruff format --check .` → **65 files already formatted**.
  - `python -m mypy app tests` → **Success: no issues found in 64 source files**.
  - `python -m pytest` → **166 passed**, 2 warnings heredadas (StarletteDeprecationWarning httpx y DeprecationWarning anyio `BlockingPortal`, de dependencias, no bloqueantes).
- **No habilitada la ejecución remota**: no se ha hecho push ni merge, por lo que el workflow no se ha disparado en GitHub (regla 4.2: un push lo activaría; se advertirá antes de cualquier push).

### Bloqueos
- **Push/habilitación del CI en GitHub**: requiere aprobación explícita; un push a `main` dispararía el workflow (se generaría la primera ejecución real, a validar).
- Siguen pendientes de aprobación externa: despliegue en VM (paso 06), validación en dispositivo real (realme GT Neo 2) y FCM real (paso 33, Firebase + `google-services.json`).

### Siguiente paso
No avanzar: siguiente archivo `27-*.md` (esperar instrucciones).

## Paso 27 - CI de Android (27-ci-android.md) COMPLETADO

### Estado
**Workflow creado y validado localmente (sintaxis YAML + lintDebug + testDebugUnitTest + assembleDebug replicados en local).** Sin push ni habilitación de ejecución remota en GitHub (requieren aprobación explícita, regla 4.2).

### Decisiones adoptadas
- **Workflow `android-ci`** en `.github/workflows/android-ci.yml`, dedicado a Android (el orquestador unificado de `docs/ci-cd.md` quedará para cuando ambos workflows existan y se decida unificarlos).
- **Disparo y filtros**: `push` y `pull_request` sobre `main`. Detección de cambios con `dorny/paths-filter` sobre paths `android/**` y `.github/workflows/android-ci.yml`. Filtros coherentes con el backend: cada workflow vigila su propia área + su propio archivo de workflow.
- **Sin checks bloqueados por filtros**: job `all-checks-ok` con `needs: android-ci` y `if: always()` que aprueba si android-ci terminó en `success` o `skipped` y falla si terminó en `failure`/`cancelled`.
- **Entorno y versiones compatibles con compilación local**: `ubuntu-latest`, JDK 17 (Temurin), Gradle 8.9 (wrapper), AGP 8.5.2, Kotlin 2.0.21 — coincide con la compilación local (JDK Temurin 17.0.20.1, SDK android-34).
- **Caché**: `actions/setup-java` con `cache: 'gradle'` + `gradle/actions/setup-gradle` (v4.2.2) para dependencias y wrapper.
- **Pipeline**: `lintDebug` (análisis estático) → `testDebugUnitTest` (82 tests unitarios JVM con MockWebServer) → `assembleDebug` (APK debug). Sin emuladores, sin matrices.
- **Artefacto**: APK debug subido con `actions/upload-artifact` (retención 1 día, ahorra cuota gratuita frente a 3 días del diseño).
- **Acciones fijadas por SHA** con comentario de versión: checkout v4.4.0, setup-java v4.7.1, setup-gradle v4.2.2, paths-filter v3.0.4, upload-artifact v4.6.2.
- **Permisos mínimos y control de coste**: `permissions: contents: read`, `concurrency` por ref con `cancel-in-progress: true`, `timeout-minutes: 15`, runner `ubuntu-latest`.
- **Sin secretos de producción**: la compilación debug funciona sin `google-services.json` (guards en `FcmTokenGateway` y `RenfeMessagingService`). Documentada la provisión de Firebase para CI real en `docs/ci-cd.md` §2.3.1 (Base64 secret + workflow manual, nunca en repo ni caché).

### Archivos creados/modificados
- Creado: `.github/workflows/android-ci.yml` (jobs `detect-changes` → `android-ci` → `all-checks-ok`).
- Modificado: `docs/ci-cd.md` (añadida §2.3.1 con documentación de Firebase en CI, retención APK a 1 día).

### Pruebas ejecutadas y resultados (evidencia)
- **Sintaxis YAML**: parseado con PyYAML y validación programática (triggers, jobs, gate `if: always()`, pinning de SHAs) → **válido**.
- **Comprobaciones del workflow replicadas en local** (JDK Temurin 17 local, Gradle wrapper 8.9):
  - `./gradlew :app:lintDebug --no-daemon` → **BUILD SUCCESSFUL** (sin errores, solo warnings heredados).
  - `./gradlew :app:testDebugUnitTest --no-daemon` → **BUILD SUCCESSFUL** (82 tests, 0 fallos en 10 suites: ApiContract 13, DiagnosticsViewModelTest 12, AuthInterceptor 3, PairingVM 6, SearchVM 12, FollowUpsVM 5, FollowUpDetailVM 8, AlertPayloadParser 11, EventLedger 6, NetworkPolicy 6).
  - `./gradlew :app:assembleDebug --no-daemon` → **BUILD SUCCESSFUL**; APK `app/build/outputs/apk/debug/app-debug.apk` (12.685.305 bytes), versionName 0.1.0, versionCode 1.
- **No habilitada la ejecución remota**: no se ha hecho push ni merge; el workflow no se ha disparado en GitHub (regla 4.2: un push a main lo activaría; se advertirá antes de cualquier push).

### Bloqueos
- **Push/habilitación del CI en GitHub**: requiere aprobación explícita; un push a `main` dispararía el workflow.
- Siguen pendientes de aprobación externa: despliegue en VM (paso 06), validación en dispositivo real (realme GT Neo 2) y FCM real (paso 33, Firebase + `google-services.json`).

### Siguiente paso
No avanzar: siguiente archivo `28-*.md` (esperar instrucciones).

## Paso 28 - Backup y recuperación (28-backup-y-recuperacion.md) COMPLETADO

### Estado
**Implementado y probado localmente** (backup WAL-aware, verificación integridad, restore, versión esquema, migraciones controladas). Sin tocar la base del bot ni la VM de producción.

### Decisiones adoptadas
- **Backup coherente (WAL-aware)**: usa la API *online backup* de SQLite (`Connection.backup()`) que garantiza estado consistente aunque haya cambios pendientes en WAL. No es copia ciega de fichero.
- **Verificación de integridad**: `PRAGMA integrity_check` antes de restaurar + SHA256 del backup para auditoría.
- **Restore seguro**: valida backup antes de sobrescribir; `--force` requerido si BD destino existe; tras restore ejecuta `apply_migrations()` para compatibilidad de esquema.
- **Versiones por commit**: tabla `schema_migrations` + CLI `version` para auditoría; versión actual 5.
- **Datos fuera de releases**: BD en `data/` (gitignored), no en paquete desplegable.
- **Migraciones idempotentes**: solo aditivas, `apply_migrations` salta versiones ya aplicadas.
- **Rollback sin pérdida silenciosa**: restore falla si verificación no pasa; no toca BD original.

### Archivos creados/modificados
- Modificado: `backend/app/db/connection.py` (añadidas `verify_database_integrity`, `get_database_version`).
- Modificado: `backend/app/cli.py` (nuevos comandos: `backup`, `restore`, `verify`, `version`).
- Creado: `docs/recovery.md` (procedimientos, pruebas, integración en despliegue, checklist).

### Pruebas ejecutadas y resultados (evidencia)
- **CLI `backup`**: crea `data/renfe-notifier.db.bak`, muestra SHA256 y versión esquema (5).
- **CLI `verify`**: `integrity_check: ok` en BD original y backup.
- **CLI `version`**: muestra versión 5 correctamente.
- **CLI `restore --force`**: restaura en mismo archivo y en BD temporal nueva; post-restore `verify` y `version` OK.
- **Ciclo completo**: BD original → backup → verify → borrar BD → restore → verify → version → todo OK.
- **Tests backend**: `pytest -q` → **166 passed** (sin regresiones).

### Bloqueos
- Pruebas en VM de producción (paso 06) pendientes de aprobación para validar en entorno real.
- Pruebas de rollback automático en despliegue (docs/ci-cd.md §3.3) requieren VM.

### Siguiente paso
No avanzar: siguiente archivo `29-*.md` (esperar instrucciones).

## Paso 29 - CD Backend (29-despliegue-backend.md) COMPLETADO

### Estado
**Workflow y script de despliegue creados y validados localmente (sintaxis, controles, migraciones).** NO se ha desplegado ni configurado la VM (requiere aprobación paso 06). Sin secretos en el repo.

### Decisiones adoptadas
- **workflow_dispatch** con inputs obligatorios: `deploy_sha` (SHA completo) + `confirm` ("DEPLOY" literal).
- **Validación criptográfica del commit**: verifica vía GitHub API que el SHA está en `main` y tiene todos los checks de CI en `success` (jobs `backend-ci` + `all-checks-ok`). Aborta antes de tocar infraestructura si falla.
- **Serialización estricta**: concurrency `production-deploy` con `max-parallel: 1` (sin cancelación durante migraciones).
- **OIDC + IAP**: autenticación GCP sin claves SSH (`google-github-actions/auth` + `setup-gcloud` + `gcloud compute ssh --tunnel-through-iap`). VM sin IP pública, sin puertos abiertos.
- **Script de despliegue transaccional** (`scripts/deploy_backend.sh`):
  1. Backup SQLite `.backup` atómico (coherente con WAL) + `integrity_check` + SHA256.
  2. Checkout SHA exacto (guarda commit anterior para rollback).
  3. Migraciones (`python -m app.db.migrate`) — fallo → rollback código + BD.
  4. `docker compose up -d` — fallo → rollback código + BD + servicio.
  5. Health check `GET /api/v1/diagnostics/health` (30s, reintentos 2s) — fallo → rollback completo automático (código, BD, servicio).
- **Secretos fuera de paquetes**: configuración vía GitHub Secrets (`GCP_PROJECT_ID`, `GCP_WORKLOAD_IDENTITY_PROVIDER`, `GCP_SERVICE_ACCOUNT`, `VM_NAME`, `VM_ZONE`, `DEPLOY_USER`). Ningún secreto en imagen Docker ni artefactos.
- **Módulo migraciones CLI**: `backend/app/db/migrate.py` (wrapper `apply_migrations`).
- **Job de validación local** (`validate-local`): prueba sintaxis script, mock de validación commit, sin secretos.

### Archivos creados/modificados
- Creado: `.github/workflows/backend-cd.yml` (jobs `validate-and-deploy` + `validate-local`).
- Creado: `scripts/deploy_backend.sh` (despliegue transaccional con rollback automático).
- Creado: `backend/app/db/migrate.py` (CLI migraciones para VM).

### Pruebas ejecutadas y resultados (evidencia)
- **YAML**: parseado con PyYAML → válido (triggers, concurrency, permisos OIDC, pasos).
- **Script `deploy_backend.sh`**: `bash -n` → sintaxis OK.
- **Módulo `app.db.migrate`**: ejecuta migraciones correctamente en BD local.
- **Validación commit mock**: `git merge-base --is-ancestor HEAD origin/main` → True.
- **Tests backend**: `pytest -q` → **166 passed** (sin regresiones).

### Bloqueos
- **Despliegue real en VM (paso 06)**: requiere aprobación externa para crear/usar VM e2-micro, configurar OIDC/WIF, IAP, Tailscale, secretos GitHub.
- **Validación de conectividad OIDC+IAP**: no probada end-to-end sin VM.
- Pendientes: validación en dispositivo real (realme GT Neo 2), FCM real (paso 33).

### Siguiente paso
No avanzar: siguiente archivo `30-*.md` (esperar instrucciones).

## Paso 30 - Firma y distribución APK (30-firma-y-distribucion-apk.md) COMPLETADO

### Estado
**Workflow de release creado y validado localmente (compilación release sin firmar, lint, tests).** NO se ha generado keystore ni configurado secrets (requiere aprobación para crear keystore y secrets GitHub). Sin claves en el repo.

### Decisiones adoptadas
- **Triggers duales**: `workflow_dispatch` (manual con inputs `version_name`, `version_code`, confirmación "RELEASE") + `push` tags `v*.*.*` (release automática tras validar CI).
- **Validación estricta**: en tags, verifica vía GitHub API que el commit está en `main` y tiene CI `success` (backend-ci, android-ci, all-checks-ok). En dispatch manual, requiere confirmación literal "RELEASE".
- **Serialización**: concurrency `release-${{ github.ref }}` con `max-parallel: 1` (sin cancelación durante firma).
- **Keystore en secret base64**: `ANDROID_KEYSTORE_BASE64` + `KEYSTORE_PASSWORD`, `KEY_ALIAS`, `KEY_PASSWORD` en GitHub Secrets. Decodificado en `${{ runner.temp }}/keystore.jks` (memoria temporal).
- **Firma y verificación**: `assembleRelease` con signingConfig condicional → `apksigner verify --print-certs`.
- **Checksum SHA-256**: generado y subido junto al APK (`.sha256`).
- **Distribución**: artefactos (retención 5 días) + Release privada GitHub (solo en tags) con APK y checksum.
- **Limpieza segura**: `shred -u` / `rm -f` del keystore temporal en `if: always()`.
- **Sin PRs**: workflow no se dispara desde pull requests (solo tags main + dispatch manual).
- **Documentación**: `docs/keystore.md` con generación, backup seguro (2+ ubicaciones físicas), rotación, verificación manual.
- **SigningConfig condicional**: `build.gradle.kts` solo aplica firma si variables de entorno presentes (compilación local sin firmar OK).

### Archivos creados/modificados
- Creado: `.github/workflows/android-release.yml` (jobs `validate-and-build` + `validate-local`).
- Creado: `docs/keystore.md` (guía completa generación, backup, uso).
- Modificado: `android/app/build.gradle.kts` (signingConfig condicional por env vars).

### Pruebas ejecutadas y resultados (evidencia)
- **YAML**: parseado con PyYAML → válido (triggers, permisos, pasos firma, limpieza, pinning SHA).
- **Compilación release local (sin firmar)**: `./gradlew :app:assembleRelease --no-daemon` → **BUILD SUCCESSFUL** (APK sin firmar generado).
- **Lint + Tests**: `./gradlew :app:lintDebug :app:testDebugUnitTest --no-daemon` → **BUILD SUCCESSFUL** (82 tests, 0 fallos).
- **Backend tests**: `pytest -q` → **166 passed** (sin regresiones).
- **Validación local job**: compila release sin firmar, verifica signingConfig presente.

### Bloqueos
- **Generación keystore + secrets GitHub**: requiere tu aprobación para ejecutar `keytool` local y configurar 4 secrets en GitHub (paso previo a cualquier release real).
- **Release real**: necesita keystore válido y secrets; workflow listo pero no ejecutado remotamente (regla 4.2: push a main/tags dispararía CI/CD).
- Pendientes: VM (paso 06), dispositivo real (realme GT Neo 2), FCM real (paso 33).

### Siguiente paso
No avanzar: siguiente archivo `31-*.md` (esperar instrucciones).

## Paso 31 - Validación local integrada (31-validacion-local-integrada.md) COMPLETADO

### Estado
**Validación completa local exitosa (sin servicios reales).** Todos los tests, compilaciones, CLI, contratos y workflows verificados. Documentado en `docs/validation.md`.

### Decisiones adoptadas
- **Alcance**: Solo validación local (Windows, Python 3.11, JDK 17, Gradle 8.9). Cero llamadas a Renfe, FCM, VM.
- **Estrategia de tests**: Backend 166 tests (pytest) + Android 82 tests (JUnit/MockWebServer) cubren todas las áreas del paso: parser/Plaza H/fechas/matching, estados/recordatorios/caducidad/reinicios, agrupación/deduplicación, auth/validación, migraciones/backup/restore, contratos Android/backend con mocks.
- **CLI verificada**: backup (WAL-aware + SHA256), verify (integrity_check), version (schema_migrations v5), restore (con --force, post-restore migraciones), migrate (aplica migraciones a BD nueva).
- **APK debug**: Compila correctamente (12.685.305 bytes, versionName 0.1.0).
- **Workflows (4)**: Todos con pinning SHA, permisos mínimos, concurrencia controlada, restricciones de despliegue verificadas. android-release sin trigger PR confirmado.

### Archivos creados/modificados
- Creado: `docs/validation.md` (evidencia completa, checklist, pendientes).
- Sin cambios en código (solo corrección SHA en `backend-cd.yml` para google-github-actions/auth v2.1.3 y setup-gcloud v2.1.0).

### Pruebas ejecutadas y resultados (evidencia)
- **Backend**: `pytest -q` → **166 passed, 2 warnings** (deprecaciones heredadas starlette/httpx).
  - Áreas clave: `test_stations` (5), `test_dwr_parser` (2), `test_scheduler_plan` (6), `test_scheduler_service` (8), `test_followup_domain` (12), `test_reminders_queue` (18), `test_pairing_database` (11) — **59 passed** en suite focalizada.
  - Migraciones/backup/restore: `test_followup_database` (13), `test_pairing_database` (11) — WAL, idempotencia, plaza_h, episodios, backup consistente, recovery.
- **Android**: `gradlew :app:testDebugUnitTest :app:lintDebug :app:assembleDebug --no-daemon` → **BUILD SUCCESSFUL**.
  - **82 tests, 0 fallos** en 10 suites (ApiContract 13, DiagnosticsViewModel 12, AuthInterceptor 3, PairingVM 6, SearchVM 12, FollowUpsVM 5, FollowUpDetailVM 8, AlertPayloadParser 11, EventLedger 6, NetworkPolicy 6).
  - Lint: sin errores (warnings heredados no bloqueantes).
  - APK debug: 12.685.305 bytes, versionName 0.1.0, versionCode 1.
- **CLI backend**: backup → verify → version → restore → verify → version → **ciclo completo OK**. migrate → version → **OK**.
- **Workflows**: 4 archivos YAML parseados, pinning SHA verificado (checkout v4.4.0, setup-python v5.6.0, setup-java v4.7.1, setup-gradle v4.2.2, paths-filter v3.0.4, upload-artifact v4.6.2, auth v2.1.3, setup-gcloud v2.1.0, create-release v1.1.4, upload-release-asset v1.0.2). android-release sin trigger PR confirmado.

### Bloqueos
- **VM e2-micro + OIDC/IAP** (paso 06/29): pendiente aprobación para crear VM, configurar WIF, IAP, Tailscale, secrets.
- **Firebase Spark + google-services.json** (paso 33/34): pendiente aprobación para crear proyecto Firebase, descargar json, configurar secret CI Android.
- **Keystore release + 4 secrets GitHub** (paso 30): pendiente `keytool` local y configuración secrets.
- **Push a GitHub / CI remoto**: pendiente autorización regla 4.2.
- **Dispositivo real + FCM real** (paso 35): pendiente instalación APK en realme GT Neo 2 + backend accesible + Firebase configurado.

### Siguiente paso
No avanzar: siguiente archivo `32-*.md` (esperar instrucciones).

## Paso 32 - Mediciones y fallback (32-mediciones-y-fallback.md) COMPLETADO

### Estado
**Mediciones locales ejecutadas y fallback Selenium evaluado.** No se requiere Selenium para el caso base (HTTP DWR basta). Documentado en `docs/measurements.json`.

### Decisiones adoptadas
- **Mediciones locales reproducibles**: CPU, RAM, disco, duración con 1 y 5 seguimientos, con/sin agrupación, peticiones y bytes observables.
- **Resultados clave**:
  - 1 seguimiento: ~0.06s, 5 req, 12 KB, 1 query lógica
  - 5 seguimientos sin agrupación (5 grupos): ~0.06s, 10 req, 25 KB, 2 queries
  - 5 seguimientos con agrupación (1 grupo): ~0.15s, 5 req, 12 KB, 1 query
  - Ahorro por agrupación (5→1 grupo): **50% menos peticiones y bytes**
  - Concurrencia 1 vs 2: tiempo similar (~0.14s)
- **Proyección VM e2-micro (1 GB RAM, 1 vCPU)**:
  - RAM base app: ~43 MB
  - RAM por ciclo: < 1 MB
  - CPU promedio: < 15%
  - Con latencia real Renfe (~300ms/req): 1 grupo = 1.5s/ciclo; 5 grupos = 7.5s/ciclo
- **Evaluación fallback Selenium**: **NO necesario**. HTTP DWR funciona; Selenium añadiría 150-300 MB RAM (Chrome headless) en e2-micro (1 GB), riesgo OOM. Margen con solo app: ~781 MB para SO + buffer.
- **Mediciones reales en e2-micro pendientes** (requieren VM aprobada): latencia real Renfe desde GCP, CPU/RAM carga sostenida 1h, GC Python bajo carga, SQLite WAL concurrente, bytes facturados vs app, arranque docker compose + health check.

### Archivos creados/modificados
- Creado: `docs/measurements.json` (resultados completos, análisis, proyección VM, evaluación Selenium)
- Modificado: `backend/app/db/migrations.py` (CREATE TABLE IF NOT EXISTS, IF NOT EXISTS en índices, plaza_h en migración 1, migración 2 solo índice, ALTER TABLE fcm_token idempotente via manejo de errores)
- Modificado: `backend/app/db/connection.py` (métodos `verify_database_integrity`, `get_database_version`)
- Modificado: `backend/app/cli.py` (comandos `backup`, `restore`, `verify`, `version`)
- Modificado: `backend/app/db/migrate.py` (nuevo CLI para migraciones)
- Modificado: `backend/app/db/migrations.py` (manejo idempotente de errores "duplicate column"/"already exists")
- Modificado: `backend/app/followups/database.py` (initialize verifica existencia real de tablas)
- Modificado: `backend/app/pairing/database.py` y `backend/app/reminders/queue.py` (guardas de inicialización)
- Creado: `scripts/measure_resources.py` (script de medición local)
- Creado: `docs/measurements.json` (evidencia estructurada)
- Creado: `docs/validation.md` (actualizado con checklist paso 31)

### Pruebas ejecutadas y resultados (evidencia)
- **Backend**: `pytest -q` → **166 passed, 2 warnings** (deprecaciones heredadas starlette/httpx)
- **CLI backup/restore/verify/version/migrate**: ciclo completo OK en BD temporales y por defecto
- **Script `measure_resources.py`**: 5 escenarios (1/5 seguimientos, con/sin agrupación, concurrencia 1/2) → métricas consistentes
- **Android**: `gradlew :app:testDebugUnitTest :app:lintDebug :app:assembleDebug --no-daemon` → **BUILD SUCCESSFUL**; 82 tests/0 fallos; APK 12.685.305 bytes

### Bloqueos
- **VM e2-micro + OIDC/IAP** (paso 06/29): pendiente aprobación para crear VM, configurar WIF, IAP, Tailscale, secrets
- **Firebase Spark + google-services.json** (paso 33/34): pendiente aprobación para crear proyecto Firebase, descargar json, configurar secret CI Android
- **Keystore release + 4 secrets GitHub** (paso 30): pendiente `keytool` local y configuración secrets
- **Push a GitHub / CI remoto**: pendiente autorización regla 4.2
- **Dispositivo real + FCM real** (paso 35): pendiente instalación APK en realme GT Neo 2 + backend accesible + Firebase configurado

### Siguiente paso
No avanzar: siguiente archivo `33-*.md` (esperar instrucciones).

## Paso 33 - Plan de configuración real (33-plan-configuracion-real.md) COMPLETADO

### Estado
**Plan detallado preparado y documentado en `docs/real-config-plan.md`.** No se ejecuta ningún cambio; solo se prepara el plan con checklist de aprobación. Requiere aprobación explícita por ítem antes de paso 34.

### Decisiones adoptadas
- **Firebase Spark (gratis)**: plan perpetuo sin facturación, FCM HTTP v1, Project ID en secret `RENFE_NOTIFIER_FCM_PROJECT_ID`, google-services.json en secret Android CI (`ANDROID_GOOGLE_SERVICES_JSON_B64`).
- **VM e2-micro + OIDC/IAP**: Free Tier (0 €), WIF para OIDC, IAP para SSH sin IP pública, Tailscale opcional. Cuenta de servicio `deployer@project.iam` con roles mínimos (`compute.instanceAdmin.v1`, `iam.serviceAccountUser`, `firebase.messaging.sender`).
- **TLS/Acceso**: certificado autofirmado para dev / túnel Cloudflare temporal; backend force_https en prod.
- **CD + Release**: workflows `backend-cd` (OIDC+IAP) y `android-release` (tags + dispatch) ya implementados; secrets GitHub definidos.
- **Keystore**: generación local `keytool`, backup en 2+ ubicaciones físicas (USB encriptado + gestor contraseñas), secrets Android release (`ANDROID_KEYSTORE_BASE64`, passwords).
- **Secrets GitHub consolidados**: 12 secrets categorizados por sensibilidad (ALTA/MEDIA/BAJA), documentados en plan.
- **Distribución APK**: Release privada GitHub (APK + SHA256), instalación manual en realme GT Neo 2.
- **Mediciones VM pendientes**: 7 ítems (latencia Renfe, CPU/RAM 1h, GC Python, SQLite WAL, bytes facturados, arranque docker) → requieren VM aprobada.
- **Selenium**: NO implementar salvo que HTTP DWR falle en VM real; plan B documentado.
- **Coste total: 0 €** (Free Tier GCP, Firebase Spark, GitHub Actions, Tailscale personal).

### Archivos creados/modificados
- Creado: `docs/real-config-plan.md` (plan completo con 10 secciones, checklist §8, coste 0 €, reversión global).
- Referencia: `docs/notificaciones-fcm.md` §7, `docs/ci-cd.md` §3, `docs/keystore.md`, `docs/measurements.json`, `docs/keystore.md`.

### Pruebas ejecutadas y resultados (evidencia)
- **Validación de plan**: Revisión cruzada con `docs/notificaciones-fcm.md`, `docs/ci-cd.md`, `docs/keystore.md`, `docs/measurements.json` — coherencia verificada.
- **Coste 0 € validado**: Free Tier GCP (e2-micro 744h), Firebase Spark, GitHub Actions (2000 min/mes), Tailscale personal, GitHub Packages 500 MB.
- **Reversibilidad**: Cada acción tiene reversión documentada; keystore es único punto irreversible (backup físico 2+ ubicaciones).

### Bloqueos
- **Aprobación explícita requerida** para cada ítem del checklist §8 (Firebase, VM, secrets, keystore, CD, release, dispositivo real, mediciones VM).
- **Push a GitHub / CI remoto**: pendiente autorización regla 4.2 (un push a main dispararía CI/CD).
- **Keystore release**: pendiente `keytool` local + backup físico + secrets GitHub.
- **VM / Firebase / Dispositivo real**: pendientes de aprobación externa.

### Siguiente paso
No avanzar: siguiente archivo `34-*.md` (esperar instrucciones con aprobaciones del checklist §8).

## Linting fix y backend-ci OK (post-paso 33) COMPLETADO

### Estado
**Linting corregido y backend-ci pasando (166 tests, ruff, mypy, format).** 

### Decisiones adoptadas
- **Fix linting en 5 archivos**: `scripts/measure_resources.py`, `backend/app/db/migrate.py`, `backend/app/main.py`, `backend/app/cli.py`, `backend/app/db/connection.py`, `backend/app/db/migrations.py`
- **Correcciones aplicadas**:
  - DTZ011: `date.today()` → `datetime.now(UTC).date()`
  - ASYNC251: `time.sleep()` → `await asyncio.sleep()` en funciones async
  - F541: f-strings sin placeholders eliminadas
  - F401: imports no usados removidos (`Lifecycle`, `Monitoring`, `Station`, `SchedulerService`, `date`)
  - I001: imports organizados (isort/ruff)
  - F541: f-strings sin placeholders removidas
  - W292: newlines finales agregados
  - Formato ruff aplicado a 5 archivos

### Archivos modificados
- `scripts/measure_resources.py`: fixes DTZ011, ASYNC251, F541, F401, I001
- `backend/app/db/migrate.py`: trailing newline
- `backend/app/main.py`: imports organizados, duplicado removido
- `backend/app/cli.py`: formato ruff
- `backend/app/db/connection.py`, `backend/app/db/migrations.py`: formato ruff

### Pruebas ejecutadas y resultados (evidencia)
- **ruff check**: All checks passed
- **ruff format --check**: 66 files already formatted
- **mypy app tests**: Success: no issues found in 65 source files
- **pytest**: 166 passed, 2 warnings (deprecaciones starlette/anyio, heredadas)
- **backend-ci workflow**: ✅ SUCCESS (GitHub Actions)

### Bloqueos
- Mismos que paso 33: requieren aprobaciones checklist §8 (Firebase, VM, secrets, keystore, CD, release, dispositivo real, mediciones VM)

### Siguiente paso
No avanzar: siguiente archivo `34-*.md` (esperar instrucciones con aprobaciones del checklist §8).


## Bloqueos revisados y CI Android verde (post-paso 33) COMPLETADO

### Estado
**Revisados los 5 bloqueos del checklist plan configuracion real. Quedan resueltos Firebase, secrets (incl. typo GDP->GCP), keystore y push/CI. Unico pendiente real: paso 35 (dispositivo real + FCM).**

### Decisiones adoptadas
- **Secret typo corregido**: el usuario renombro el secret GDP_SERVICE_ACCOUNT a GCP_SERVICE_ACCOUNT en GitHub (evita fallo en backend-cd, referencia correcta en backend-cd.yml:113).
- **android-release.yml**: clave invalida max-parallel: 1 en concurrency reemplazada por cancel-in-progress: false (mismo fix que backend-cd); el run previo fallaba al instante (jobs: []). Commit e931b8b.
- **gradlew no ejecutable**: android-ci fallaba en lintDebug con exit 126 (Permission denied) porque ndroid/gradlew tenia modo 100644 en git. Corregido con git update-index --chmod=+x (ahora 100755). Commit 37a8493.

### Archivos modificados/creados
- .github/workflows/android-release.yml: concurrency fix
- ndroid/gradlew: mode change 100644 -> 100755
- PROGRESS.md (2)

### Pruebas ejecutadas y resultados (evidencia)
- **backend-ci [ok] run 35148529308**: ruff check + ruff format + mypy + pytest (166 passed).
- **android-ci [ok] run 35148684913**: detect-changes, lintDebug, testDebugUnitTest, assembleDebug, all-checks-ok todos success.
- **Verificado con gh run view**: jobs and steps all success.

### Bloqueos
- **Paso 35 (dispositivo real + FCM real)**: pendiente instalacion APK en realme GT Neo 2 + backend accesible + Firebase.

### Siguiente paso
No avanzar: siguiente archivo 34-*.md (esperar instrucciones).


# Paso 34 - Configuracion real autorizada (34-configuracion-real-autorizada.md) COMPLETADO

## Estado
Checklist §8 aprobado parcialmente por el usuario (items 1-5: Firebase, VM/OIDC/IAP, secrets, keystore+backup, secrets release). Aplicada la configuracion autorizada del item 1/5 que faltaba (plugin Google Services + decodificacion segura de google-services.json en release). NO ejecutado CD (items 6-9 pendientes de aprobacion).

## Decisiones adoptadas
- Aprobaciones del usuario durante la sesion: item 1 (Firebase Spark + google-services.json), item 2 (VM/OIDC/WIF/IAP, sin Tailscale), item 3 (secrets GitHub), item 4 (keystore + backup USB encriptado + Bitwarden Free), item 5 (secrets release keystore + google-services.json).
- Gap detectado en item 1/5: el secret ANDROID_GOOGLE_SERVICES_JSON_B64 existia pero el build Android NO aplicaba el plugin de Google Services y android-release.yml no lo decodificaba; el secret era inerte y FCM real (item 8/9) habria fallado.
- Plugin com.google.gms.google-services 4.4.2 aplicado de forma CONDICIONAL (en build.gradle.kts raiz como apply false y en app solo si existe google-services.json). Sin el archivo, el build debug/CI sigue funcionando como antes sin Firebase.
- android-release.yml: nuevo paso 'Decodificar google-services.json desde secret base64' (guardado por env, valida con jq project_id y mobilesdk_app_id) + limpieza segura con shred del json al final.
- Seguridad: google-services.json nunca se commitea (gitignore) y se limpia tras el build. Ningun secret en repo. PUSH NO REALIZADO (requiere autorizacion y activaria CI).

## Archivos modificados
- android/gradle/libs.versions.toml: googleServices = 4.4.2 y plugin google-services
- android/build.gradle.kts: alias google-services apply false
- android/app/build.gradle.kts: aplicacion condicional del plugin si existe google-services.json
- .github/workflows/android-release.yml: decodifica google-services.json + limpieza shred

## Pruebas ejecutadas y resultados (evidencia)
- gh secret list: 12 secrets presentes; todos los referenciados por los workflows existen (incl. GCP_SERVICE_ACCOUNT corregido).
- git grep de secrets.* en workflows coincide con los secrets existentes sin typos.
- git ls-files: sin google-services, keystore ni .jks trackeados.
- YAML android-release.yml parseado con PyYAML: OK.
- [VALIDADO LOCALMENTE con JDK 17 de Microsoft] `./gradlew :app:assembleDebug --no-daemon` (sin google-services.json): BUILD SUCCESSFUL, 38 tasks; el plugin condicional NO rompe build debug/CI sin Firebase.
- [VALIDADO LOCALMENTE] `./gradlew :app:processDebugGoogleServices` con google-services.json de prueba (Estructura publica, sin datos reales): BUILD SUCCESSFUL; el plugin se aplica y genera `build/generated/res/processDebugGoogleServices/values/values.xml` (google_app_id, project_id, gcm_defaultSenderId). Archivo de prueba ELIMINADO tras validar; git status limpio de google-services.json.

## Bloqueos
- Items 6-9 del checklist NO aprobados (backend-cd, android-release tag, instalacion realme, mediciones VM).
- Push pendiente de autorizacion: activaria workflows CI (backend-ci, android-ci).
- El resto de la validacion real (FCM, backend en produccion) pendiente de items 6-9.

## Siguiente paso
No avanzar; esperar instrucciones (push autorizado y/o items 6+).


# Item 6 - Despliegue backend-cd real (post-paso 34) EN CURSO

## Estado
dry-run del backend-cd: SUCCESS. Despliegue real iniciado; bloqueado en prerequisitos de la VM (sqlite3 no instalado). Cadena de fixes aplicados durante el item 6.

## Decisiones adoptadas y fixes (evidencia por run)
- Fix 1 - Secret typo GDP_SERVICE_ACCOUNT -> GCP_SERVICE_ACCOUNT (renombrado por usuario en GitHub); verificado gh secret list (2026-09-16T20:44).
- Fix 2 - android-release max-parallel invalido -> cancel-in-progress:false (e931b8b); run pasaba sin jobs.
- Fix 3 - gradlew sin bit ejecutivo (100644) -> git update-index --chmod=+x (100755, 37a8493); android-ci lint exit 126 -> OK.
- Fix 4 - WIF/OIDC: iamcredentials.googleapis.com deshabilitada; usuario la habilito en consola (run 35153841604 auth OK).
- Fix 5 - GCP_PROJECT_ID con numero de proyecto: gcloud scp requiere PROJECT ID textual; usuario actualizo secret (run 35154616983, error --project Project number).
- Fix 6 - roles/iap.tunnelResourceAccessor ausente: error IAP 4033 'not authorized'; usuario otorgo rol via gcloud projects add-iam-policy-binding (run 35154980863, tunel conecta OK).
- Fix 7 - Prerequisitos VM: deploy_backend.sh aborta 'ERROR: Comando requerido no encontrado: sqlite3' (run 35154980863). La VM no tiene sqlite3 instalado. Fail-fast: no se toco BD ni contenedor.
- dry-run (run 35153841604): SUCCESS - confirmacion, SHA en main, checks CI (backend-ci/android-ci/all-checks-ok), auth OIDC, setup gcloud; Desplegar skipped por dry_run=true.
- Devuelta: backend-cd verifica checks por NOMBRE (jobs requeridos) en vez de estado combinado /status (contaminado por jobs del propio workflow); commit 1c3152e.

## Archivos modificados/creados
- .github/workflows/backend-cd.yml: input dry_run (boolean, default false) + paso 'Validacion dry-run completada'; verificacion de checks por nombre.
- backend/pyproject.toml: filtros de warnings de terceros (starlette httpx UserWarning, anyio BlockingPortal DeprecationWarning) - CI limpio (166 passed, 0 warnings).

## Pruebas ejecutadas y resultados (evidencia)
- gh secret list: 12 secrets, referencias workflow == secrets (sin typos tras Fix 1).
- backend-ci: runs 35151028433/35152059421/35151734916 OK (ruff+mypy+pytest 166).
- android-ci: runs 35151028531/35152059543/35151735017 OK (lint+test+assemble).
- pytest local: 166 passed in 7.59s (0 warnings tras filtros).
- backend-cd dry-run: run 35153841604 SUCCESS completo.
- backend-cd real: run 35154146218 (project number), 35154616983 (IAP not authorized), 35154980863 (progreso hasta prerequisitos VM, sqlite3 ausente).

## Bloqueos
- VM: sqlite3 no instalado (requiere instalacion en VM o cambio de approach). Pendiente tambien validar docker/docker compose/git presentes.
- El resto de prerequisitos de deploy_backend.sh (sqlite3, docker, docker compose, curl, git) por confirmar en la VM.
- Items 7-9 del checklist pendientes de aprobacion (android-release, realme, mediciones VM).
- Push ya autorizado para CI del item 6 (commits 89cdee4, ffac7f6, 1c3152e en main).

## Siguiente paso
Instalar prerequisitos en la VM y relanzar backend-cd real (dry_run=false) con el SHA vigente.


# Item 6 - Bootstrap de primera instalacion en la VM (systemd) EN CURSO

## Estado
Backend nuevo desplegado a mano en la VM por primera vez con runtime UVICORN + SYSTEMD (No Docker): servicio renfe-notifier-backend active/running, health ok en localhost:8000. Script deploy_backend.sh adaptado a systemd (pasos 4-5) y unit creado.

## Decisiones adoptadas
- Runtime del paso 29 (docker compose) re-evaluado: no existian Dockerfile ni docker-compose.yml del backend nuevo en el repo (solo se habian versionado scripts/workflow del paso 29; la auditoria descarto el Dockerfile pesado original). Usuario eligio UVICORN + SYSTEMD (plan real seccion 3 'systemd env').
- Repo PRIVADO: es primera instalacion, no redeploy. Bootstrap manual en la VM autorizado por el usuario (clonar repo con PAT de lectura, crear .venv, .env, unit systemd, sudoers restringido).
- Conectividad: workflow conecta por OS Login como el SA sa_104384329745603569192 (no como ubuntu ni pablovb01_gmail_com); /data y el repo viven bajo el SA. PATH del unit y del script usan /home/sa_104384329745603569192.
- deploy_backend.sh reescrito: quita docker, usa systemctl restart (via sudoers NOPASSWD restringido SOLO al servicio), crea la BD vacia si no existe (primera instalacion), mantiene backup/rollback/health.

## Archivos modificados/creados
- scripts/deploy_backend.sh: reescrito a runtime systemd (primera instalacion inclusa).
- scripts/renfe-notifier-backend.service (nuevo): unit systemd de referencia (uvicorn, EnvironmentFile .env, NoNewPrivileges).
- VM (fuera de git): /home/sa_104384329745603569192/renfe-notifier-android (clone privado), backend/.venv (Python 3.14 del sistema + requirements.lock), backend/.env (produccion, SECRET_KEY generada con openssl, DB /data/renfe_notifier.db, FCM project), /etc/systemd/system/renfe-notifier-backend.service, /etc/sudoers.d/renfe-notifier-backend (440).

## Pruebas ejecutadas y resultados (evidencia)
- VM: uvicorn active (running), Main PID 16512, memoria 61.2M, health curl responde status ok (uptime 9.9s).
- Requisitos previos VM comprobados por el usuario: sqlite3 3.46.1, git 2.53.0, curl 8.18.0, docker 29.1.3 + compose v2.40.3 (docker ya no es necesario para backend; queda instalado para mediciones de arranque compose del item 9).

## Bloqueos
- El unit systemd fue creado directo en la VM (aun no esta commiteado en git ni en el repo del SA; el clone es del SHA 1c3152e).
- Falta: commit+push del script reescrito y del unit (autoria explicita; activaria CI), luego re-despachar backend-cd real con el nuevo SHA y verificar que el workflow (que copia scripts/deploy_backend.sh al SHA desplegado) ejecute el flujo systemd completo en la VM.
- El .env de la VM apunta a RENFE_NOTIFIER_FCM_PROJECT_ID (Firebase real) y a production SECRET_KEY; valida FCM de extremo a extremo pendiente del item 8.

## Siguiente paso
Commit+push autorizado del ajuste (script systemd + unit), esperar CI verde, y relanzar backend-cd real con el nuevo SHA. PENDIENTE de aprobacion del usuario para push (regla 4.2: avisar que activa CI).


## RESULTADO FINAL Item 6 (run 35158433609, SHA cf313d58): DESPLIEGUE REAL EXITOSO
- Paso 1/5 Backup: /data/backups/backup_20260916_223656.db (SHA256 78f52be9...) OK
- Paso 2/5 Checkout cf313d58ec1be23e9cd199851099e39d0d04f3cd OK
- Paso 3/5 Migraciones: aplicadas correctamente
- Paso 4/5 systemctl restart renfe-notifier-backend OK
- Paso 5/5 Health check 200 OK -> DESPLIEGUE EXITOSO
- Cadena completa validada: confirmacion DEPLOY, SHA en main, checks CI (backend-ci/android-ci/all-checks-ok en cf313d58), OIDC+WIF, IAP, script systemd en VM.
- Item 6 del checklist seccion 8 (docs/real-config-plan.md) CERRADO.


# Item 7 - android-release (tag v0.1.0) EN CURSO

## Decisiones adoptadas
- Corregido el mismo bug de auto-contaminacion del estado combinado en android-release.yml (igual fix que 1c3152e en backend-cd): el paso de verificacion de checks usa ahora la API check-runs por NOMBRE (backend-ci, android-ci, all-checks-ok) tolerando multiples coincidencias y success/skipped, en lugar de /commits/{sha}/status.
- El trigger por tag v*.*.* se disparara contra el commit de main elegido; antes de crear el tag hay que esperar CI verde del commit del fix.

## Archivos modificados
- .github/workflows/android-release.yml: verificación de checks nominal (curl check-runs). YAML validado con PyYAML.

## Pendiente
- Commit + push autorizado (activaria backend-ci/android-ci/all-checks-ok + luego android-release al crear tag).
- Crear tag v0.1.0 sobre commit con CI verde y esperar run android-release (compila release firmado + release privada).


## RESULTADO FINAL Item 7 (run 35265221397, SHA f9e9b7a): RELEASE v0.1.0 EXITOSA
- apksigner verificación OK (tras fix PATH build-tools)
- gh release create v0.1.0 → APK + checksum subidos a Release privada
- URL Release: https://github.com/Pablovb019/renfe-notifier-android/releases/tag/v0.1.0
- Item 7 del checklist seccion 8 (docs/real-config-plan.md) CERRADO.


# Item 8 - Instalar APK en realme GT Neo 2 + Emparejamiento + FCM real CERRADO

## Decisiones adoptadas
- **Crash en botones (v0.1.1/0.1.2/0.1.3)**: el mensaje "Esquema o host inseguro: se exige HTTPS salvo hosts locales de test." era IllegalStateException de ApiModule.validateBaseUrl (backend no-loopback con http), no del network_security_config. Fix: allowHttpHosts incluye la IP (db8b220). El network_security_config se ajusto a includeSubdomains=false por lint (f895220).
- **Conectividad**: el backend solo escuchaba en 127.0.0.1 dentro de la VM. Fix en unit systemd: --host 0.0.0.0 (7aa749d) + firewall GCP allow-renfe-backend (tcp:8000, sources 0.0.0.0/0, target-tag renfe-backend) + tag en la VM.
- **IP estatica**: la IP efimera 34.73.192.35 se perdio al parar/arrancar (la app al 0.1.3 apuntaba ahi). Se reservo 34.26.252.164 como estatica y se reasigno a la VM; la app 0.1.5 apunta a esa (build.gradle.kts BACKEND_URL, network_security_config, allowHttpHosts).
- **FCM 403**: dos causas encadenadas:
  1. ACCESS_TOKEN_SCOPE_INSUFFICIENT: la VM tenia scopes restringidos. Fix: set-service-account --scopes=cloud-platform (requiere VM parada) + rol roles/firebase.admin a 337831457324-compute@developer.gserviceaccount.com en renfe-notifier-bot + fcm.googleapis.com habilitada.
  2. SenderId mismatch despues: la app usaba el proyecto Firebase renfe-notifier-android, el backend enviava a renfe-notifier-bot. Fix: .env RENFE_NOTIFIER_FCM_PROJECT_ID=renfe-notifier-android + rol firebase.admin de la SA de la VM en el proyecto renfe-notifier-android + fcm.googleapis.com habilitada ahi.
- Validacion final: "Enviar Notificacion de Prueba" (Diagnostico y ajustes) -> notificacion FCM recibida correctamente en realme GT Neo 2 con la app 0.1.5. Emparejamiento OK previamente.
- Versionado alineado: desde v0.1.3 cada release tag coincide con versionName/versionCode del APK (0.1.3/code4, 0.1.4/code5, 0.1.5/code6).

## Archivos modificados
- android/app/build.gradle.kts: BACKEND_URL=http://34.26.252.164:8000/, versionCode/versionName por release.
- android/app/src/main/res/xml/network_security_config.xml: dominio 34.26.252.164 (cleartext) con includeSubdomains=false.
- android/app/src/main/java/com/pablovb019/renfenotifier/core/network/ApiModule.kt: allowHttpHosts incluye la IP de produccion.
- scripts/renfe-notifier-backend.service: plantilla con --host 0.0.0.0.
- VM (fuera de git): unit /etc/systemd/system (0.0.0.0), .env RENFE_NOTIFIER_FCM_PROJECT_ID=renfe-notifier-android, IP estatica 34.26.252.164, scopes cloud-platform de la instancia.

## Pruebas ejecutadas y resultados (evidencia)
- adb logcat: IllegalStateException "Esquema o host inseguro..." -> crasheo al pulsar botones.
- Test-NetConnection 34.73.192.35:8000 -> True tras abrir firewall y bind 0.0.0.0.
- curl salud backend 34.26.252.164:8000/api/v1/diagnostics/health -> {"status":"ok"}.
- FCM manual desde VM con token real: CODE PERMISSION_DENIED (antiguo) -> tras fixes, 400 INVALID_ARGUMENT en pruebas con token fake (auth OK) y notificacion 200 entregada en el movil.
- systemctl is-active renfe-notifier-backend -> active; journal muestra POST fcm 200/403 segun estado.
- Notificacion de prueba recibida correctamente en realme GT Neo 2 (validacion real).

## Bloqueos
- Cuota/tiempo de espera de codigo de emparejamiento: el codigo caduca a los 600s; si expira hay que regenerarlo en VM (app.cli pairing-code --generate).

## Siguiente paso
- item 9 (mediciones de recursos de la VM e2-micro) pendiente de aprobacion del usuario.


# Item 9 - Mediciones de recursos de la VM e2-micro CERRADO

## Decisiones adoptadas
- Autorizado 1 GET controlado a venta.renfe.com desde la VM para medir latencia real (regla 3: consulta unica, sin reservar ni buscar).
- `scripts/measure_resources.py` adaptado a ejecucion segura en produccion:
  - Nuevos argumentos `--db`, `--output`, `--environment`; por defecto usa BD temporal en /tmp (solo medicion, nunca /data/renfe_notifier.db).
  - Aplica `apply_migrations` a la BD temporal antes de medir (venvs sin editable install).
  - Mide el tamano solo de .db/.db-wal/.db-shm (antes sumaba todo el directorio).
  - Nueva columna GC Delta (gc.get_stats collected delta por escenario).
  - sys.path incluye `backend/` ademas del repo root (en la VM el venv no tiene install editable).
- En la VM no estaba instalado psutil en .venv (7.2.2 ya existia al reintentar); se instalo via .venv/bin/pip install psutil.
- Rutas health: el router health.py no lleva prefijo -> endpoint real GET /health (a /api/v1/health responde 404). Medido contra /health.
- No se ejecuta Selenium: la medicion confirma que HTTP DWR con agrupacion es viable en e2-micro sin Chrome (-150-300 MB RAM).

## Archivos modificados
- scripts/measure_resources.py: parametrizacion segura + GC delta + medicion solo de ficheros de la BD + sys.path backend.
- docs/measurements.json: reestructurado como `runs` (local-windows 2026-09-14 + vm-e2-micro-real 2026-09-17T22:22Z con latencia_renfe_real y health_restart).
- PROGRESS.md (raiz) y prompts/PROGRESS.md.

## Pruebas ejecutadas y resultados (evidencia)
- Local (Windows): smoke `py_compile` OK; run con BD temporal y `--environment local-windows-smoke`: 5 escenarios OK (RAM base ~43 MB, CPU 9-15%, GC 0). Sin regresiones.
- VM e2-micro (BD /tmp/renfe-measure-vm.db, FakeSearch): 5 escenarios OK. Numeros clave:
  - 1 seguimiento/1 grupo: 0.037s, CPU 9.4%, RAM 46.9->47.8 MB, 5 req, 12450 B.
  - 5 seguimientos/5 grupos: 0.039s, CPU 12.6%, 10 req, 24900 B, RAM ~47.9 MB.
  - 5 seguimientos/1 grupo: 0.040s, CPU 15.2%, 5 req, 12450 B, RAM ~48 MB.
  - Concurrencia 1 y 2: 0.040s/0.041s, CPU 9.2%/12.4%. GC delta 0 en todos, disco 0 KB.
- Latencia real Renfe (1 GET autorizado): dns 0.030s, connect 0.121s, tls 0.317s, ttfb 0.424s, total 0.424s, HTTP 200, 11083 bytes.
- Restart systemd + health: primer curl 0.026s, siguientes ~0.002s (mediana), 10x 200 OK; journal muestra GET /health 200.
- CI/CD: push 8a58448 + 0a4ca09 -> backend-ci y android-ci verdes en main.

## Bloqueos
- Ninguno. La VM usa IP estatica 34.26.252.164; el backend sigue activo tras el restart (health 200).

## Siguiente paso
- Pendiente de instrucciones del usuario (siguiente item del checklist real-config-plan.md o cierre de proyecto).


# Paso 35 - Validacion real controlada EN CURSO (escenario 1 VALIDADO; pruebas en realme en curso 2026-09-18)

## Decisiones adoptadas
- El usuario autorizo el paso 35 completo: 1 busqueda DWR real controlada (escenario 1), notificacion end-to-end + acciones (escenario 3-4) y resiliencia en el realme (escenario 5).
- Se preparo un one-shot de validacion SIN tocar git (heredoc en VM), reutilizando el motor real del backend (RenfeDwrClient + TrainSearchEngine + StationCatalog), sin crear seguimientos ni reservar billetes.
- Tras el hallazgo del protocolo DWR, el usuario autorizo corregir el flujo replicando el bot original en produccion.

## Pruebas ejecutadas y resultados (evidencia real)
- **Escenario 1 - primera consulta (FALLO parcial)**: fases search/generateId#1 OK; generateId#2 fallo con `handleBatchException: Failed to find parameter: windowName`. Diagnostico sanitizado real. Causa: payload DWR distinto al del bot original.
- **Escenario 1 - segunda consulta (FALLO parcial)**: tras corregir el handshake, `getTrainsList` devolvio `handleException U014 ("Ha pasado demasiado tiempo. Debe volver a iniciar la sesion")`. Causa: POST de busqueda enviado sin `Content-Type` de formulario, por lo que `buscarTren.do` no registraba el contexto.
- **Escenario 1 - tercera consulta (VALIDADO, commit 613bd5a)**:
  - Madrid (60000) -> Barcelona (71801) el 2026-09-19, plaza_h=False, 5 POSTs, todos HTTP 200.
  - Metricas por fase: search 702 ms/160021 B; generate_id_0 104 ms/168 B; generate_id_1 104 ms/168 B; update_session 104 ms/143 B; train_list 542 ms/73516 B. Total 2,247 s y 234016 B.
  - `ParseStatus: OK`, **14 trenes reales** con horas y precios reales (p. ej. 06:43->10:12 99,6; 09:27->13:04 87,15; 12:27->16:09 124,5).
  - Sin reservar billetes ni crear seguimientos persistentes.
- **Correccion implementada (probada localmente y validada en VM)**:
  - `app/renfe/client.py`: payloads DWR replicados (generateId con `windowName=`/`instanceId=0`/`c0-id=0` y sin `c0-param0`; update_session y getTrainsList con `c0-eN`/`Object_Object`; page `buscarTrenEnlaces.do`); POST de busqueda como formulario real (`data=dict` + Content-Type) con cookie `Search`; `scriptSessionId` con `_tokenify`; DWR_ENDPOINT con barra final; UA Chrome.
  - `app/renfe/parser.py`: soporta la estructura real anidada (`listadoTrenes` -> grupos -> `listviajeViewEnlaceBean`) y disponibilidad/`tarifaMinima`/`razonNoDisponible`; trata `"NaN"` como sin precio. Mantiene compatibilidad con la estructura plana sintetica.
  - Tests anadidos: forma exacta de los payloads y del formulario en `test_renfe_client.py`; parseo de la estructura agrupada real en `test_dwr_parser.py`.
  - Resultados locales: **168 tests pasan**, `ruff format --check` OK, `ruff check` OK, `mypy app` OK (Windows, Python 3.14.7). CI backend y android verdes.
- **Pruebas en realme GT Neo 2 (RMX3370) por USB (2026-09-18)**; app v0.1.5/code6.
  - Diagnostico en dispositivo: Google Play Services OK, permiso de notificaciones concedido, canal de avisos activo; dispositivos=1, seguimientos=0, episodios=0, consultas logicas=0, peticiones HTTP=0, bytes=0 B. La etapa "Deteccion" muestra "Revisar" (logical_queries=0), coherente con que el backend aun no ejecuta el planificador.
  - Canales confirmados por el sistema: `disponibilidad_plazas` (importance=4, sonido/vibracion) y `resumen_y_servicio` (importance=2); ambos `mBypassDnd=false`. Desactivar/reactivar el canal responde en la app.
  - **Hallazgo de arquitectura (no es fallo del paso)**: el backend no ejecuta el planificador ni entrega avisos reales (`SchedulerService` sin instanciar; `AlertQueue.enqueue/due_events` y `FcmNotificationSender.send_alert` sin llamadores; `create_followup` no encola). Coherente con `prompts/34` ("no actives sondeo del nuevo backend") y pendiente para la transicion (paso 37). Por eso el escenario "alerta real con acciones" aun no es validable.
  - **Defecto real detectado y corregido**: `send_test` reutilizaba `event_id="test"` fijo y la app deduplica por `event_id` durante 30 dias (`FcmTokenGateway.kt:36`, `EventIdStore.kt:56-57`), descartando silenciosamente las pruebas repetidas. Evidencia con adb y DND desactivado (`zen_mode=0`): logcat muestra recepcion FCM `FirebaseInstanceIdReceiver ... act=com.google.android.c2dm.intent.RECEIVE pkg=com.pablovb019.renfenotifier` y `dumpsys notification` no muestra ninguna notificacion de la app. Fix en `backend/app/notifications/fcm.py` (`event_id=f"test-{uuid4().hex}"`) + test `test_send_test_uses_unique_event_id_per_send`. Verificacion local: ruff format/check OK, mypy OK, 169 tests OK.
  - **Fix desplegado y validado**: la VM estaba en HEAD desacoplado; se paso a `main` (d95733f) y `systemctl restart`. Notificacion de prueba enviada 2 veces -> **llegaron 2** (antes solo la primera).
  - **Defecto de audio detectado**: el canal `disponibilidad_plazas` usa `AudioAttributes.USAGE_ALARM` (`NotificationChannels.kt`), por lo que suena por el stream de alarmas e **ignora** el modo vibracion/silencio/volumen de notificaciones (confirmado por el sistema: `mAudioAttributes usage=USAGE_ALARM`, `mUserLockedFields=4`).
  - **v0.1.6 (code7) - borrar/recrear NO funciona**: con el mismo id Android conserva los ajustes bloqueados por el usuario al recrear el canal (lo re-creo con `usage=USAGE_ALARM` aun con el codigo ya corregido). La **actualizacion con la misma firma** si se valido (firstInstall sin cambios, lastUpdate 2026-09-18 01:31, v0.1.6/code7).
  - **v0.1.7 (code8) - fix definitivo (implementado y validado el canal)**: canal de avisos con **id versionado** `disponibilidad_plazas_v2` (`NotificationChannels.kt`, `AlertPayload.kt`), migracion que borra el canal antiguo, y backend + docs + tests coherentes (`fcm.py`, `notificaciones-fcm.md`, `architecture.md`, `AlertPayloadParserTest.kt`). La app ya mapea cualquier `channel_id` no-servicio a su canal (`NotificationDisplayer.kt:115-120`), asi que el envio de prueba funciona con el id nuevo.
  - **Validado en realme (v0.1.7/code8, instalado encima de v0.1.6; firstInstall intacto)**: `disponibilidad_plazas_v2` con `usage=USAGE_NOTIFICATION` y `mUserLockedFields=0` (importancia 4); el canal antiguo `disponibilidad_plazas` queda `mDeleted=true`; `resumen_y_servicio` intacto. La actualizacion con la misma firma se valido dos veces (v0.1.6 y v0.1.7).
  - **Entrega end-to-end por el canal v2 (16:49, realme desbloqueado)**: se pulso "Enviar notificacion de prueba" en Diagnostico; FCM recibido (`FirebaseInstanceIdReceiver ... c2dm.intent.RECEIVE`), vibracion del sistema y `NotificationRecord` activo con `channel=disponibilidad_plazas_v2` (importance=4).
  - **Pendiente de verificacion funcional (accion del usuario)**: confirmacion auditiva de que respeta el modo vibracion/silencio (movil en silencio/vibracion y pulsar "Enviar notificacion de prueba"). No se pide ni se lee el PIN (secreto). **YA CONFIRMADO por el usuario** (vibra/silencioso/sonoro OK).

## Bloqueos
- Ninguno tecnico para el escenario 1 (cerrado).
- "Alerta real con acciones" NO validable aun: la entrega de avisos reales no esta cableada (ver hallazgo) y el paso 34 prohibe activar el sondeo; corresponde a la transicion (paso 37).
- El realme quedo bloqueado con PIN durante la sesion y no se desbloquea via adb (keyguard seguro); los pasos que necesitan UI requieren desbloqueo manual del usuario (ya desbloqueado). Pruebas pendientes: entrega activo/desbloqueado, entrega bloqueado/pantalla apagada (no debe ser limitante), Doze, reinicio, Wi-Fi/datos y **sin conexion -> recuperacion al volver** (FCM alta prioridad, TTL 300 s). El envio para estas pruebas usa el nuevo `renfe-notifier-cli test-notification` (en la VM), no el boton de la app.

## Siguiente paso
- Commit+push del subcomando `test-notification` (aviso: dispara CI) y `git pull` en la VM (no requiere restart: el CLI corre desde el repo).
- Ejecutar el plan de entrega en el realme: activo+desbloqueado -> bloqueado/pantalla apagada -> Doze -> reinicio -> sin conexion/recuperacion.
- Terreno sin conexion: apagar Wi-Fi/datos en el realme, enviar desde la VM, reactivar y comprobar que llega dentro del TTL de 300 s (o marca caducidad si pasó).
- Actualizar PROGRESS.md y detenerse (no avanzar al paso 36).
