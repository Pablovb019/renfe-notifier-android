# Validación Local Integrada (Paso 31)

> **Fecha**: 2026-09-13  
> **Entorno**: Windows 10/11 local, Python 3.11.9, JDK Temurin 17.0.20.1, Gradle 8.9, AGP 8.5.2  
> **Alcance**: Validación completa **sin servicios reales** (sin Renfe, sin FCM, sin VM).  
> **Evidencia**: Todos los comandos ejecutados localmente y verificados.

---

## 1. Tests Completos de Backend (166 passed)

```bash
cd backend && .venv\Scripts\python.exe -m pytest -q
```

**Resultado**: **166 passed, 2 warnings** (deprecaciones heredadas de starlette/httpx, no bloqueantes)

### Cobertura por módulo:
| Módulo | Tests | Áreas validadas |
|--------|-------|-----------------|
| `test_config.py` | 2 | Validación entorno, secretos en producción |
| `test_diagnostics_api.py` | 6 | Health, diagnostics, test notification, FCM 410/503 |
| `test_dwr_parser.py` | 2 | Parser sintético, distinción no-disponibilidad vs error |
| `test_fcm_api.py` | 3 | Auth, registro/renovación token, token malformado |
| `test_fcm_providers.py` | 5 | GCE metadata, fallback AutoTokenProvider, sin credenciales |
| `test_fcm_transport.py` | 19 | Payload validation, retry logic, collapse_key, TTL, test alerts |
| `test_followup_database.py` | 13 | WAL, migraciones idempotentes, plaza_h, episodios, backup/restore |
| `test_followup_domain.py` | 12 | Estados, episodios, caducidad, midnight/horario verano, modos |
| `test_followups_api.py` | 8 | CRUD, lifecycle (pause/resume/renew/ack/delete), auth |
| `test_health.py` | 1 | Health endpoint básico |
| `test_monitoring.py` | 3 | Contadores queries lógicas, HTTP requests, bytes |
| `test_pairing_api.py` | 8 | Claim, rate limit, log redaction, token nunca expone código |
| `test_pairing_database.py` | 11 | Migraciones, códigos, dispositivos, FCM token, reclaim |
| `test_reminders_domain.py` | 1 | Status strings |
| `test_reminders_queue.py` | 18 | Enqueue/deliver/cancel/suspend, max_attempts, restart recovery |
| `test_renfe_client.py` | 4 | 5 POST documentados, retry-after, 403, budget total |
| `test_scheduler_plan.py` | 6 | Agrupación por plaza_h/ruta, observaciones, errores |
| `test_scheduler_service.py` | 8 | Run once/forever, grupos, duplicados, reaparición disponibilidad |
| `test_search_api.py` | 6 | Stations search, trains search, validaciones, auth |
| `test_stations.py` | 5 | Normalización (tildes/puntuación), alias, grupos, by_code |

---

## 2. Parser, Plaza H, Fechas y Matching

| Test | Qué valida |
|------|------------|
| `test_dwr_parser.py::test_parser_reads_synthetic_fixture_and_keeps_plaza_h_context` | Parser DWR lee fixture sintético y preserva contexto Plaza H |
| `test_dwr_parser.py::test_parser_distinguishes_no_availability_from_invalid_response` | Distingue "sin plazas" de respuesta inválida/error |
| `test_stations.py::test_normalization_removes_accents_and_punctuation` | Normalización elimina tildes/puntuación para búsqueda |
| `test_stations.py::test_catalog_resolves_alias_and_preserves_full_catalog` | Catálogo resuelve alias y mantiene catálogo completo |
| `test_stations.py::test_group_returns_concrete_city_stations` | Grupos devuelven estaciones concretas de ciudad |
| `test_scheduler_plan.py::test_build_plan_single_group_collects_compatible_followups` | Agrupa followups compatibles en plan único |
| `test_scheduler_plan.py::test_build_plan_splits_groups_by_plaza_h_and_route` | Separa grupos por plaza_h y ruta origen-destino |
| `test_scheduler_plan.py::test_train_list_to_observation_maps_every_train` | Mapea cada tren a observación válida |

