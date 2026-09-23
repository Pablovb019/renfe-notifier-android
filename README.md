# Renfe Notifier Android

Aplicación Android nativa + backend ligero (Python/FastAPI) de **uso personal y gratuito** que monitoriza plazas en Renfe y avisa por notificación FCM cuando aparecen. El bot original `Pablovb019/renfe-notifier-bot` **no se toca**; este repositorio es un desarrollo nuevo que convivirá en transición.

- **Repositorio**: privado, nuevo. Código Android (Jetpack Compose/Kotlin), backend Python, GitHub Actions.
- **Presupuesto**: 0,00 € (Free Tier GCP + Firebase Spark + GitHub Actions). Ver `docs/costs.md`.
- **Dispositivo objetivo**: realme GT Neo 2 (Realme UI 4, Android 13).

## Estado

- **En curso**: `v0.2.3` (código 15) — icono launcher xxhdpi retocado manualmente + badge "Disponible" en verde success (contraste con "Sin Plazas"); **badge validado en el realme** (pixel `#00522E`).
- **Última release publicada**: `v0.2.3` (código 15) — badge "Disponible" en verde success + icono launcher retocado + icono de notificación. Publicada 2026-09-23 por `android-release.yml`.
  - `app-release.apk` (9.379.039 B) — SHA256 `de5719a741a7fa2bbee20df9a93bc0e5bcd85b11400e72741e8b57fa10823c21`
  - Evidencia verificada descargando los assets de la Release v0.2.3 (API GitHub) y comprobando el checksum con `Get-FileHash`.
  - Instalada en el realme (mismo keystore, conserva vinculación); badge verde confirmado por muestreo de píxeles.
  - Anterior: `v0.2.0` (código 12, 2026-09-22) — UI rediseñada a **Material 3** (fases 0–13 del rediseño); `v0.1.10` (código 11) instalada y validada en el realme.
- **Backend en producción (VM e2-micro)**: desplegado con fix DWR (`899f740`), validado 100/100 contra Renfe real, CI verde. Ranking 197 tests OK, ruff/mypy OK.
- Bloques A–D completos. Los residuos del bot anterior NO se han eliminado (el bot anterior se conserva).

> No se declaran completos aquí requisitos que siguen pendientes de verificación; consulta `docs/requirements-checklist.md` y `PROGRESS.md`.

## Índice de documentación

### Arquitectura y decisiones
- `docs/architecture.md` — arquitectura del sistema (Android + backend + SQLite).
- `docs/security.md` — seguridad, autenticación backend↔app, FCM, anti-SSRF.
- `docs/notificaciones-fcm.md` — diseño de notificaciones FCM (canales, payload, Doze/realme).
- `docs/notificaciones-fcm.md` — canales de notificación definidos.

### Construcción, instalación y CI
- `docs/android-build.md` — compilación local del APK en **Windows** (JDK 17 + Gradle).
- `docs/instalacion-apk.md` — instalación/actualización manual del APK en el realme.
- `docs/keystore.md` — **clave de firma**: generación, backup, verificación y rotación.
- `docs/ci-cd.md` — CI/CD: workflows Android y backend en GitHub Actions.
- `docs/real-config-plan.md` — plan de configuración real (Firebase, VM, TLS, secrets).

### Operación en VM (GCP e2-micro)
- `docs/costs.md` — costes, cuotas y controles (0,00 €, Free Tier).
- `docs/recovery.md` — backup y recuperación de la base de datos SQLite.
- `docs/transicion.md` — plan de transición desde el bot original.
- `docs/security.md` — acuerdos de acceso admin a la VM.

### Validación y mantenimiento
- `docs/validation.md` — resultado de pruebas (190 tests backend OK, etc.).
- `docs/renfe-dwr-diagnosis/` — diagnóstico del flujo DWR con Renfe (fix token `generateId`).
- `docs/real-config-plan.md` — despliegue real y validación en realme (pasos 32–36).
- `docs/requirements-checklist.md` — checklist de requisitos comprobables.
- `docs/audit.md` — auditoría del repositorio original.
- `docs/transicion.md` — sustitución del bot anterior.

### Detección de errores y diagnóstico
- `docs/diagnostico-realme.md` — guía de diagnóstico para realme GT Neo 2.
- `docs/audit.md` — auditoría del código original.

## Uso rápido (resumen)

1. **Compilar el APK** (Windows): ver `docs/android-build.md`.
2. **Instalar/actualizar** en el realme: ver `docs/instalacion-apk.md`.
3. **Configurar el backend** en la VM: ver `docs/real-config-plan.md`.
4. **Backup/restore** de la BD: ver `docs/recovery.md`.
5. **Coste**: 0,00 €/mes con la VM e2-micro en Free Tier (`docs/costs.md`).

## Licencias y atribuciones
- El repositorio original (`renfe-notifier-bot`) aporta conocimiento del protocolo DWR de Renfe; este desarrolla el flujo desde cero. Ver `docs/licencias.md`.
- Código reutilizado: atribuciones y condiciones en `docs/licencias.md`.

## Cierre y mantenimiento
El estado del proyecto, pasos ejecutados y pendientes se registran en `PROGRESS.md`. Emplea el registro siguiente para continuar.
