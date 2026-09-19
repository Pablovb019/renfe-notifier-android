# Plan de Configuración Real (Paso 33)

> **Objetivo**: Preparar una lista exacta de acciones necesarias para habilitar la configuración real (Firebase, VM, TLS, secretos, distribución APK, mediciones en VM).
> **Regla**: Este paso **solo prepara el plan**; no ejecuta cambios. Requiere aprobación explícita por ítem antes de ejecutar (paso 34).

---

## 1. Firebase Spark + Permisos FCM

| Acción | Recurso afectado | Riesgo | Coste | Reversión |
|--------|------------------|--------|-------|-----------|
| Crear proyecto Firebase en plan **Spark (gratis)** | Firebase Console → nuevo proyecto | Bajo (proyecto aislado, sin facturación) | 0 € | Borrar proyecto desde Firebase Console |
| Habilitar **Cloud Messaging** | Proyecto Firebase → Cloud Messaging | Bajo | 0 € | Deshabilitar en consola |
| Generar **google-services.json** para Android (`com.pablovb019.renfenotifier`) | Firebase Console → Configuración proyecto → Apps Android | Bajo (archivo solo para app, no secreto de servidor) | 0 € | Borrar app de Firebase / regenerar archivo |
| Obtener **Project ID** (ej. `renfe-notifier-xxxxx`) | Firebase Console → Configuración general | N/A | 0 € | N/A |
| Dar a **cuenta de servicio de la VM** (OIDC) rol `roles/firebase.messaging.sender` | IAM de GCP → cuenta de servicio `deployer@project.iam` | Bajo (rol mínimo `firebase.messaging`) | 0 € | Quitar rol en IAM |

**Secrets GitHub a crear (Backend):**
- `RENFE_NOTIFIER_FCM_PROJECT_ID` = Project ID de Firebase (no secreto, pero en secrets por consistencia)

**Secrets GitHub a crear (Android CI):**
- `ANDROID_GOOGLE_SERVICES_JSON_B64` = `base64 -w0 google-services.json` (secreto, solo para workflows de release con `workflow_dispatch`)

**Validación gratuita**: Plan Spark de Firebase es perpetuamente gratuito (límites generosos: 10K mensajes/día, suficiente para uso personal).

---

## 2. Identidad del Servidor (VM + OIDC + IAP)

| Acción | Recurso afectado | Riesgo | Coste | Reversión |
|--------|------------------|--------|-------|-----------|
| Crear VM **e2-micro** en GCP (europe-west1-b, Ubuntu 22.04, sin IP externa) | Compute Engine → Instancia VM | Bajo (Free Tier: 1 e2-micro gratis/mes) | 0 € (Free Tier) | Eliminar instancia |
| Configurar **Workload Identity Federation (WIF)** | IAM → Workload Identity Pools | Medio (configuración una vez) | 0 € | Borrar pool/proveedor |
| Crear **cuenta de servicio** `deployer@project.iam.gserviceaccount.com` | IAM → Cuentas de servicio | Bajo | 0 € | Eliminar cuenta |
| Dar roles a cuenta de servicio: `compute.instanceAdmin.v1`, `iam.serviceAccountUser`, `firebase.messaging.sender` | IAM → Permisos | Bajo (roles mínimos) | 0 € | Quitar roles |
| Habilitar **Identity-Aware Proxy (IAP)** para SSH | IAP → Recursos → SSH | Bajo | 0 € | Deshabilitar IAP |
| Crear regla firewall `allow-iap-ssh` (puerto 22 desde `35.235.240.0/20`) | VPC Firewall | Bajo | 0 € | Borrar regla |
| Instalar **Tailscale** en VM (acceso admin privado opcional) | VM → `curl -fsSL https://tailscale.com/install.sh | sh` | Bajo | 0 € (plan personal gratis) | `tailscale down` + desinstalar |