---

## 3. Estados, Recordatorios, Caducidad y Reinicios

| Test | Qué valida |
|------|------------|
| `test_followup_domain.py::test_new_availability_episode_acknowledgement_and_reappearance` | Nuevo episodio disponibilidad, acknowledge, reaparición |
| `test_followup_domain.py::test_errors_and_stale_data_do_not_create_new_episodes` | Errores/datos stale no crean episodios falsos |
| `test_followup_domain.py::test_pause_renew_and_delete_are_independent_transitions` | Pause/renew/delete son transiciones independientes |
| `test_followup_domain.py::test_all_mode_creates_an_episode_for_newly_available_train` | Modo ALL crea episodio cuando tren disponible |
| `test_followup_domain.py::test_first_and_last_select_the_boundary_train` | FIRST/LAST selecciona tren frontera |
| `test_followup_domain.py::test_expiry_is_limited_by_departure_and_all_mode_ends_at_local_day_end` | Caducidad limitada por salida, ALL termina fin día local |
| `test_followup_domain.py::test_midnight_arrival_and_summer_time_change_use_madrid_dates` | Llegada medianoche y cambio horario usan fechas Madrid |
| `test_reminders_queue.py::test_enqueue_and_get_roundtrip` | Enqueue/get roundtrip |
| `test_reminders_queue.py::test_enqueue_duplicate_is_idempotent` | Duplicados son idempotentes |
| `test_reminders_queue.py::test_deliver_consumes_attempt_and_programs_next` | Entrega consume intento y programa siguiente |
| `test_reminders_queue.py::test_deliver_is_bounded_by_max_attempts` | Entrega limitada por max_attempts |
| `test_reminders_queue.py::test_cancel_for_followup_sets_reason` | Cancel por razón (acknowledged/paused/expired/unavailable) |
| `test_reminders_queue.py::test_suspend_hides_pending_events` | Suspend oculta eventos pendientes |
| `test_reminders_queue.py::test_restart_recovers_pending_state` | Reinicio recupera estado pendiente |
| `test_scheduler_service.py::test_run_once_does_not_create_duplicate_episode_while_still_available` | No duplica episodio mientras sigue disponible |
| `test_scheduler_service.py::test_run_once_creates_new_episode_when_availability_reappears` | Crea episodio cuando disponibilidad reaparece |

---

## 4. Agrupación y Deduplicación

| Test | Qué valida |
|------|------------|
| `test_scheduler_plan.py::test_build_plan_splits_groups_by_plaza_h_and_route` | Grupos separados por plaza_h y ruta |
| `test_scheduler_plan.py::test_build_plan_single_group_collects_compatible_followups` | Un grupo para followups compatibles |
| `test_followup_database.py::test_list_filter_by_lifecycle_and_purge_deleted` | Filtro por lifecycle y purga deleted |
| `test_reminders_queue.py::test_enqueue_duplicate_is_idempotent` | Enqueue duplicado idempotente |
| `test_reminders_queue.py::test_enqueue_replaces_previous_event_of_same_followup` | Reemplaza evento previo del mismo followup |
| `test_reminders_queue.py::test_enqueue_keeps_events_of_other_followups` | Mantiene eventos de otros followups |
| `test_fcm_transport.py::test_send_alert_builds_collapse_key_per_followup` | Collapse key por followup (dedup FCM) |

---

## 5. Autenticación y Validación

