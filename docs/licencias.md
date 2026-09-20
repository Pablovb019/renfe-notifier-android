# Licencias y Atribuciones

> Objetivo del paso 38: dejar constancia de las licencias y atribuciones del proyecto, enlazando la evidencia ya verificada. **No se añade ninguna dependencia ni licencia que no esté documentada** en el árbol; si algo no está verificado, se indica como pendiente en `docs/requirements-checklist.md`.

## 1. Repositorio original (renfe-notifier-bot, referencia)

- El proyecto parte de la especificación y de la auditoría del repositorio `Pablovb019/renfe-notifier-bot` (inalterado, REQ-01.2 en `docs/requirements-checklist.md`; auditoría en `docs/audit.md`).
- La licencia del repositorio original se verificó como **MIT** (evidencia en `docs/audit.md`, requisito REQ-01.5 de `docs/requirements-checklist.md`).

> **Nota de conservación**: El código y el bot anterior NO se eliminan; la transición y rollback están documentados en `docs/transicion.md`.

## 2. Código generado en este repositorio

- Backend (Python/FastAPI), app Android (Kotlin/Jetpack Compose), workflows CI/CD y documentación: **escritos desde cero** en este proyecto (auditoría interna `docs/audit.md` confirma que no hay copia literal del bot original salvo conocimiento adquirido del protocolo DWR, documentado en `docs/renfe-dwr-diagnosis/`).
- Evidencia de autoría/limpieza: 190 tests backend + validaciones Android en `docs/validation.md`.

## 3. Dependencias y frameworks (licencias conocidas, documentadas en el propio árbol de dependencias)

Las dependencias reales usadas y sus licencias forman parte de los artefactos de build (Gradle/BOM, requirements) y su verificación se registra en `docs/ci-cd.md` y `docs/validation.md`. Para la conformidad final de cada revisor, cada release incluye el checksum y el artefacto firmado (ver `README.md` y `docs/instalacion-apk.md`).

## 4. Firma y keystore

- La clave de firma (keystore) y su conservación: `docs/keystore.md` — incluye procedimiento de backup, verificación (`apksigner verify`) y rotación. El keystore **no se versiona** en git (regla de seguridad, `docs/security.md`).

## 5. Checksums y evidencia

- Los checksums de los artefactos de cada release (APK + `.sha256`) se publican como assets de la release y se verifican según el procedimiento de `docs/validation.md` y `docs/ci-cd.md`.

## 6. Pendiente de verificación (NO se declara completo)

- Revisión de terceros/legal: lista motu proprio de dependencias y sus licencias **no realizada todavía**; se registra como pendiente en `docs/requirements-checklist.md` §Requisitos pendientes. No se declara completa ninguna atribución sin evidencia comprobable (regla del proyecto).