> **DATO REAL DE INFRAESTRUCTURA (2026-09-19, para el futuro)**:
> - La VM `instance-renfe-notifier-android` está alojada en el proyecto GCP **`renfe-notifier-bot`** (NO en `renfe-notifier-android`). Usar `--project renfe-notifier-bot` en cualquier comando `gcloud compute` que la toque.
> - IP externa estática: `34.26.252.164` (puerto 8000 backend). Zona usada por defecto en `gcloud compute scp`: `us-east1-c`.
> - Acceso por **OS Login** (cuenta de servicio `sa_104384329745603569192`), usuario shell `sa_104384329745603569192`, home en `~/renfe-notifier-android/backend`. La cuenta local `pablovb01@gmail.com` NO tiene compute habilitado sobre `renfe-notifier-android`; por eso los `gcloud` fallan a menos que se indique `--project renfe-notifier-bot`.
> - La VM NO tiene bot antiguo ni rastro de él (inventario pasos 37-B); solo corre el backend nuevo (`renfe-notifier-backend.service`).
> - Firma SSH del host `34.26.252.164` (ed25519, registrada al primer scp): `SHA256:RoYpXifW9rJPOZWA+4DvNn4UmyOWwxXgN03q2wWxQns`.

**Secrets GitHub a crear:**
- `GCP_PROJECT_ID` = ID del proyecto GCP
- `GCP_WORKLOAD_IDENTITY_PROVIDER` = resource name del proveedor WIF (ej. `projects/123/locations/global/workloadIdentityPools/github-pool/providers/github-provider`)
- `GCP_SERVICE_ACCOUNT` = `deployer@project.iam.gserviceaccount.com`
- `VM_NAME` = nombre de la instancia (ej. `renfe-notifier-vm`)
- `VM_ZONE` = zona (ej. `europe-west1-b`)
- `DEPLOY_USER` = usuario SSH (ej. `ubuntu`)

**Validación gratuita**: e2-micro en Free Tier (744h/mes), IAP gratis, WIF gratis, Tailscale plan personal gratis.

---

## 3. TLS y Acceso Android/Backend

| Acción | Recurso afectado | Riesgo | Coste | Reversión |
|--------|------------------|--------|-------|-----------|
| Configurar **dominio personalizado** (opcional) o usar IP pública + certificado autofirmado para desarrollo | Dominio / Certificado | Medio (requiere dominio o cert autofirmado) | 0 € (cert autofirmado) / ~10 €/año (dominio) | Eliminar cert / no renovar dominio |
| Backend: configurar `RENFE_NOTIFIER_FCM_PROJECT_ID` en VM | VM / systemd env | Bajo | 0 € | Borrar variable |
| Android: `google-services.json` en `app/` (solo build release, no debug) | Android app | Bajo | 0 € | Quitar archivo |
| Backend: permitir solo HTTPS en producción (force_https middleware) | FastAPI app | Bajo | 0 € | Quitar middleware |
| Android: `network_security_config.xml` para cert autofirmado en dev | Android app | Bajo | 0 € | Quitar config |

**Nota**: Para validación inicial (paso 35), usar túnel Cloudflare (`cloudflared tunnel --url http://localhost:8000`) o ngrok gratis → HTTPS temporal sin certificado propio.

---

## 4. Acceso Administrativo y CD

| Acción | Recurso afectado | Riesgo | Coste | Reversión |
|--------|------------------|--------|-------|-----------|
| Configurar **secrets GitHub** para CD (backend) | GitHub Settings → Secrets → Actions | Bajo (secrets encriptados) | 0 € | Borrar secrets |
| Configurar **secrets GitHub** para release Android | GitHub Settings → Secrets → Actions | Bajo (keystore en base64) | 0 € | Borrar secrets |
| Probar workflow `backend-cd` con `workflow_dispatch` (dry-run sin VM) | GitHub Actions | Bajo | 0 € | N/A |
| Probar workflow `android-release` con `workflow_dispatch` (sin firmar) | GitHub Actions | Bajo | 0 € | N/A |

