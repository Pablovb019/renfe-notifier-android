# Plan de CI/CD y Automatización: renfe-notifier-android

> **AVISO**: Este documento define el diseño arquitectónico y las especificaciones de los workflows. **Ningún workflow debe habilitarse ni crearse en `.github/workflows/` hasta su aprobación expresa**.

---

## 1. Principios y Restricciones Operativas

1. **Coste Cero Garantizado (0,00 €)**:
   - Consumo estimado de GitHub Actions: ~100–160 minutos/mes sobre los 2.000 minutos mensuales gratuitos para repositorios privados en cuentas Free.
   - Límite de gasto configurado en **0,00 $ USD** en la cuenta de GitHub para impedir cualquier facturación automática en caso de reintentos o saturación.
   - Retención de artefactos configurada en **3 días** para no consumir el límite de 500 MB.
2. **Sin Runners en la VM de Producción**:
   - Todo el cómputo de CI se realiza en runners Linux estándar de GitHub (`ubuntu-latest`).
   - **Prohibido instalar self-hosted runners en la VM `e2-micro`**: la compilación de Android y los tests saturarían su única vCPU compartida y 1 GB de RAM.
3. **Aislamiento Absoluto de Entornos y Datos Sensibles**:
   - Las pruebas automatizadas en CI **nunca se conectan a los servidores de Renfe ni a la base de datos de producción**. Todas las pruebas utilizan fixtures estáticos y mocks.
   - Las pull requests y branches no autorizadas no tienen acceso a secretos de despliegue ni a claves de firma privada.
4. **Fijación de Acciones por SHA y Permisos Mínimos**:
   - Todas las GitHub Actions externas deben fijarse por su commit hash completo (SHA inmutable) con etiqueta de versión en comentario, mitigando riesgos de supply chain attack.
   - El token por defecto `GITHUB_TOKEN` opera con permisos mínimos de solo lectura:
     ```yaml
     permissions:
       contents: read
     ```

---

## 2. Integración Continua (CI)

### 2.1. Matriz y Filtrado de Cambios sin Checks Bloqueados

Para evitar que las reglas de protección de rama (`Branch Protection Rules`) queden eternamente pendientes cuando un commit afecta únicamente a una parte del proyecto (por ejemplo, sólo a Android o sólo a backend), se diseña un **Workflow Orquestador Unificado**:

```
                              Evento: Push / Pull Request a main
                                              │
                                              ▼
                                   ┌─────────────────────┐
                                   │ Job: Detect Changes │
                                   │ (paths-filter SHA)  │
                                   └──────────┬──────────┘
                                              │
                     ┌────────────────────────┴────────────────────────┐
                     │                                                 │
            ¿Cambios en backend/**?                           ¿Cambios en android/**?
                     │                                                 │
            ┌────────▼────────┐                               ┌────────▼────────┐
            │   Job: CI       │                               │   Job: CI       │
            │   Backend       │                               │   Android       │
            │ (Lint + Tests)  │                               │ (Lint + Build)  │
            └────────┬────────┘                               └────────┬────────┘
                     │                                                 │
                     └────────────────────────┬────────────────────────┘
                                              │
                                              ▼
                                   ┌─────────────────────┐
                                   │ Job: All Checks OK  │
                                   │ (Gate de Aprobación)│
                                   └─────────────────────┘
```

- **Rutas de activación (`paths`)**:
  - `backend`: `backend/**`, `data/stations.json`, `.github/workflows/ci.yml`.
  - `android`: `android/**`, `.github/workflows/ci.yml`.
- Si una ruta no tiene cambios, el job correspondiente se omite limpiamente y el job agregador `All Checks OK` evalúa la condición de éxito sin bloquear la fusión.

### 2.2. Pipeline de Backend en CI
1. **Entorno**: `ubuntu-latest`, Python 3.12.
2. **Caché**: `actions/setup-python` con `cache: 'pip'`.
3. **Análisis Estático**:
   - `ruff check .` (linter ultrarrápido) y `ruff format --check .`.
   - `mypy .` para comprobación estricta de tipos.
4. **Pruebas Unitarias**:
   - `pytest` con cobertura (`pytest-cov`).
   - Verificación de parser DWR con respuestas grabadas (fixtures JSON).
   - Verificación de SQLite (migraciones, concurrencia, transacciones WAL).
   - Duración estimada: **< 1 minuto**.

