# Renfe Notifier Android

Aplicación Android nativa + backend ligero (Python/FastAPI) de **uso personal y gratuito** que monitoriza plazas en Renfe y avisa por notificación FCM cuando aparecen. El bot original `Pablovb019/renfe-notifier-bot` **no se toca**; este repositorio es un desarrollo nuevo que convivirá en transición.

- **Repositorio**: privado, nuevo. Código Android (Jetpack Compose/Kotlin), backend Python, GitHub Actions.
- **Presupuesto**: 0,00 ââ€šÂ¬ (Free Tier GCP + Firebase Spark + GitHub Actions). Ver `docs/costs.md`.
- **Dispositivo objetivo**: realme GT Neo 2 (Realme UI 4, Android 13).

## Estado

- **ÃƒÅ¡ltima release**: `v0.1.10` (código 11) ââ‚¬â€ APP firmada + checksum. Instalada y validada en el realme (autocompletado validado por el usuario en el flujo real).
  - `app-release.apk` (8.995.411 B) ââ‚¬â€ SHA256 `37a59734efc5567f3362b671d87aebf037c2374c4a68fa4649bbcb718bdf4c76`
  - Evidencia verificada vía API de releases de GitHub (2026-09-20).
- **Backend en producción (VM e2-micro)**: desplegado con fix DWR (`899f740`), validado 100/100 contra Renfe real, CI verde. Ranking 197 tests OK, ruff/mypy OK.
- Bloques Aââ‚¬â€œD completos. Los residuos del bot anterior NO se han eliminado (el bot anterior se conserva).

> No se declaran completos aquí requisitos que siguen pendientes de verificación; consulta `docs/requirements-checklist.md` y `PROGRESS.md`.

## Índice de documentación

### Arquitectura y decisiones
- `docs/architecture.md` ââ‚¬â€ arquitectura del sistema (Android + backend + SQLite).
- `docs/security.md` ââ‚¬â€ seguridad, autenticación backendââ€ â€app, FCM, anti-SSRF.
- `docs/notificaciones-fcm.md` ââ‚¬â€ diseño de notificaciones FCM (canales, payload, Doze/realme).
- `docs/notificaciones-fcm.md` ââ‚¬â€ canales de notificación definidos.

### Construcción, instalación y CI
- `docs/android-build.md` ââ‚¬â€ compilación local del APK en **Windows** (JDK 17 + Gradle).
- `docs/instalacion-apk.md` ââ‚¬â€ instalación/actualización manual del APK en el realme.
- `docs/keystore.md` ââ‚¬â€ **clave de firma**: generación, backup, verificación y rotación.
- `docs/ci-cd.md` ââ‚¬â€ CI/CD: workflows Android y backend en GitHub Actions.
- `docs/real-config-plan.md` ââ‚¬â€ plan de configuración real (Firebase, VM, TLS, secrets).

### Operación en VM (GCP e2-micro)
- `docs/costs.md` ââ‚¬â€ costes, cuotas y controles (0,00 ââ€šÂ¬, Free Tier).
- `docs/recovery.md` ââ‚¬â€ backup y recuperación de la base de datos SQLite.
- `docs/transicion.md` ââ‚¬â€ plan de transición desde el bot original.
- `docs/security.md` ââ‚¬â€ acuerdos de acceso admin a la VM.

### Validación y mantenimiento
- `docs/validation.md` ââ‚¬â€ resultado de pruebas (190 tests backend OK, etc.).
- `docs/renfe-dwr-diagnosis/` ââ‚¬â€ diagnóstico del flujo DWR con Renfe (fix token `generateId`).
- `docs/real-config-plan.md` ââ‚¬â€ despliegue real y validación en realme (pasos 32ââ‚¬â€œ36).
- `docs/requirements-checklist.md` ââ‚¬â€ checklist de requisitos comprobables.
- `docs/audit.md` ââ‚¬â€ auditoría del repositorio original.
- `docs/transicion.md` ââ‚¬â€ sustitución del bot anterior.

### Detección de errores y diagnóstico
- `docs/diagnostico-realme.md` ââ‚¬â€ guía de diagnóstico para realme GT Neo 2.
- `docs/audit.md` ââ‚¬â€ auditoría del código original.

## Uso rápido (resumen)

1. **Compilar el APK** (Windows): ver `docs/android-build.md`.
2. **Instalar/actualizar** en el realme: ver `docs/instalacion-apk.md`.
3. **Configurar el backend** en la VM: ver `docs/real-config-plan.md`.
4. **Backup/restore** de la BD: ver `docs/recovery.md`.
5. **Coste**: 0,00 ââ€šÂ¬/mes con la VM e2-micro en Free Tier (`docs/costs.md`).

## Licencias y atribuciones
- El repositorio original (`renfe-notifier-bot`) aporta conocimiento del protocolo DWR de Renfe; este desarrolla el flujo desde cero. Ver `docs/licencias.md`.
- Código reutilizado: atribuciones y condiciones en `docs/licencias.md`.

## Cierre y mantenimiento
El estado del proyecto, pasos ejecutados y pendientes se registran en `PROGRESS.md`. Emplea el registro siguiente para continuar.