**Secrets GitHub backend CD (ya listos en docs/ci-cd.md):**
- `GCP_PROJECT_ID`, `GCP_WORKLOAD_IDENTITY_PROVIDER`, `GCP_SERVICE_ACCOUNT`, `VM_NAME`, `VM_ZONE`, `DEPLOY_USER`

**Secrets GitHub Android release:**
- `ANDROID_KEYSTORE_BASE64` (keystore.jks en base64)
- `KEYSTORE_PASSWORD`, `KEY_ALIAS`, `KEY_PASSWORD`

---

## 5. Secretos de GitHub (Resumen consolidado)

| Secret | Workflow(s) | Descripción | Sensibilidad |
|--------|-------------|-------------|--------------|
| `GCP_PROJECT_ID` | `backend-cd` | ID proyecto GCP | Baja (público en URLs) |
| `GCP_WORKLOAD_IDENTITY_PROVIDER` | `backend-cd` | Resource name WIF | Media |
| `GCP_SERVICE_ACCOUNT` | `backend-cd` | Email cuenta de servicio | Media |
| `VM_NAME` | `backend-cd` | Nombre instancia VM | Baja |
| `VM_ZONE` | `backend-cd` | Zona GCP | Baja |
| `DEPLOY_USER` | `backend-cd` | Usuario SSH VM | Baja |
| `RENFE_NOTIFIER_FCM_PROJECT_ID` | `backend-cd` (env) | Project ID Firebase | Baja |
| `ANDROID_KEYSTORE_BASE64` | `android-release` | Keystore.jks base64 | **ALTA** |
| `KEYSTORE_PASSWORD` | `android-release` | Password keystore | **ALTA** |
| `KEY_ALIAS` | `android-release` | Alias clave (ej. `renfe-notifier`) | Media |
| `KEY_PASSWORD` | `android-release` | Password clave | **ALTA** |
| `ANDROID_GOOGLE_SERVICES_JSON_B64` | `android-release` (opcional) | google-services.json b64 | **ALTA** |

**Regla**: Ningún secreto real en el repositorio. Keystore y google-services.json solo en secrets GitHub (encriptados). `docs/keystore.md` documenta generación y backup seguro (2+ ubicaciones físicas).

---

## 6. Distribución del APK

| Acción | Recurso afectado | Riesgo | Coste | Reversión |
|--------|------------------|--------|-------|-----------|
| Generar `keystore.jks` local (`keytool`) | Local (Windows) | **ALTA** (pérdida = no actualizaciones) | 0 € | Backup en 2+ ubicaciones físicas |
| Configurar secrets Android release en GitHub | GitHub Secrets | Media | 0 € | Borrar secrets |
| Workflow `android-release` (tags `v*.*.*` + `workflow_dispatch`) | GitHub Actions | Bajo | 0 € | Deshabilitar workflow |
| Release privada en GitHub (APK + SHA256) | GitHub Releases (privado) | Bajo | 0 € | Borrar release |
| Instalación manual en realme GT Neo 2 (adb / archivo) | Dispositivo físico | Bajo | 0 € | Desinstalar app |

**Nota**: `docs/keystore.md` ya documenta generación, backup (2+ ubicaciones físicas: USB encriptado + gestor contraseñas), verificación (`apksigner verify`), rotación. **No se ejecuta hasta aprobación**.

---

## 7. Mediciones Pendientes en VM (Paso 32 → 35)