### 2.3. Pipeline de Android en CI
1. **Entorno**: `ubuntu-latest`, JDK 17.
2. **Caché**: `gradle/actions/setup-gradle` para cachear dependencias y el wrapper de Gradle.
3. **Análisis y Pruebas**:
   - `./gradlew lintDebug` (análisis estático oficial de Android).
   - `./gradlew testDebugUnitTest` (pruebas unitarias de ViewModels, repositorios y mapeadores).
4. **Compilación de Verificación**:
   - `./gradlew assembleDebug` (compila el APK de depuración para verificar la integridad del ensamblado).
5. **Artefacto de Depuración**:
    - Sube `app-debug.apk` con retención estricta de **1 día** (reducido desde 3 días para ahorrar cuota).
    - Duración estimada: **~3–5 minutos**.

### 2.3.1. Configuración de Firebase en CI (cuando corresponda)
La compilación **debug no requiere `google-services.json`**: la app guarda runtime con `FirebaseApp.getApps(context).isNotEmpty()` y compila sin el plugin de Google Services. Para validar FCM real (pasos 33-34):
1. Codificar `google-services.json` en Base64 y almacenarlo como secreto `ANDROID_GOOGLE_SERVICES_JSON_B64` en GitHub.
2. En un workflow manual (`workflow_dispatch`), decodificarlo en `android/app/google-services.json` antes de `assembleDebug`.
3. **Nunca** commitear el archivo ni incluirlo en caché/artefactos.
La CI de verificación (este workflow) omite este paso y valida solo la compilación debug sin credenciales.

### 2.4. Gestión de Concurrencia y Ahorro de Minutos
Para no desperdiciar minutos gratuitos en commits sucesivos:
```yaml
concurrency:
  group: ci-${{ github.ref }}
  cancel-in-progress: true
```
Si se envía un nuevo commit a una PR antes de que termine el anterior, el workflow previo se cancela automáticamente de inmediato.

---

## 3. Despliegue Continuo (CD) del Backend

### 3.1. Condiciones y Gobernanza de Despliegue
- **Activación**: Exclusivamente **manual** mediante `workflow_dispatch`.
- **Serialización Estricta**:
  ```yaml
  concurrency:
    group: production-deploy
    max-parallel: 1
  ```
- **Verificación Criptográfica del Commit**:
  - El workflow verifica mediante la API de GitHub que:
    1. El commit seleccionado pertenece a la rama `main`.
    2. El SHA exacto ha superado con éxito todos los checks de CI.
    3. Si el commit no ha pasado CI, el despliegue se detiene antes de conectar a la infraestructura.

### 3.2. Conectividad Segura con la VM (OIDC + IAP)

> [!IMPORTANT] **LIMITACIÓN DE OIDC RESUELTA**:
> La autenticación mediante Google Cloud Workload Identity Federation (OIDC) autentica al runner ante las APIs de GCP, pero **no proporciona por sí sola un canal de red SSH hacia una VM privada**.
> Dado que la VM de producción utiliza Tailscale y no tiene puertos abiertos al exterior en Internet, el despliegue automático desde GitHub Actions se realiza mediante **Google Cloud Identity-Aware Proxy (IAP)**:
> - El runner se autentica con OIDC (sin claves privadas de servicio almacenadas en GitHub).
> - Se conecta mediante el túnel seguro de IAP (`gcloud compute ssh --tunnel-through-iap`), que no requiere IP pública, no abre puertos al mundo y es completamente gratuito en GCP.

### 3.3. Procedimiento de Despliegue en la VM (Paso a Paso con Rollback)

El script de despliegue ejecutado en la VM sigue un protocolo transaccional estricto:

```
                  ┌──────────────────────────────────────────┐
                  │ 1. Backup consistente de SQLite (WAL)    │
                  │    sqlite3 .backup a /data/backups/      │
                  └────────────────────┬─────────────────────┘
                                       │
                                       ▼
                  ┌──────────────────────────────────────────┐
                  │ 2. Actualización de código validado      │
                  │    (Checkout del commit SHA exacto)      │
                  └────────────────────┬─────────────────────┘
                                       │
                                       ▼
                  ┌──────────────────────────────────────────┐
                  │ 3. Ejecución de migraciones SQLite       │
                  │    python -m app.db.migrate              │
                  └────────────────────┬─────────────────────┘
                                       │ ¿Migración OK?
                             ┌─────────┴─────────┐
                          SÍ │                   │ NO
                             ▼                   ▼
     ┌──────────────────────────────────┐     ┌──────────────────────────────────┐
     │ 4. Reinicio de contenedor Docker │     │ ROLLBACK INMEDIATO:              │
     │    docker compose up -d          │     │ Restaurar DB desde backup        │
     └─────────────────┬────────────────┘     └──────────────────────────────────┘
                       │
                       ▼
     ┌──────────────────────────────────┐
     │ 5. Health Check Post-Despliegue  │
     │    curl http://localhost:8000/   │
     │    (reintentos durante 30s)      │
     └─────────────────┬────────────────┘
                       │ ¿Saludable?
             ┌─────────┴─────────┐
          SÍ │                   │ NO
             ▼                   ▼
    ┌──────────────────┐      ┌──────────────────────────────────┐
    │ Despliegue Éxito │      │ ROLLBACK AUTOMÁTICO:             │
    │ Notificar fin    │      │ - Volver al commit anterior      │
    └──────────────────┘      │ - Restaurar base de datos        │
                              │ - Reiniciar servicio previo      │
                              └──────────────────────────────────┘
```

1. **Backup Previo Obligatorio**:
   ```bash
   TIMESTAMP=$(date +%Y%m%d_%H%M%S)
   sqlite3 /data/renfe_notifier.db ".backup '/data/backups/backup_${TIMESTAMP}.db'"
   ```
   El comando `.backup` de SQLite es atómico y garantiza una instantánea íntegra y consistente incluso si hay transacciones activas en el archivo WAL.
2. **Ejecución de Migraciones**:
   - Aplica scripts versionados de forma incremental. Si una migración falla, se detiene antes de tocar el contenedor.
3. **Health Check y Rollback Automático**:
   - Se sondea el endpoint `GET /api/v1/diagnostics/health` durante 30 segundos.
   - Si no responde `HTTP 200 OK`, el script revierte automáticamente al commit anterior y restaura la copia de seguridad de SQLite.

---

## 4. Entrega y Distribución del APK de Android

### 4.1. Generación de Versión Firmada
- **Activación**: Manual mediante `workflow_dispatch` o mediante creación de una etiqueta de versión (`v1.0.0`).
- **Gestión Criptográfica de la Clave de Firma (Keystore)**:
  - El archivo `keystore.jks` se almacena como secreto de GitHub codificado en Base64 (`ANDROID_KEYSTORE_BASE64`).
  - Las credenciales (`KEYSTORE_PASSWORD`, `KEY_ALIAS`, `KEY_PASSWORD`) se configuran como GitHub Secrets protegidos.
  - **Ciclo de vida del Keystore temporal**:
    - El archivo físico se decodifica en un directorio temporal en memoria durante el paso de compilación.
    - Se destruye y borra de forma segura (`shred -u` o `rm -f`) inmediatamente después de la firma, antes de finalizar el job.
    - Nunca se almacena en cachés ni se incluye en los artefactos generados.

### 4.2. Distribución y Verificación de Integridad
- El workflow compila `./gradlew assembleRelease` y firma con `apksigner`.
- Se genera el hash criptográfico SHA-256 del APK resultante:
  ```bash
  sha256sum app-release.apk > app-release.apk.sha256
  ```
- Se publica el APK firmado y su archivo `.sha256` en una **GitHub Release privada**.
- **Instalación en el teléfono realme GT Neo 2**:
  - Descarga manual del APK desde la interfaz privada de GitHub.
  - Instalación manual en Android 13 mediante el instalador de paquetes del sistema.
  - Al mantenerse la misma clave de firma a lo largo del proyecto, las actualizaciones sucesivas se instalan de forma incremental sobre la versión anterior sin pérdida de datos.

---

## 5. Plan de Contingencia: Compilación y Despliegue Manual desde Windows

Si se agotase la cuota de 2.000 minutos de GitHub Actions o no hubiese conectividad con el servicio de automatización, se dispondrá de un script local en PowerShell (`scripts/dev_local.ps1`):

1. **Backend**:
   - Ejecución local de tests: `pytest backend/tests`.
   - Conexión directa a la VM por SSH (vía consola Google Cloud o Tailscale) para actualizar el código y reiniciar el contenedor manualmente.
2. **Android**:
   - Compilación local en el PC Windows del usuario:
     ```powershell
     .\gradlew.bat testDebugUnitTest
     .\gradlew.bat assembleRelease
     ```
   - Firma mediante `apksigner` local con el keystore guardado en el PC del usuario.
   - Transferencia directa al móvil realme mediante cable USB (`adb install -r app-release.apk`) o descarga local.