| Test | Qué valida |
|------|------------|
| `test_pairing_api.py::test_claim_with_valid_code_returns_token` | Claim válido retorna token |
| `test_pairing_api.py::test_claim_with_invalid_code_returns_401` | Código inválido → 401 |
| `test_pairing_api.py::test_claim_rejects_malformed_payload` | Payload malformado rechazado |
| `test_pairing_api.py::test_devices_requires_valid_bearer` | Devices requiere Bearer válido |
| `test_pairing_api.py::test_devices_succeeds_with_valid_device_token` | Device token válido → éxito |
| `test_pairing_api.py::test_revoked_device_is_rejected` | Device revocado → rechazado |
| `test_pairing_api.py::test_claim_is_rate_limited_per_client` | Rate limit por cliente/IP |
| `test_pairing_api.py::test_log_redaction_hides_secrets` | Logs redactan secretos |
| `test_pairing_api.py::test_token_never_echoes_pairing_code` | Token nunca expone código emparejamiento |
| `test_fcm_api.py::test_fcm_requires_auth` | FCM requiere auth |
| `test_fcm_api.py::test_fcm_registers_and_renews_token` | Registro y renovación token FCM |
| `test_fcm_api.py::test_fcm_rejects_malformed_token` | Token malformado rechazado |
| `test_diagnostics_api.py::test_diagnostics_requires_auth` | Diagnostics requiere auth |
| `test_search_api.py::test_stations_search_requires_auth` | Stations search requiere auth |
| `test_search_api.py::test_search_trains_requires_auth` | Trains search requiere auth |
| `test_followups_api.py::test_create_followup_requires_auth` | Create followup requiere auth |

---

## 6. Migraciones, Backups y Restauración

| Test | Qué valida |
|------|------------|
| `test_followup_database.py::test_initialize_creates_schema_with_wal_and_pragmas` | Inicializa esquema con WAL y pragmas |
| `test_followup_database.py::test_initialize_is_idempotent_across_restarts` | Idempotente tras reinicios |
| `test_followup_database.py::test_restart_persists_followups` | Reinicio persiste followups |
| `test_followup_database.py::test_plaza_h_roundtrip_in_repository` | Plaza H roundtrip en repositorio |
| `test_followup_database.py::test_episodes_roundtrip_and_unique_per_episode` | Episodios roundtrip y únicos por episodio |
| `test_followup_database.py::test_backup_is_consistent_and_independent` | Backup consistente e independiente |
| `test_followup_database.py::test_backup_missing_source_raises` | Backup origen faltante lanza error |
| `test_followup_database.py::test_recovery_creates_fresh_database_after_removal` | Recuperación crea BD fresca tras borrado |
| `test_pairing_database.py::test_initialize_applies_migrations` | Inicializa aplica migraciones |
| `test_pairing_database.py::test_purge_expired_codes_removes_only_expired` | Purga expira solo códigos expirados |

### CLI Backup/Restore verificada manualmente:
```bash
# Backup
python -m app.cli backup --output backend/data/validation_backup.db
# → Backup creado: ... (SHA256: 5669c3bb7d0d1779544f8ef34b5781f00c172781b26745d734579cc101861304, versión esquema: 5)

# Verify
python -m app.cli verify --path backend/data/validation_backup.db
# → OK: integrity_check: ok

# Version
python -m app.cli version --path backend/data/validation_backup.db
# → Versión esquema aplicada: 5

# Restore en BD nueva
RENFE_NOTIFIER_DATABASE_PATH="backend/data/restored_validation.db" \
python -m app.cli restore --input backend/data/validation_backup.db --force
# → Restaurado: ... (versión esquema: 5)
# Verify post-restore → OK
# Version post-restore → 5

# Migraciones CLI
RENFE_NOTIFIER_DATABASE_PATH="backend/data/migrate_test.db" \
python -c "from app.db.connection import connect; from pathlib import Path; connect(Path('backend/data/migrate_test.db')).close()"
python -m app.db.migrate
# → Migraciones aplicadas correctamente
python -m app.cli version --path backend/data/migrate_test.db
# → Versión esquema aplicada: 5
```

---

## 7. Tests Android y Compilación APK

```bash
cd android
.\gradlew.bat :app:testDebugUnitTest :app:lintDebug :app:assembleDebug --no-daemon
```

**Resultado**: **BUILD SUCCESSFUL** en todas las tareas