| Medición | Qué mide | Cómo | Cuándo |
|----------|----------|------|--------|
| Latencia real Renfe (DWR) desde GCP | HTTP requests reales a `venta.renfe.com` | `curl -w "@format.txt" -o /dev/null -s` en VM | Tras VM aprobada |
| CPU/RAM carga sostenida 1h | `pidstat`, `psutil` en proceso backend | Script `measure_resources.py` adaptado a VM | Tras VM + backend desplegado |
| GC Python bajo carga | `gc.get_stats()`, `tracemalloc` | Logs estructurados | Tras VM + carga |
| SQLite WAL concurrente | `PRAGMA wal_checkpoint`, locks | Benchmark concurrencia 2 | Tras VM + backend |
| Bytes facturados vs app | `tc` / `nethogs` / GCP billing | Comparar bytes app vs facturación GCP | Tras VM + 24h |
| Arranque docker compose + health check | Tiempo `docker compose up -d` + health check | Script deploy | Tras VM + docker |

**Decisión Selenium**: **NO implementar** salvo que mediciones VM demuestren que HTTP DWR falla (bloqueo Renfe). Plan B documentado en `docs/measurements.json`: opcional, bajo demanda, serializado, recursos acotados.

---

## 8. Checklist de Aprobación (Paso 34)

Antes de ejecutar (paso 34), cada ítem debe tener ✅ aprobación explícita:

| # | Ítem | Aprobación (sí/no) | Comentarios |
|---|------|-------------------|-------------|
| 1 | Crear proyecto Firebase Spark + google-services.json | | |
| 2 | Crear VM e2-micro + OIDC/WIF + IAP + Tailscale | | |
| 3 | Configurar secrets GitHub (backend CD + Android release) | | |
| 4 | Generar keystore.jks local + backup seguro (2+ ubicaciones) | | |
| 5 | Configurar secrets Android release (keystore + google-services.json) | | |
| 6 | Ejecutar workflow `backend-cd` (dry-run → real) | | |
| 7 | Ejecutar workflow `android-release` (tag v0.1.0) | | |
| 8 | Instalar APK en realme GT Neo 2 + probar FCM real | | |
| 9 | Ejecutar mediciones VM reales (paso 32 pendientes) | | |

---

## 9. Coste Total Estimado (0 €)

| Recurso | Coste mensual | Notas |
|---------|---------------|-------|
| VM e2-micro | 0 € (Free Tier 744h) | 1 instancia, 1 GB RAM |
| IAP / WIF / OIDC | 0 € | Servicios GCP gratuitos |
| Firebase Spark | 0 € | Plan perpetuo gratuito |
| GitHub Actions | 0 € (2000 min/mes Free) | ~100-160 min/mes estimados |
| GitHub Packages / Releases | 0 € (500 MB gratis) | APK ~13 MB, retención 5 días |
| Tailscale | 0 € (plan personal) | 1 dispositivo |
| Dominio / TLS | 0 € (cert autofirmado / túnel) | Opcional: ~10 €/año si dominio |

**Total: 0 €** — Cumple restricción "Presupuesto total: 0 €".

---

## 10. Reversión Global (Plan de Contingencia)

Si cualquier paso falla o se revoca aprobación:
1. **Firebase**: Borrar proyecto / quitar rol `firebase.messaging.sender`
2. **VM**: `gcloud compute instances delete` / borrar reglas firewall / IAP
3. **GitHub Secrets**: Borrar secrets individuales
4. **Keystore**: No se puede revocar (backup físico es la única protección)
5. **GitHub Actions**: Deshabilitar workflows (`backend-cd`, `android-release`)
5. **App en dispositivo**: `adb uninstall com.pablovb019.renfenotifier`

---

## 11. Próximos Pasos

1. **Revisión del usuario**: Marcar ✅/❌ en checklist §8
2. **Paso 34**: Ejecutar solo ítems aprobados (configuración real)
3. **Paso 35**: Validación real controlada (realme + FCM + Renfe real)
4. **Paso 36**: Plan transición (backup bot, arranque backend nuevo)
4. **Paso 37**: Transición autorizada (si todo valida)
5. **Paso 38**: Entrega y mantenimiento

---

> **Nota**: Este documento **no ejecuta nada**. Espera aprobación explícita del usuario en checklist §8 antes de proceder al paso 34.