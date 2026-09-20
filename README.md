# Renfe Notifier Android

AplicaciÃƒÂ³n Android nativa + backend ligero (Python/FastAPI) de **uso personal y gratuito** que monitoriza plazas en Renfe y avisa por notificaciÃƒÂ³n FCM cuando aparecen. El bot original `Pablovb019/renfe-notifier-bot` **no se toca**; este repositorio es un desarrollo nuevo que convivirÃƒÂ¡ en transiciÃƒÂ³n.

- **Repositorio**: privado, nuevo. CÃƒÂ³digo Android (Jetpack Compose/Kotlin), backend Python, GitHub Actions.
- **Presupuesto**: 0,00 Ã¢â€šÂ¬ (Free Tier GCP + Firebase Spark + GitHub Actions). Ver `docs/costs.md`.
- **Dispositivo objetivo**: realme GT Neo 2 (Realme UI 4, Android 13).

## Estado

- **ÃƒÅ¡ltima release**: `v0.1.10` (cÃƒÂ³digo 11) Ã¢â‚¬â€ APP firmada + checksum. Instalada y validada en el realme (autocompletado validado por el usuario en el flujo real).
  - `app-release.apk` (8.995.411 B) Ã¢â‚¬â€ SHA256 `37a59734efc5567f3362b671d87aebf037c2374c4a68fa4649bbcb718bdf4c76`
  - Evidencia verificada vÃƒÂ­a API de releases de GitHub (2026-09-20).
- **Backend en producciÃƒÂ³n (VM e2-micro)**: desplegado con fix DWR (`899f740`), validado 100/100 contra Renfe real, CI verde. Ranking 197 tests OK, ruff/mypy OK.
- Bloques AÃ¢â‚¬â€œD completos. Los residuos del bot anterior NO se han eliminado (el bot anterior se conserva).

> No se declaran completos aquÃƒÂ­ requisitos que siguen pendientes de verificaciÃƒÂ³n; consulta `docs/requirements-checklist.md` y `PROGRESS.md`.

## ÃƒÂndice de documentaciÃƒÂ³n

### Arquitectura y decisiones
- `docs/architecture.md` Ã¢â‚¬â€ arquitectura del sistema (Android + backend + SQLite).
- `docs/security.md` Ã¢â‚¬â€ seguridad, autenticaciÃƒÂ³n backendÃ¢â€ â€app, FCM, anti-SSRF.
- `docs/notificaciones-fcm.md` Ã¢â‚¬â€ diseÃƒÂ±o de notificaciones FCM (canales, payload, Doze/realme).
- `docs/notificaciones-fcm.md` Ã¢â‚¬â€ canales de notificaciÃƒÂ³n definidos.

### ConstrucciÃƒÂ³n, instalaciÃƒÂ³n y CI
- `docs/android-build.md` Ã¢â‚¬â€ compilaciÃƒÂ³n local del APK en **Windows** (JDK 17 + Gradle).
- `docs/instalacion-apk.md` Ã¢â‚¬â€ instalaciÃƒÂ³n/actualizaciÃƒÂ³n manual del APK en el realme.
- `docs/keystore.md` Ã¢â‚¬â€ **clave de firma**: generaciÃƒÂ³n, backup, verificaciÃƒÂ³n y rotaciÃƒÂ³n.
- `docs/ci-cd.md` Ã¢â‚¬â€ CI/CD: workflows Android y backend en GitHub Actions.
- `docs/real-config-plan.md` Ã¢â‚¬â€ plan de configuraciÃƒÂ³n real (Firebase, VM, TLS, secrets).

### OperaciÃƒÂ³n en VM (GCP e2-micro)
- `docs/costs.md` Ã¢â‚¬â€ costes, cuotas y controles (0,00 Ã¢â€šÂ¬, Free Tier).
- `docs/recovery.md` Ã¢â‚¬â€ backup y recuperaciÃƒÂ³n de la base de datos SQLite.
- `docs/transicion.md` Ã¢â‚¬â€ plan de transiciÃƒÂ³n desde el bot original.
- `docs/security.md` Ã¢â‚¬â€ acuerdos de acceso admin a la VM.

### ValidaciÃƒÂ³n y mantenimiento
- `docs/validation.md` Ã¢â‚¬â€ resultado de pruebas (190 tests backend OK, etc.).
- `docs/renfe-dwr-diagnosis/` Ã¢â‚¬â€ diagnÃƒÂ³stico del flujo DWR con Renfe (fix token `generateId`).
- `docs/real-config-plan.md` Ã¢â‚¬â€ despliegue real y validaciÃƒÂ³n en realme (pasos 32Ã¢â‚¬â€œ36).
- `docs/requirements-checklist.md` Ã¢â‚¬â€ checklist de requisitos comprobables.
- `docs/audit.md` Ã¢â‚¬â€ auditorÃƒÂ­a del repositorio original.
- `docs/transicion.md` Ã¢â‚¬â€ sustituciÃƒÂ³n del bot anterior.

### DetecciÃƒÂ³n de errores y diagnÃƒÂ³stico
- `docs/diagnostico-realme.md` Ã¢â‚¬â€ guÃƒÂ­a de diagnÃƒÂ³stico para realme GT Neo 2.
- `docs/audit.md` Ã¢â‚¬â€ auditorÃƒÂ­a del cÃƒÂ³digo original.

## Uso rÃƒÂ¡pido (resumen)

1. **Compilar el APK** (Windows): ver `docs/android-build.md`.
2. **Instalar/actualizar** en el realme: ver `docs/instalacion-apk.md`.
3. **Configurar el backend** en la VM: ver `docs/real-config-plan.md`.
4. **Backup/restore** de la BD: ver `docs/recovery.md`.
5. **Coste**: 0,00 Ã¢â€šÂ¬/mes con la VM e2-micro en Free Tier (`docs/costs.md`).

## Licencias y atribuciones
- El repositorio original (`renfe-notifier-bot`) aporta conocimiento del protocolo DWR de Renfe; este desarrolla el flujo desde cero. Ver `docs/licencias.md`.
- CÃƒÂ³digo reutilizado: atribuciones y condiciones en `docs/licencias.md`.

## Cierre y mantenimiento
El estado del proyecto, pasos ejecutados y pendientes se registran en `PROGRESS.md`. Emplea el registro siguiente para continuar.