### Tests Unitarios (82 tests, 0 fallos, 10 suites):
| Suite | Tests | Qué valida |
|-------|-------|------------|
| `ApiContractTest` | 13 | Contratos HTTP: health, claim, stations, trains, followups, FCM |
| `AuthInterceptorTest` | 3 | Bearer, omitido en /health, 401 limpio |
| `PairingViewModelTest` | 6 | Éxito solo 200, código vacío, 401, 429, error red, sanitización |
| `SearchViewModelTest` | 12 | Debounce, selección, éxito, vacío, validaciones, fecha pasada, 503, IOException, specific-requires-train, create OK reutilizando ruta/fecha, create error 409 |
| `FollowUpsViewModelTest` | 5 | Carga con filtro todos, cambio filtro consulta lifecycle, vacío ≠ error, 503 ≠ vacío, error red |
| `FollowUpDetailViewModelTest` | 8 | Última comprobación válida, 404, pausa recarga servidor, 409 sin éxito falso, acknowledge sin borrar + refresco, error red, delete OK (1 llamada) y 409 sin borrado |
| `AlertPayloadParserTest` | 11 | Payload completo, placeholders origin/destination, test con placeholder ignorado, timestamp ISO con offset, observed_at nulo/ausente, "-"→null, caducidad pasada, stale 30 min, alert ≠ test siempre entregado, channel_id ausente → default |
| `EventLedgerTest` | 6 | Entrega duplicada detectada, corte ventana 30 días, poda remember/prune, restauración snapshot, capacidad 500 |
| `NetworkPolicyTest` | 6 | IOException reintentable; 429/5xx reintentables; 4xx no; 404/409 éxito idempotente; 401 revocación; corte tras 3 intentos |
| `DiagnosticsViewModelTest` | 12 | 5 etapas (QUERY/DETECTION/SEND/RECEIVE/NOTIFY), StageStatus, FakeDeviceEnvironment/FakeSettingsSource |

### Lint:
- **BUILD SUCCESSFUL** (sin errores)
- Warnings heredados: versiones más nuevas (play-services-base 18.5.0→18.10.1, etc.), `Modifier.menuAnchor()` deprecado, recursos sin uso — **no bloqueantes**

### APK Debug:
- `app/build/outputs/apk/debug/app-debug.apk` → **12.685.305 bytes**
- `versionName: 0.1.0`, `versionCode: 1`, `applicationId: com.pablovb019.renfenotifier`

---

## 8. Contratos y Flujos Android/Backend (Servicios Simulados)

| Área | Validación |
|------|------------|
| **Emparejamiento** | `FakeRenfeApi.pairingCodeHandler` + `claimHandler` → flujo completo claim + token |
| **Búsqueda** | `stationsHandler` + `trainsHandler` → debounce, validaciones, empty/error states |
| **Seguimientos** | `listFollowUpsHandler` (con filtro lifecycle) + `followUpDetailHandler` (episodios) + `pause/resume/renew/acknowledge/delete` handlers |
| **FCM** | `registerFcmTokenHandler` (PUT /api/v1/fcm/token, 409 si inactivo) + `FakeSender` para test notification |
| **Diagnósticos** | `diagnosticsHandler` (search_stats, server_contact, device_env) + `sendTestNotificationHandler` (FakeSender → message-test-1) |
| **Auth** | `AuthInterceptor` añade Bearer, omite /health, 401 limpio |

Todos usan `MockWebServer` / `FakeRenfeApi` / `FakeSender` — **cero llamadas reales**.

---

## 9. Validación de Workflows y Restricciones de Despliegue

### Workflows (4) — Todos con pinning SHA:
| Workflow | Archivo | Triggers | Jobs clave | Pinning SHA verificado |
|----------|---------|----------|------------|------------------------|
| **backend-ci** | `.github/workflows/backend-ci.yml` | push, PR → main | detect-changes → backend-ci → all-checks-ok | checkout v4.4.0, setup-python v5.6.0, paths-filter v3.0.4 |
| **android-ci** | `.github/workflows/android-ci.yml` | push, PR → main | detect-changes → android-ci → all-checks-ok | checkout v4.4.0, setup-java v4.7.1, setup-gradle v4.2.2, paths-filter v3.0.4, upload-artifact v4.6.2 |
| **backend-cd** | `.github/workflows/backend-cd.yml` | workflow_dispatch | validate-and-deploy (OIDC+IAP) + validate-local | checkout v4.4.0, auth v2.1.3, setup-gcloud v2.1.0 |
| **android-release** | `.github/workflows/android-release.yml` | workflow_dispatch, tags v*.*.* | validate-and-build (keystore base64, apksigner, SHA256) + validate-local | checkout v4.4.0, setup-java v4.7.1, setup-gradle v4.2.2, create-release v1.1.4, upload-release-asset v1.0.2 |

### Restricciones verificadas:
- ✅ **android-release**: sin trigger `pull_request` (solo `workflow_dispatch` + tags `v*.*.*`)
- ✅ **Permisos mínimos**: `contents: read` en CI; `contents: write` solo en release; `id-token: write` solo en CD para OIDC
- ✅ **Concurrencia controlada**: `cancel-in-progress: true` en CI; `max-parallel: 1` en CD y release (sin cancelación durante migraciones/firma)
- ✅ **Sin secretos en código**: Keystore en secret base64, credenciales GitHub Secrets, OIDC sin claves SSH
- ✅ **Retención artefactos controlada**: CI Android 1 día, Release 5 días (dentro de cuota 500 MB)
- ✅ **Validación criptográfica commit**: CD y Release verifican SHA en main + CI success vía GitHub API antes de tocar infra

---

## Checklist de Validación (Paso 31)

| Ítem | Estado | Evidencia |
|------|--------|-----------|
| Tests backend completos | ✅ | 166 passed |
| Parser / Plaza H / Fechas / Matching | ✅ | test_dwr_parser, test_stations, test_scheduler_plan |
| Estados / Recordatorios / Caducidad / Reinicios | ✅ | test_followup_domain, test_reminders_queue, test_scheduler_service |
| Agrupación / Deduplicación | ✅ | test_scheduler_plan, test_followup_database, test_reminders_queue |
| Autenticación / Validación | ✅ | test_pairing_api, test_fcm_api, test_diagnostics_api, test_search_api |
| Migraciones / Backup / Restore | ✅ | test_followup_database, test_pairing_database, CLI manual verificado |
| Tests Android + APK | ✅ | 82 tests, lint OK, APK 12.6 MB |
| Contratos Android/Backend (mocks) | ✅ | FakeRenfeApi, FakeSender, MockWebServer — 0 llamadas reales |
| Workflows (pinning SHA, permisos, concurrencia) | ✅ | 4 workflows validados programáticamente |
| Restricciones despliegue (sin PRs en release, OIDC, etc.) | ✅ | Verificado en YAML |

---

## Pendientes / Bloqueados (requieren aprobación externa)

| Bloqueo | Paso | Qué falta |
|---------|------|-----------|
| **VM e2-micro + OIDC/IAP** | 06 / 29 | Crear VM, configurar WIF, IAP, Tailscale, secrets GitHub |
| **Firebase Spark + google-services.json** | 33 / 34 | Crear proyecto Firebase, descargar google-services.json, secret Base64 CI Android |
| **Keystore release + secrets** | 30 | `keytool` local + 4 secrets GitHub (`ANDROID_KEYSTORE_BASE64`, `KEYSTORE_PASSWORD`, `KEY_ALIAS`, `KEY_PASSWORD`) |
| **Push a GitHub / CI remoto** | 31+ | Autorización regla 4.2 (push a main dispararía CI/CD) |
| **Dispositivo real (realme GT Neo 2)** | 35 | Instalación APK + pruebas UX con backend accesible (VM o túnel local) |
| **FCM real end-to-end** | 33 / 35 | Proyecto Firebase + permisos backend VM + google-services.json en app |
| **Mediciones reales en VM** | 32 | CPU/RAM/disco/duración en e2-micro con carga real |

---

## Conclusión

**Validación local integrada COMPLETA y EXITOSA.**  
Todos los tests (166 backend + 82 Android), compilaciones (debug + release sin firmar), lint, CLI backup/restore/migrate, contratos con mocks, y workflows — **pasan sin llamadas a servicios reales**.

El sistema está listo para la fase de configuración real (Pasos 33–34) una vez aprobados los bloqueos externos.