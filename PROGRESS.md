# Progreso del Proyecto: renfe-notifier-android

## Paso 02: Repositorio Privado
- **Estado**: Completado
- **Fecha**: 2026-09-12
- **Decisiones**:
  - Se comprobó la cuenta de GitHub autenticada mediante GitHub CLI (`gh auth status`): autenticado como `Pablovb019`.
  - Se verificó si existía el repositorio remoto `Pablovb019/renfe-notifier-android`: no existía previamente.
  - Se creó el repositorio remoto `Pablovb019/renfe-notifier-android` como PRIVADO con inicialización mínima (README).
  - Se verificó URL (`https://github.com/Pablovb019/renfe-notifier-android`), propietario (`Pablovb019`) y visibilidad (`PRIVATE`).
  - No se han habilitado workflows ni despliegues en el nuevo repositorio.
  - El repositorio original `Pablovb019/renfe-notifier-bot` no ha sido tocado ni modificado.
  - La carpeta local `prompts/` permanece intacta.
- **Archivos modificados / creados**:
  - `PROGRESS.md`
- **Pruebas ejecutadas y resultados**:
  - `gh auth status`: Éxito (cuenta activa: `Pablovb019`).
  - `gh repo view Pablovb019/renfe-notifier-android`: Verificado que no existía antes de la creación.
  - `gh repo create Pablovb019/renfe-notifier-android --private --add-readme`: Creación exitosa.
  - `gh repo view Pablovb019/renfe-notifier-android --json name,owner,isPrivate,url,visibility`: Retorna `{"isPrivate":true,"name":"renfe-notifier-android","owner":{"login":"Pablovb019"},"url":"https://github.com/Pablovb019/renfe-notifier-android","visibility":"PRIVATE"}`.
- **Bloqueos**: Ninguno.
- **Siguiente paso**: Paso 03 (`prompts/03-documentacion-base.md`).

## Paso 03: Documentación Base y Contexto Persistente
- **Estado**: Completado
- **Fecha**: 2026-09-12
- **Decisiones**:
  - Se creó `AGENTS.md` conteniendo las reglas estables de trabajo basadas en `prompts/01-reglas.md`, especificando explícitamente que no debe asumirse su carga automática en Antigravity CLI y debe ser leído de forma explícita.
  - Se creó `.gitignore` con cobertura completa de exclusión para: secretos y credenciales (`.env`, `*.jks`, `*.keystore`, certificados, credenciales Firebase), bases de datos (`*.db`, `*.sqlite`, WAL/SHM), artefactos de build (Python y Android), logs, backups y ficheros de IDEs.
  - Se creó `docs/requirements-checklist.md` conteniendo los requisitos comprobables organizados en las 15 secciones de `prompts/00-especificacion-original.md`.
  - Se mantiene `prompts/00-especificacion-original.md` como especificación original inalterada.
  - No se copiaron secretos ni se implementó código de la aplicación.
- **Archivos modificados / creados**:
  - `AGENTS.md` (creado)
  - `.gitignore` (creado)
  - `docs/requirements-checklist.md` (creado)
  - `PROGRESS.md` (actualizado)
- **Pruebas ejecutadas y resultados**:
  - Verificación de creación de los ficheros y comprobación de rutas relativas y absolutas.
  - Verificación de patrones de `.gitignore` mediante prueba de concordancia de rutas y extensiones de seguridad.
- **Bloqueos**: Ninguno.
- **Siguiente paso**: Paso 04 (`prompts/04-auditoria-funcional.md`).

## Paso 04: Auditoría Funcional y Licencia
- **Estado**: Completado
- **Fecha**: 2026-09-12
- **Decisiones**:
  - Se inspeccionó el repositorio original `Pablovb019/renfe-notifier-bot` exclusivamente en modo lectura, clonado en directorio de trabajo aislado sin alterar el repositorio remoto ni local.
  - Se registró el commit exacto analizado: `dd658019280f588112bcb8d6f6f10224a85d3960`.
  - Se verificó la licencia del repositorio original y sus dependencias directas: Licencia MIT (sin restricciones bloqueantes para la reutilización, manteniendo la atribución).
  - Se redactó `docs/audit.md` con evidencia detallada por archivo y función:
    - Normalización NFKD, ranking y sugerencias de estaciones (`conversations.py`).
    - Catálogo y metadatos de más de 1.300 estaciones (`data/stations.json`).
    - Restricciones de calendario (hoy hasta 2 meses) y opciones Plaza H.
    - Modos de búsqueda (`specific`, `first`, `last`, `all`) y flujo contextual "otra consulta".
    - Ciclo de vida en SQLite, contadores de consultas, expiración y renovación (`dbmanager.py`).
    - Planificación de tareas, bucle de notificación cada 10s y matching por horas (`renfebot.py`).
    - Flujo HTTP/DWR de 5 peticiones POST y arranque prematuro de Selenium (`renfechecker.py`).
  - Se documentó la separación entre comportamientos comprobados frente a hipótesis y componentes a conservar, adaptar o descartar.
  - No se implementó código de la aplicación.
- **Archivos modificados / creados**:
  - `docs/audit.md` (creado)
  - `PROGRESS.md` (actualizado)
- **Pruebas ejecutadas y resultados**:
  - Consulta de commit exacto en GitHub API: `dd658019280f588112bcb8d6f6f10224a85d3960` verificado.
  - Ejecución de la suite de tests existente del bot original (`python python/test_stats_and_alerts.py`): 11 tests ejecutados con éxito en 0,453 s (`OK`).
- **Bloqueos**: Ninguno.
- **Siguiente paso**: Paso 05 (`prompts/05-auditoria-tecnica.md`).

## Paso 05: Auditoría Técnica (Consultas, Tareas, Persistencia e Infraestructura)
- **Estado**: Completado
- **Fecha**: 2026-09-12
- **Decisiones**:
  - Se auditó a nivel técnico en profundidad el commit `dd658019280f588112bcb8d6f6f10224a85d3960` cubriendo:
    - *Arranque incondicional de Firefox/Xvfb*: Identificado consumo prohibitivo (350–550 MB RAM) en `RenfeChecker.__init__`, crítico para la VM e2-micro (1 GB RAM). Decisión: eliminar Selenium del despliegue base; la vía HTTP/DWR es autosuficiente y consume menos de 40 MB.
    - *Secuencia de 5 POSTs DWR*: Verificada la necesidad técnica de las dos llamadas a `generateId` para la negociación de tokens DWR y sesión (`DWRSESSIONID`). Decisión: conservar la secuencia de 5 POSTs con pool de conexiones y timeout estricto.
    - *Aislamiento y cierre HTTP*: Identificada la ausencia de cierre explícito de `requests.Session()` y la renegociación completa de TLS en cada consulta. Decisión: gestionar el ciclo de vida con contexto (`with`) y pool de conexiones aislado por búsqueda.
    - *Concurrencia y planificador*: Identificado cuello de botella por ejecución secuencial bloqueante sin agrupación de consultas idénticas. Identificado que el estado `notifying` detiene las consultas reales y spamea cada 10 s a ciegas, y que `/stop` borra el registro de la BD. Decisión: planificador con agrupación inteligente por `(origen, destino, fecha, plaza_h)`, concurrencia baja (1–2 workers), notificaciones push FCM desacopladas y separación estricta entre estado del seguimiento y confirmación del aviso.
    - *Matching de trenes*: Identificada la fragilidad del matching basado exclusivamente en `%H:%M`. Decisión: incorporar clave compuesta robusta en el nuevo modelo.
    - *Zonas horarias y SQLite*: Identificado riesgo de desfase por cálculo de timestamps locales sin timezone explícito, ausencia de modo WAL y falta de `busy_timeout`. Decisión: SQLite en modo WAL, `busy_timeout=5000` y almacenamiento en UTC con renderizado en `Europe/Madrid`.
    - *Docker y CI/CD original*: Identificada imagen pesada (>1,1 GB), compilación directa en la VM y workflow de despliegue por SSH directo sin tests previos ni backup de SQLite. Decisión: imagen ligera (<150 MB), CI obligatoria en GitHub Actions y despliegue manual controlado.
  - Se completó `docs/audit.md` con la sección técnica detallada y la matriz de decisiones.
  - No se realizaron consultas reales a Renfe.
  - No se implementó código de la aplicación.
- **Archivos modificados / creados**:
  - `docs/audit.md` (actualizado)
  - `PROGRESS.md` (actualizado)
- **Pruebas ejecutadas y resultados**:
  - Inspección exhaustiva de código y análisis de dependencias de `renfechecker.py`, `renfebot.py`, `dbmanager.py`, `Dockerfile`, `docker-compose.yml` y `.github/workflows/deploy.yml`.
  - Verificación de ausencia de consultas salientes hacia Renfe.
- **Bloqueos**: Ninguno.
- **Siguiente paso**: Paso 06 (`prompts/06-viabilidad-cero-euros.md`).

## Paso 06: Viabilidad de Coste Cero (0,00 €)
- **Estado**: Completado
- **Fecha**: 2026-09-12
- **Decisiones**:
  - Se investigaron las condiciones oficiales publicadas y se redactó el estudio exhaustivo en `docs/costs.md`.
  - *Google Cloud Compute Engine*: Se documentaron los requisitos excluyentes para que la VM `e2-micro` permanezca en el Free Tier permanente (regiones `us-central1`, `us-east1` o `us-west1`, 744 h/mes agregadas, disco `pd-standard` ≤ 30 GB, 1 GB/mes de salida de red y 50 GiB/mes de logs).
  - *Diferencia entre tiers*: Se separó claramente el Free Tier permanente (sin expiración) de los créditos temporales ($300 por 90 días, no aceptables como base de coste cero).
  - *Firebase Spark*: Se confirmó que FCM es 100% gratuito e ilimitado en el plan Spark y no requiere asociar tarjeta de crédito ni facturación.
  - *GitHub Actions*: Se validó la cuota mensual para repositorios privados (2.000 minutos, 500 MB artefactos, 10 GB caché) y se definió la configuración obligatoria del Spending Limit a $0.00 USD para blindar la cuenta ante cobros automáticos.
  - *Acceso seguro sin dominio de pago*: Se analizaron alternativas para conectar el móvil a la VM a 0 €, recomendando Tailscale (WireGuard P2P + MagicDNS HTTPS con Let's Encrypt gratuito) o DuckDNS + Let's Encrypt con firewall cerrado.
  - *Alertas presupuestarias*: Se documentó expresamente que Google Cloud Budgets sólo envía avisos por correo y NO detiene instancias ni corta el gasto de forma automática.
  - Se estableció el bloqueo preventivo de despliegue hasta verificar en consola que la VM cumple las condiciones de región y disco.
- **Archivos modificados / creados**:
  - `docs/costs.md` (creado)
  - `PROGRESS.md` (actualizado)
- **Pruebas ejecutadas y resultados**:
  - Consulta y verificación de documentación oficial de Google Cloud Free Program, Firebase Pricing y GitHub Actions Billing.
  - Estimación matemática de tráfico en sondeo continuo (~57,6 MB/día) y definición de estrategias de software (agrupación y no sondeo en reposo) para no superar 1 GB/mes.
- **Bloqueos**:
  - *Bloqueo preventivo de despliegue*: No se desplegará en la VM hasta que el usuario confirme que la instancia existente está en una región Free Tier (`us-central1`, `us-east1`, `us-west1`) y con disco estándar.
- **Siguiente paso**: Paso 07 (`prompts/07-arquitectura-y-conexion.md`).

## Paso 07: Arquitectura y Conexión
- **Estado**: Completado
- **Fecha**: 2026-09-12
- **Decisiones**:
  - Se diseñó y redactó la arquitectura mínima desacoplada en `docs/architecture.md`:
    - *Backend*: Monolito modular en FastAPI + Uvicorn (1 worker asíncrono, ~45 MB RAM) con módulos `api`, `renfe`, `db`, `scheduler` y `notifications`.
    - *Persistencia*: SQLite con `PRAGMA journal_mode = WAL`, `PRAGMA busy_timeout = 5000`, migraciones versionadas y modelo con separación de estados (ciclo de vida, disponibilidad y aviso).
    - *Planificador*: Un único propietario asíncrono con agrupación de consultas redundantes y descarte de tareas atrasadas si una consulta tarda >30s.
    - *Android*: App nativa en Kotlin + Jetpack Compose (Material 3) en un único módulo estructurado por funcionalidades (`search`, `followups`, `settings`, `core`).
    - *Notificaciones*: Firebase Cloud Messaging (FCM) mediante `data payload` con canales diferenciados y acciones interactivas.
    - *Control de recursos*: Límite estricto del contenedor Docker a 256 MB de RAM y 0.8 CPU para preservar >650 MB libres en la VM e2-micro.
  - Se redactó el modelo de seguridad en `docs/security.md`:
    - Esquema de usuario único sin registro público.
    - Emparejamiento mediante código temporal (OTP) generado por CLI local en la VM y credencial criptográfica única (`device_token`) guardada en el Android Keystore.
    - Administración exclusivamente mediante CLI local en la VM (sin endpoints administrativos en la API).
    - *Conexión confirmada*: El usuario aprobó explícitamente la **Opción 1: Tailscale** (WireGuard P2P, puertos 100% cerrados a Internet, MagicDNS y HTTPS gratuito).
  - No se implementó código de la aplicación.
- **Archivos modificados / creados**:
  - `docs/architecture.md` (creado)
  - `docs/security.md` (creado)
  - `PROGRESS.md` (actualizado)
- **Pruebas ejecutadas y resultados**:
  - Verificación de consistencia del diseño arquitectónico, compatibilidad de cuotas (0,00 €) y huella de memoria (<100 MB).
- **Bloqueos**: Ninguno. Elección de transporte resuelta a favor de Tailscale por el usuario.
- **Siguiente paso**: Paso 08 (`prompts/08-plan-ci-cd.md`).

## Paso 08: Plan de CI/CD
- **Estado**: Completado
- **Fecha**: 2026-09-12
- **Decisiones**:
  - Se diseñó y redactó el plan exhaustivo de CI/CD en `docs/ci-cd.md` sin habilitar workflows ejecutables en el repositorio:
    - *CI*: Runners Linux estándar de GitHub (`ubuntu-latest`) con análisis estático (Ruff, mypy, Android Lint), pruebas unitarias con fixtures mockeados (sin conexión a Renfe ni producción) y compilación de APK de depuración con retención de 3 días.
    - *Filtro de rutas sin bloqueos*: Workflow orquestador unificado con detección de cambios (`paths-filter`) y job agregador (`All Checks OK`) para evitar checks obligatorios eternamente pendientes en PRs que solo modifican un subsistema.
    - *Seguridad*: Acciones fijadas por commit SHA completo inmutable, token `GITHUB_TOKEN` con permisos mínimos de solo lectura (`contents: read`), y cancelación automática de ejecuciones obsoletas (`cancel-in-progress: true`).
    - *CD Backend*: Activación manual por `workflow_dispatch`, serializado (`max-parallel: 1`), con verificación del commit exacto (debe pertenecer a `main` y haber superado CI).
    - *Conectividad VM*: Despliegue mediante túnel IAP (`gcloud compute ssh --tunnel-through-iap`) con autenticación OIDC/WIF de GCP (sin abrir puertos al mundo ni depender de IPs públicas).
    - *Protocolo transaccional*: Backup consistente atómico de SQLite (`.backup`), ejecución de migraciones, arranque de contenedor y health check con rollback automático en 30s.
    - *Distribución Android*: Generación y firma de APK Release con keystore protegido en secrets (destruido tras la firma), publicación en GitHub Releases privada con checksum SHA-256 e instalación manual en el móvil.
    - *Plan de contingencia*: Procedimiento de compilación y despliegue manual desde Windows en PowerShell si se agota la cuota gratuita de GitHub Actions.
    - Sin dependencias de aprobaciones de GitHub Environments (servicio de pago en repositorios privados).
  - No se crearon ni habilitaron workflows en `.github/workflows/`.
- **Archivos modificados / creados**:
  - `docs/ci-cd.md` (creado)
  - `PROGRESS.md` (actualizado)
- **Pruebas ejecutadas y resultados**:
  - Verificación de consistencia de la arquitectura de CI/CD y validación de compatibilidad con 0,00 € y 1 GB de RAM.
- **Bloqueos**: Ninguno.
- **Siguiente paso**: Esperar autorización e instrucciones del usuario para el paso 09 (`prompts/09-puerta-de-aprobacion.md`).

## Paso 09: Puerta de Aprobación
- **Estado**: Completado — Aprobación recibida
- **Fecha**: 2026-09-12
- **Decisiones**:
  - Se presentó el resumen completo de Auditoría/Licencia, Arquitectura, Conexión/Seguridad (Tailscale), Viabilidad 0 €, CI/CD y bloqueos pendientes.
  - Se actualizó `docs/requirements-checklist.md` con la sección de Verificación Formal de Coherencia confirmando que ninguna propuesta contradice la especificación original.
  - El usuario ha otorgado **aprobación explícita de la Arquitectura y el Plan de CI/CD** el 2026-09-12 a las 20:40 CEST.
  - Skills adicionales instaladas en `.agents/skills/` desde el repositorio `sickn33/agentic-awesome-skills`: `fastapi-pro`, `android-jetpack-compose-expert`, `kotlin-coroutines-expert`, `pytest-skill`, `docker-expert`. Directorio `.agents/` añadido a `.gitignore`.
- **Archivos modificados / creados**:
  - `docs/requirements-checklist.md` (actualizado con sección CONF-01 a CONF-09)
  - `.agents/skills/fastapi-pro/SKILL.md` (instalado)
  - `.agents/skills/fastapi-pro/resources/implementation-playbook.md` (instalado)
  - `.agents/skills/android-jetpack-compose-expert/SKILL.md` (instalado)
  - `.agents/skills/kotlin-coroutines-expert/SKILL.md` (instalado)
  - `.agents/skills/pytest-skill/SKILL.md` (instalado)
  - `.agents/skills/pytest-skill/reference/playbook.md` (instalado)
  - `.agents/skills/docker-expert/SKILL.md` (instalado)
  - `.gitignore` (actualizado con `.agents/`)
  - `PROGRESS.md` (actualizado)
- **Pruebas ejecutadas y resultados**:
  - Verificación de coherencia completa de los 9 puntos CONF contra `prompts/00-especificacion-original.md`: todos conformes.
- **Bloqueos**: Ninguno. Aprobación recibida.
- **Siguiente paso**: Paso 10 (`prompts/10-base-backend.md`) — Crear backend mínimo.

## Paso 10: Base de Backend
- **Estado**: Completado
- **Fecha**: 2026-09-12
- **Decisiones**:
  - Se estableció un paquete Python mínimo en `backend/` con FastAPI, Uvicorn y configuración mediante Pydantic Settings; las integraciones de Renfe, FCM, SQLite, navegador y planificador siguen fuera de esta base.
  - La configuración usa exclusivamente variables con prefijo `RENFE_NOTIFIER_`; en producción exige `RENFE_NOTIFIER_SECRET_KEY` de al menos 32 caracteres, sin valores secretos por defecto ni en el repositorio.
  - El ciclo de vida ASGI solo registra arranque y cierre. El único endpoint es `GET /health`, que devuelve exclusivamente `status` y `uptime_s`.
  - Se incorporaron Ruff, mypy y pytest, más `requirements.lock` con las versiones resueltas localmente para reproducir la instalación en Python 3.11.
- **Archivos modificados / creados**:
  - `backend/pyproject.toml`
  - `backend/requirements.lock`
  - `backend/.env.example`
  - `backend/app/__init__.py`
  - `backend/app/config.py`
  - `backend/app/main.py`
  - `backend/app/api/__init__.py`
  - `backend/app/api/health.py`
  - `backend/tests/test_config.py`
  - `backend/tests/test_health.py`
  - `PROGRESS.md`
- **Pruebas ejecutadas y resultados**:
  - Entorno local creado con Python 3.11.9 e instalación editable de las dependencias resueltas.
  - `ruff check .`: éxito, sin incidencias.
  - `python -m mypy app tests`: éxito, sin incidencias en 7 archivos.
  - `python -m pytest`: éxito, 3 pruebas superadas. La librería instalada emitió 2 advertencias de deprecación de Starlette/AnyIO, sin fallos.
  - Prueba de ciclo de vida ASGI y arranque con `TestClient`: éxito; se observó arranque, `GET /health` con HTTP 200 y cierre limpio. No se validó en dispositivo real ni en VM.
- **Bloqueos**: Ninguno para el paso 10. Permanece el bloqueo preventivo de despliegue de la VM anotado en el paso 06.
- **Siguiente paso**: Esperar instrucción explícita para el paso 11 (`prompts/11-estaciones-y-parser.md`).

## Paso 11: Estaciones y Parser DWR
- **Estado**: Completado
- **Fecha**: 2026-09-12
- **Decisiones**:
  - Se incorporó el catálogo de 1.347 estaciones desde `Pablovb019/renfe-notifier-bot` únicamente mediante lectura del fichero autorizado por la auditoría. Se documentó su procedencia y SHA-256 (`eab08be5a308204f656e5b94f54d0b557efe3cc98f2c4760f1eb26f9dc96850f`) en `backend/app/renfe/ATTRIBUTION.md`.
  - Se implementó normalización NFKD, resolución de nombres oficiales y alias comunes, además de grupos con sufijo `(TODAS)` que devuelven exclusivamente estaciones concretas de la ciudad.
  - Se implementó un parser DWR aislado, sin cliente HTTP: extrae el callback comprobado, procesa `listadoTrenes`, genera identificadores compuestos de servicio/salida/llegada y conserva los horarios; el precio ausente se representa como desconocido (`None`).
  - Plaza H solo se preserva como contexto explícito de la consulta. No se deduce ninguna regla de disponibilidad para Plaza H, ya que esa semántica no está comprobada.
  - Una respuesta DWR inválida provoca `DwrParseError`; una lista válida sin plazas se devuelve como `no_availability`. La fixture de DWR está marcada explícitamente como sintética y no se presenta como evidencia de Renfe.
- **Archivos modificados / creados**:
  - `backend/pyproject.toml`
  - `backend/requirements.lock`
  - `backend/app/renfe/__init__.py`
  - `backend/app/renfe/ATTRIBUTION.md`
  - `backend/app/renfe/data/stations.json`
  - `backend/app/renfe/stations.py`
  - `backend/app/renfe/parser.py`
  - `backend/tests/fixtures/dwr_train_list_synthetic.txt`
  - `backend/tests/test_stations.py`
  - `backend/tests/test_dwr_parser.py`
  - `PROGRESS.md`
- **Pruebas ejecutadas y resultados**:
  - Verificación local del catálogo: JSON válido, 1.347 estaciones y SHA-256 calculado igual al registrado.
  - `ruff check .`: éxito, sin incidencias.
  - `python -m mypy app tests`: éxito, sin incidencias en 12 archivos.
  - `python -m pytest`: éxito, 8 pruebas superadas (normalización, alias, grupos, parser DWR, precio desconocido, Plaza H, ausencia de plazas y respuesta inválida). Persisten 2 advertencias de deprecación de dependencias de Starlette/AnyIO, sin fallos.
  - No se realizaron consultas reales contra Renfe, ni se validó en dispositivo real o VM.
- **Bloqueos**: Ninguno para el paso 11. La semántica real de Plaza H continúa deliberadamente sin inferir hasta disponer de evidencia autorizada.
- **Siguiente paso**: Esperar instrucción explícita para el paso 12 (`prompts/12-cliente-http-renfe.md`).

## Paso 12: Cliente HTTP/DWR de Renfe
- **Estado**: Completado
- **Fecha**: 2026-09-12
- **Decisiones**:
  - Se implementó `RenfeDwrClient` asíncrono con una sesión `httpx.AsyncClient` efímera por búsqueda. Cada sesión se cierra mediante `async with`, tanto en éxito como ante errores, y no comparte cookies ni estado entre búsquedas.
  - Se conservó la secuencia auditada de cinco POST: búsqueda, dos llamadas a `__System.generateId`, actualización de sesión y listado de trenes. No se inició Selenium ni se añadieron mecanismos de evasión de CAPTCHA o bloqueos.
  - Se establecieron timeout por petición (10 s por defecto) y presupuesto total (30 s por defecto). El cliente aplica reintentos acotados con backoff y jitter para fallos de transporte, 429 y 502/503/504; `Retry-After` se respeta como mínimo. Un 403 detiene el flujo de inmediato sin reintento.
  - Las métricas por búsqueda exponen número de peticiones, duración, bytes recibidos, fase y estado HTTP, sin registrar cuerpos, cookies ni tokens. `httpx` pasó a dependencia de ejecución del backend.
- **Archivos modificados / creados**:
  - `backend/pyproject.toml`
  - `backend/app/renfe/client.py`
  - `backend/tests/test_renfe_client.py`
  - `PROGRESS.md`
- **Pruebas ejecutadas y resultados**:
  - `ruff check .`: éxito, sin incidencias.
  - `python -m mypy app tests`: éxito, sin incidencias en 14 archivos.
  - `python -m pytest`: éxito, 12 pruebas superadas. El transporte simulado verificó la secuencia de cinco POST, métricas, 429 con `Retry-After`, parada ante 403, cierre del transporte y presupuesto total. Persisten 2 advertencias de deprecación de dependencias de Starlette/AnyIO, sin fallos.
  - No se realizaron conexiones ni consultas reales a Renfe; el flujo real no se ha validado en VM o dispositivo.
- **Bloqueos**: Ninguno para el paso 12. La compatibilidad del protocolo con Renfe en vivo requiere una autorización específica y controlada antes de validarse.
- **Siguiente paso**: Esperar instrucción explícita para el paso 13 (`prompts/13-modelo-seguimientos.md`).

## Paso 13: Modelo de seguimientos
- **Estado**: Completado (registro completo en `prompts/PROGRESS.md`)
- **Fecha**: 2026-09-13
- **Decisiones**:
  - Modelo de dominio puro en `backend/app/followups/domain.py` sin dependencias del planificador.
  - Enums `Lifecycle`, `AvailabilityState`, `AlertState`, `FollowUpMode`, `ObservationKind` y clases `TrainSnapshot`, `Observation`, `FollowUp`, `ObservationResult`.
  - Reglas: nueva aparición válida de plazas crea episodio; errores y datos obsoletos no; confirmación sin borrar; pausa/renovación/eliminación como transiciones independientes; caducidad inicial de 30 días limitada por la salida (o fin de día local para ALL); primero/último/todos; nuevos trenes dentro de ALL; medianoche y cambio horario con fechas Madrid; ID real preferido y fallback compuesto documentado.
  - UTC para instantes y Europe/Madrid para fechas de viaje.
- **Archivos modificados / creados**:
  - `backend/app/followups/domain.py`
  - `backend/tests/test_followup_domain.py`
  - `prompts/PROGRESS.md`
- **Pruebas ejecutadas y resultados**:
  - `pytest`: 22 tests superados (10 de dominio + 12 previos).
  - `ruff check .`: éxito sin incidencias.
  - `python -m mypy app tests`: éxito.
- **Bloqueos**: Ninguno.
- **Siguiente paso**: Paso 14 (`prompts/14-sqlite-y-migraciones.md`).

## Paso 14: SQLite y Migraciones
- **Estado**: Completado
- **Fecha**: 2026-09-13
- **Decisiones**:
  - Capa `backend/app/db/` (conexión, migraciones y backup) según la arquitectura aprobada: WAL, `busy_timeout=5000`, `foreign_keys=ON` y conexión efímera por operación con cierre garantizado.
  - Migraciones versionadas atómicas por versión (`BEGIN/COMMIT` dentro de `executescript` con registro en `schema_migrations` en la misma transacción).
  - `backup_database()` mediante la API online backup de SQLite (copia consistente, no copia ciega del `.db`).
  - `FollowUpRepository` en `backend/app/followups/database.py` (reemplaza el placeholder), con transacciones cortas y sin transacciones abiertas durante consultas de red.
  - Tablas: `followups`, `episodes` (UNIQUE por followup+episode, FK con ON DELETE CASCADE), `app_config` (KV) y `schema_migrations`.
  - Serialización: enums por valor textual, instantes como epoch UTC entero, fechas de viaje `YYYY-MM-DD`, `seen_available_train_ids` como JSON ordenado.
  - Docker fuera de directorios reemplazables: `database_path` en `Settings` con prefijo `RENFE_NOTIFIER_`, default `data/renfe-notifier.db` fuera del paquete `app/`.
  - No se importaron seguimientos del bot.
- **Archivos modificados / creados**:
  - `backend/app/db/__init__.py` (creado)
  - `backend/app/db/connection.py` (creado)
  - `backend/app/db/migrations.py` (creado)
  - `backend/app/followups/database.py` (reemplazado placeholder por repositorio completo)
  - `backend/app/config.py` (añadido `database_path`)
  - `backend/.env.example` (documentada variable de ruta de DB)
  - `backend/tests/test_followup_database.py` (creado, 11 tests)
  - `prompts/PROGRESS.md` (actualizado)
- **Pruebas ejecutadas y resultados**:
  - `ruff check .`: All checks passed.
  - `python -m mypy app tests`: no issues found in 22 source files.
  - `python -m pytest`: 33 superados (22 anteriores + 11 de persistencia): creación WAL/pragmas, migración idempotente, roundtrip create/save/get, reinicio, episodios y UNIQUE, filtro por lifecycle y purga, config KV, integridad ante duplicados, backup consistente e independiente, `FileNotFoundError`, recuperación tras eliminación de ficheros.
  - No se realizaron pruebas reales contra Renfe ni en dispositivo/VM.
- **Bloqueos**: Ninguno para el paso 14. Permanece el bloqueo preventivo de despliegue en VM del paso 06.
- **Siguiente paso**: Paso 15 (`prompts/15-planificador-y-agrupacion.md`).

## Paso 15: Planificador y Agrupación
- **Estado**: Completado
- **Fecha**: 2026-09-13
- **Decisiones**:
  - Creado paquete `backend/app/scheduler/` con separación estricta entre lógica pura (`plan.py`: `GroupKey`, `build_plan`, `train_list_to_observation`, `error_observation`) y ciclo asíncrono con IO (`service.py`: `SchedulerService`).
  - `GroupKey(origin_code, destination_code, travel_date, plaza_h)` como clave de agrupación; cada grupo aparece exactamente una vez por ciclo.
  - `plaza_h: bool` incorporado a `FollowUp` en el dominio, a la BD (migración v2 con `ALTER TABLE` + índice) y al repositorio; la migración v1 convergió con la v2 sin intervención manual.
  - `SchedulerService` con cliente y reloj inyectables, semáforo de concurrencia (por defecto 2), rehidratación desde SQLite en cada tick, y persistencia optimizada (solo cuando `outcome.followup is not followup`).
  - `StationCatalog.by_code()` añadido para resolver códigos de estación en el planificador.
  - Optimización de guardado: solo persiste cuando el followup cambia; los nuevos episodios se registran en tabla separada.
  - Búsquedas manuales equivalentes: código reutilizado (`build_plan`, mapeo de observaciones), resultados HTTP no compartidos.
- **Archivos modificados / creados**:
  - `backend/app/scheduler/__init__.py` (creado)
  - `backend/app/scheduler/plan.py` (creado)
  - `backend/app/scheduler/service.py` (creado)
  - `backend/app/followups/domain.py` (añadido `plaza_h`)
  - `backend/app/db/migrations.py` (migración v2)
  - `backend/app/followups/database.py` (serialización de `plaza_h`)
  - `backend/app/renfe/stations.py` (`by_code`)
  - `backend/tests/test_scheduler_plan.py` (creado)
  - `backend/tests/test_scheduler_service.py` (creado)
  - `backend/tests/test_stations.py` (tests de `by_code`)
  - `backend/tests/test_followup_database.py` (roundtrip `plaza_h`, versiones [1,2])
  - `prompts/PROGRESS.md` (actualizado)
- **Pruebas ejecutadas y resultados**:
  - `ruff check .`: All checks passed.
  - `python -m mypy app tests`: no issues found in 27 source files.
  - `python -m pytest`: 50 superados (33 anteriores + 17 nuevos): agrupación compatible y separación por plaza_h/ruta; vacío; mapeo TrainList→Observation; error_observation; sin seguimientos activos no busca; happy path persiste AVAILABLE/PENDING/ep1; RenfeClientError mantiene followup intacto; estación desconocida descarta grupo; concurrencia=1 procesa ambos grupos; sin episodio duplicado mientras sigue disponible; reaparición crea episodio 2; run_forever 3 ticks; roundtrip plaza_h; by_code existente y None; versiones migración [1,2].
  - No se realizaron pruebas reales contra Renfe ni en dispositivo/VM.
- **Bloqueos**: Ninguno para el paso 15. El bloqueo preventivo de despliegue en VM del paso 06 permanece.
- **Siguiente paso**: Paso 16 (`prompts/16-recordatorios-y-cola-avisos.md`).

## Paso 16: Recordatorios y cola de avisos
- **Estado**: Completado
- **Decisiones**:
  - Creado paquete `backend/app/reminders/` con dominio puro (`domain.py`) y repositorio de cola SQLite (`queue.py`); cola persistente directamente en SQLite sin Redis ni Celery.
  - Nueva migración v3 con la tabla `alert_events`: `event_id` (PK, idempotencia), `followup_id`, `episode_id` (FK al episodio concreto), `observed_at`, `expires_at` (caducidad), `created_at`, `remind_at`, `status`, `attempts`/`max_attempts`, `delivered_at`, `cancelled_reason`.
  - Estados persistentes: `pending`, `suspended`, `cancelled`, `delivered`, `failed`.
  - Recordatorios configurables y acotados (`RENFE_NOTIFIER_REMINDER_INITIAL_DELAY_S/INTERVAL_S/MAX_ATTEMPTS` en `Settings` y `.env.example`); retraso inicial por evento, intervalo inyectado en `AlertQueue`, tope de intentos por evento.
  - Cancelación al confirmar/pausar/vencer/desaparecer plazas (`cancel_for_followup(reason)`, razones `acknowledged`/`paused`/`expired`/`unavailable`); suspensión por obsolescencia (`suspend_for_followup()`, reanulable con `cancelled_reason=stale`).
  - Al encolar un episodio más reciente se invalida el aviso previo del mismo seguimiento (`replaced`): un único aviso activo por seguimiento.
  - Entrega *al menos una vez* con idempotencia (`enqueue` no-op sobre `event_id` duplicado; `deliver`/`mark_failed` no-ops sobre estados no pendientes) y reintentos acotados; **no se promete entrega exactamente una vez** (documentado).
  - Recuperación tras reinicio: estado íntegro en SQLite; `add_episode` devuelve ahora la fila `Episode` con `episode_id` para ligar eventos a episodios.
  - Sin envío FCM ni salidas de red en este paso.
- **Archivos modificados / creados**:
  - `backend/app/reminders/__init__.py`, `backend/app/reminders/domain.py`, `backend/app/reminders/queue.py` (creados)
  - `backend/app/db/migrations.py` (migración v3)
  - `backend/app/followups/database.py` (`Episode` con `episode_id`; `add_episode` y `list_episodes` lo devuelven)
  - `backend/app/config.py` y `backend/.env.example` (opciones de recordatorios)
  - `backend/tests/test_reminders_domain.py` (7 tests), `backend/tests/test_reminders_queue.py` (17 tests) (creados)
  - `backend/tests/test_followup_database.py` (versiones `[1,2,3]`, `episode_id` de `add_episode`)
  - `prompts/PROGRESS.md` (actualizado)
- **Pruebas ejecutadas y resultados**:
  - `ruff check .`: All checks passed.
  - `python -m mypy app tests`: no issues found in 32 source files.
  - `python -m pytest`: 74 superados (50 anteriores + 24 nuevos). Cubren: roundtrip encolado; duplicado idempotente; reemplazo de avisos previos; conservación por seguimiento; retraso inicial; reintentos acotados con entrega final; no-ops (desconocido/delivered); errores (`mark_failed` una vez); cancelaciones acknowledged/paused/expired/unavailable; suspensión y cancelación por stale; recuperación tras reinicio; eventos ligados a episodios persistidos; primer recordatorio, UTC estricto y epoch roundtrip en el dominio.
  - No se realizaron pruebas reales contra Renfe, envíos de FCM ni validaciones en dispositivo/VM.
- **Bloqueos**: Ninguno para el paso 16. El bloqueo preventivo de despliegue en VM del paso 06 permanece.
- **Siguiente paso**: Paso 17 (`prompts/17-emparejamiento-y-seguridad.md`).

## Paso 17: Emparejamiento y seguridad
- **Estado**: Completado (registro completo en `prompts/PROGRESS.md`)
- **Decisiones**:
  - Mecanismo aprobado: CLI local genera código OTP temporal `RF-XXXXXX`; la app lo reclama con `POST /api/v1/pairing/claim` y recibe `device_token` (`secrets.token_urlsafe(48)`). La BD guarda **solo hashes SHA-256** de códigos y tokens.
  - Migración v4: tablas `pairing_codes` y `devices` (`device_id` PK UNIQUE y `token_hash` UNIQUE).
  - `PairingError` unifica motivos inválido/caducado/agotado/usado → 401 genérico único sin revelar causa.
  - Re-claim del mismo `device_id` revoca el token previo (`INSERT OR REPLACE`): un único dispositivo por identificador.
  - Límites: `can_try()`/`max_attempts` en el dominio + rate limit por IP en memoria (ventana deslizante, por defecto 10 req / 60 s en `/claim`).
  - `GET /api/v1/pairing/devices` protegida con `Authorization: Bearer` (verificación por SHA-256); sin token o revocado → 401.
  - Logs sanitizados (`SensitiveFormatter`/`redact()`): enmascaran Bearer, `RF-XXXXXX`, tokens largos y cookies.
  - Dependencias FastAPI con patrón `Annotated[Type, Depends(...)]` (ruff B008); clock inyectable en `PairingService` para tests deterministas.
- **Archivos modificados / creados**:
  - `backend/app/pairing/` (`domain.py`, `database.py`, `service.py`, `__init__.py`) — creados
  - `backend/app/api/deps.py`, `backend/app/api/pairing.py` — creados
  - `backend/app/middleware/rate_limit.py`, `backend/app/middleware/logging.py` — creados
  - `backend/app/cli.py` — creado (CLI local: `pairing-code`, `devices`)
  - `backend/app/config.py`, `backend/.env.example` — opciones `pairing_*` y `rate_limit_*`
  - `backend/app/db/migrations.py` — migración v4
  - `backend/app/main.py` — lifespan con `PairingService`, routers y rate limit
  - `backend/tests/test_pairing_domain.py` (7), `test_pairing_database.py` (8), `test_pairing_service.py` (9), `test_pairing_api.py` (9) — creados
  - `backend/tests/test_followup_database.py` — versiones de esquema `[1,2,3,4]`
  - `prompts/PROGRESS.md` (actualizado)
- **Pruebas ejecutadas y resultados**:
  - `ruff check .`: All checks passed.
  - `python -m mypy app tests`: no issues found in 46 source files.
  - `python -m pytest`: 107 superados (74 anteriores + 33 nuevos). Cubren: generación/expiración/agotamiento/revocación en dominio; migración [1,2,3,4], roundtrip, purga de caducados, solo hash en BD; claim válido e inválido (PairingError), caducado (reloj falso), reuso, re-claim revoca previo, revoke/revoke-all, token no emparejado; API: claim 200/401/422, acceso sin token 401, token revocado 401, rate limit 429, redacción de logs, el código OTP no se devuelve en la respuesta.
  - No se realizaron pruebas reales contra Renfe, envíos de FCM ni validaciones en dispositivo/VM.
- **Bloqueos**: Ninguno para el paso 17. El bloqueo preventivo de despliegue en VM del paso 06 permanece.
- **Siguiente paso**: Paso 18 (según `prompts/`).

## Paso 18: API funcional (estaciones, b�squedas, seguimientos, FCM, diagn�stico)
- **Estado**: Completado (registro completo en prompts/PROGRESS.md)
- **Decisiones**:
  - **Anti-SSRF**: las b�squedas solo aceptan c�digos de estaci�n validados contra el cat�logo local (StationCatalog.by_code); ning�n campo admite URLs del cliente (cumple docs/security.md).
  - pp/renfe/search.py (TrainSearchEngine) como l�gica pura para evitar import circular con deps.py.
  - pp/api/search.py: GET /api/v1/search/stations?q= y POST /api/v1/search/trains (fecha en [hoy, hoy+62] Europe/Madrid, origen?destino, TrainOut con identity del dominio 
eal:...; estaci�n desconocida ? 400, RenfeClientError ? 503).
  - pp/api/followups.py: CRUD + pause/resume/renew/acknowledge/delete bajo /api/v1/followups. ollowup_id generado en servidor; mode specific exige specific_train_id; 404 si no existe y 409 si el estado no lo permite; delete l�gico idempotente; pause/delete/acknowledge cancelan recordatorios (queue.cancel_for_followup). Confirmar no borra el seguimiento (REQ-07.4).
  - pp/api/fcm.py: PUT /api/v1/fcm/token registra/renueva el token FCM del dispositivo.
  - pp/api/diagnostics.py: GET /api/v1/diagnostics/health (p�blico), GET /api/v1/diagnostics (contadores) y POST /api/v1/diagnostics/test-notification (emisor inyectado; 503 sin emisor, 409 sin token FCM).
  - pp/notifications/base.py: protocolo NotificationSender.send_test (FCM real en paso 19).
  - Migraci�n v5 (devices.fcm_token), PairingRepository.update_fcm_token/count_devices, PairingService.register_fcm_token, FollowUpRepository.count(lifecycle)/count_episodes.
  - Lifespan de main.py expone repositorio/cola/cat�logo/engine en pp.state; 
edact() enmascara tokens FCM largos (ahora con :).
- **Archivos modificados / creados**:
  - Creados: ackend/app/renfe/search.py, ackend/app/api/search.py, ollowups.py, cm.py, diagnostics.py, ackend/app/notifications/, ackend/tests/helpers.py, ackend/tests/test_search_api.py, 	est_followups_api.py, 	est_fcm_api.py, 	est_diagnostics_api.py.
  - Modificados: pp/api/deps.py, pp/main.py, pp/__init__.py (APP_VERSION), pp/db/migrations.py (v5), pp/pairing/{domain,database,service}.py, pp/followups/database.py, pp/middleware/logging.py, 	ests/__init__.py, aserciones de esquema [1,2,3,4,5].
- **Pruebas ejecutadas y resultados**:
  - 
uff check .: All checks passed.
  - python -m mypy app tests: no issues found in 59 source files.
  - python -m pytest: 134 superados (107 anteriores + 27 nuevos). Cubren: estaciones/b�squedas con MockTransport (auth, validaciones de fecha/estaciones, TrainOut tipado); CRUD y acciones de seguimientos (id servidor, validaciones de mode, estados/409, acknowledge sin borrar, delete idempotente); FCM (auth, registro/renovaci�n, 422); diagn�stico (health p�blico, contadores, test-notification 401/503/409/200 con FakeSender) y fcm_token/count en repositorio.
  - No se realizaron pruebas reales contra Renfe, env�os FCM ni validaciones en dispositivo/VM.
- **Bloqueos**: Ninguno para el paso 18. El bloqueo preventivo de despliegue en VM del paso 06 permanece.
- **Siguiente paso**: Paso 19 (seg�n prompts/).

## Paso 19: Transporte FCM
- **Estado**: Completado (registro completo en `prompts/PROGRESS.md`)
- **Decisiones**:
  - Transporte FCM por **HTTP v1** (`https://fcm.googleapis.com/v1/projects/{project_id}/messages:send`) sin Admin SDK ni Cloud Functions; cero dependencias nuevas de producción.
  - Autenticacion mediante ADC: en la VM la **identidad de la instancia** (metadatos GCE, permisos minimos) y en local `google-auth` opcional (`GOOGLE_APPLICATION_CREDENTIALS`); ninguna credencial se lee en el contexto del modelo ni viaja por la API. `AutoTokenProvider` (ADC -> metadatos GCE -> 503).
  - **Payload data-only** segun REQ-08.5 (sin bloque `notification`): la app muestra la notificacion nativa sin consulta adicional. Contrato tipado en `AlertMessage`/`FcmMessage` (claves validas, valores <=1024 y total <=4096 bytes antes de enviar).
  - `high` solo para alertas y test (visibles); TTL corto por defecto (300 s) como declaracion con descarte por `expires_at`; `collapse_key="followup:{followup_id}"`.
  - Reintentos y deduplicacion delegados en la cola existente (`AlertQueue` + `event_id`); el transporte es idempotente y no reintenta. Separacion **aceptado por FCM vs mostrado** (`FcmResult`; el cliente confirma via episodio acknowledged).
  - Errores: 404/`UNREGISTERED`/"registration token" -> `token_invalid` (permite invalidar el token); 401/403/400 -> no retryable; 429/5xx -> retryable; sin credenciales -> `FcmNotConfiguredError`. Diagnostico: token invalido -> **410**, no configurado -> **503**, otros -> 502.
  - Invalidacion de tokens: `PairingRepository.clear_fcm_token`/`get_device_by_fcm_token` y `PairingService.clear_fcm_token`/`invalidate_fcm_token` (sin revocar el emparejamiento).
  - Settings nuevas: `fcm_project_id`, `fcm_timeout_s`, `fcm_default_ttl_s`, `fcm_app_package` (documentadas en `.env.example`). El sender se construye en `main.py` solo si `RENFE_NOTIFIER_FCM_PROJECT_ID` esta definido.
  - `docs/notificaciones-fcm.md` con el contrato. Anadida dependencia `tzdata` (solo Windows) requerida por `zoneinfo` en el entorno local.
- **Archivos modificados / creados**:
  - Creados: `backend/app/notifications/fcm.py`, `backend/tests/test_fcm_transport.py`, `backend/tests/test_fcm_providers.py`, `docs/notificaciones-fcm.md`.
  - Modificados: `backend/app/api/diagnostics.py` (410/503), `backend/app/main.py` (wiring condicionado), `backend/app/config.py` y `backend/.env.example` (`fcm_*`), `backend/app/pairing/database.py` y `service.py` (invalidacion), `backend/pyproject.toml` (`tzdata`), `backend/tests/test_diagnostics_api.py`, `test_pairing_database.py`, `test_pairing_service.py`, `prompts/PROGRESS.md`, `PROGRESS.md`.
- **Pruebas ejecutadas y resultados**:
  - `ruff check .` -> **All checks passed**.
  - `python -m mypy app tests` -> **no issues found in 62 source files**.
  - `python -m pytest` -> **163 superados** (134 anteriores + 29 nuevos). Cubren: transporte con `MockTransport` (cuerpo completo, 200+name, 404/UNREGISTERED->token_invalid, 401/400 no retryable, 429/500/503 retryable, defaults, validacion de payload, contratos de `build_test_alert`/`AlertMessage`, `send_test`); proveedores (metadatos GCE OK/fallos, AutoTokenProvider con y sin google-auth); API (410 y 503 en test-notification) e invalidacion de tokens (repositorio y servicio).
  - **No se enviaron mensajes FCM reales, no se crearon recursos ni se toco la VM.** La validacion con Firebase real queda pendiente (pasos 33-35).
- **Bloqueos**: Ninguno nuevo. Continua el bloqueo preventivo de despliegue en VM del paso 06. Configuracion real de FCM (proyecto Spark, permisos de la VM, `google-auth` local) documentada y pendiente del paso 33.
- **Siguiente paso**: No avanzar. Siguiente archivo `prompts/20-base-android.md` en cuanto el usuario lo indique.

## Paso 20: Base Android
- **Estado**: Completado (APK debug compilado; registro completo en `prompts/PROGRESS.md`)
- **Decisiones**:
  - Proyecto en `android/`, modulo unico `:app`, paquete `com.pablovb019.renfenotifier`, organizado por funcionalidades (`feature/home`; `search`, `followups`, `settings` y `common` en pasos posteriores, sin paquetes vacios).
  - Stack estable: Gradle 8.9, AGP 8.5.2, Kotlin 2.0.21 (plugin compose), Compose BOM 2024.12.01, material3, activity-compose 1.9.3, lifecycle 2.8.7, navigation-compose 2.8.5, core-ktx 1.13.1 (1.15.0 exige compileSdk 35). JDK 17 + jvmTarget 17. Version catalog en `gradle/libs.versions.toml`.
  - SDKs: compileSdk 34, targetSdk 34 (no target antiguo), minSdk 26 (realme GT Neo 2 = Android 13).
  - Activity unica con `enableEdgeToEdge` + `setContent`; `AppNavHost` (NavHost con destino unico `home`).
  - Tema claro/oscuro: `RenfeNotifierTheme` con color dinamico (Android 12+) y paleta fija M3 (azul Renfe) de respaldo.
  - `HomeViewModel` con `StateFlow` inmutable + coroutines/Flow (bucle horario en `viewModelScope`); `HomeContent` sin estado; `collectAsStateWithLifecycle`.
  - Recursos en espanol (default), accesibilidad basica (`contentDescription`, RTL).
  - Toolchain 0 EUR en Windows sin Android Studio: JDK 17 Temurin + Gradle 8.9 + cmdline-tools + plataforma 34 + build-tools 34.0.0 en `%LOCALAPPDATA%`; wrapper de Gradle versionado. `local.properties` gitignored.
  - `docs/android-build.md` documenta la compilacion local en Windows (paso a paso verificado).
- **Archivos modificados / creados**:
  - `android/settings.gradle.kts`, `android/build.gradle.kts`, `android/gradle.properties`, `android/gradle/libs.versions.toml`, `android/gradlew(.bat)`, `android/gradle/wrapper/*`, `android/local.properties` (gitignored)
  - `android/app/build.gradle.kts`, `android/app/proguard-rules.pro`, `android/app/src/main/AndroidManifest.xml`
  - `android/app/src/main/res/values/{themes,strings}.xml` (espanol)
  - `android/app/src/main/java/com/pablovb019/renfenotifier/MainActivity.kt`, `navigation/AppNavHost.kt`, `ui/theme/{Theme,Type}.kt`, `feature/home/{HomeUiState,HomeViewModel,HomeScreen}.kt`
  - `docs/android-build.md`
  - `prompts/PROGRESS.md`, `PROGRESS.md`
- **Pruebas ejecutadas y resultados**:
  - Toolchain provisionado: `sdkmanager --version` -> 12.0; plataforma android-34 y build-tools 34.0.0 instaladas.
  - `gradle wrapper --gradle-version 8.9 --no-daemon` -> BUILD SUCCESSFUL (1m39s).
  - `.\gradlew.bat :app:assembleDebug --no-daemon` -> **BUILD SUCCESSFUL in 43s**. Correcciones durante el intento: core-ktx 1.13.1 (la 1.15.0 exige compileSdk 35) y `stringResource` fuera del lambda `semantics`.
  - APK verificado: `android/app/build/outputs/apk/debug/app-debug.apk` (9.844.466 bytes); `output-metadata.json`: applicationId `com.pablovb019.renfenotifier`, versionName 0.1.0, versionCode 1, minSdk 26, variante debug.
  - **No se instalo ni validó en dispositivo real** (requiere aprobacion; el APK esta disponible para el realme GT Neo 2).
- **Bloqueos**: Ninguno para el paso 20. Sigue el bloqueo preventivo de despliegue en VM del paso 06 y la validacion en dispositivo real pendiente de aprobacion.
- **Siguiente paso**: No avanzar. Siguiente archivo `prompts/21-cliente-android-y-emparejamiento.md` en cuanto el usuario lo indique.

## Paso 21: Cliente Android y emparejamiento
- **Estado**: Completado (12 tests unitarios OK, lint OK, APK debug compilado; registro completo en `prompts/PROGRESS.md`). El usuario autorizo ejecutar el paso 21 antes que el 22 (que lo necesita).
- **Decisiones**:
  - Red: Retrofit 2.11.0 + OkHttp 4.12.0 + converter-gson + Datastore 1.1.1; Gson con LOWER_CASE_WITH_UNDERSCORES (snake_case del backend <-> camelCase). Contratos espejo de los Pydantic (health, pairing, search, followups) en `RenfeApi`.
  - TLS normal (nunca lenient): solo HTTPS salvo hosts de bucle local (`localhost`/`127.0.0.1`/`10.0.2.2`); `network_security_config.xml` con cleartextTrafficPermitted=false por defecto; INTERNET en manifest; `BACKEND_URL` en BuildConfig.
  - Emparejamiento: `feature/pairing` (campo OTP + estados carga/error). El token solo se guarda en Keystore cuando el backend responde 200 (nunca se marca exito una operacion remota fallida).
  - Credenciales: `KeystoreTokenVault` AES/GCM con clave no exportable en AndroidKeyStore; IV+blob Base64 en SharedPreferences; interfaz `TokenVault` (+ `InMemoryTokenVault` para tests).
  - DataStore para preferencias no secretas (`PreferencesRepository` : `SessionStore`; `isPaired`, `setPaired`, `clearAll`, `backendUrl`).
  - Errores: `AuthInterceptor` anade Bearer solo a endpoints protegidos; ante 401 limpia token y emite `credentialsRevoked` -> HomeVM limpia sesion y muestra CTA de emparejamiento. Mapas 401/429/5xx/red en UI.
  - Navegacion HOME <-> PAIRING; ViewModel con dependencias inyectadas (probable) via `viewModel { }` + `CreationExtras`.
  - Fix lint `NewApi` en `Theme.kt` (color dinamico requiere API 31; bug latente del paso 20).
- **Archivos modificados / creados**: core/network/model/ApiModels.kt, core/network/{RenfeApi,ApiModule,AuthInterceptor}.kt, core/security/{TokenVault,PreferencesRepository}.kt, feature/pairing/{PairingViewModel,PairingScreen}.kt, res/xml/network_security_config.xml, MainActivity.kt, AppNavHost.kt, feature/home/{HomeUiState,HomeViewModel,HomeScreen}.kt, strings.xml, AndroidManifest.xml, libs.versions.toml, app/build.gradle.kts, ui/theme/Theme.kt, + 4 tests.
- **Pruebas ejecutadas y resultados**:
  - `testDebugUnitTest` -> BUILD SUCCESSFUL; **12 tests / 0 fallos** (ApiContractTest con MockWebServer: health, claim, stations; AuthInterceptorTest: Bearer/omitido/401; PairingViewModelTest: exito, codigo vacio, 401, 429, red, sanitizacion).
  - `assembleDebug` -> BUILD SUCCESSFUL; `app-debug.apk` (11.054.386 bytes), versionName 0.1.0.
  - `lintDebug` -> BUILD SUCCESSFUL tras fix `NewApi` en Theme.kt (apartado del paso 20, corregido ahora).
  - **No validado en dispositivo real** (pendiente de aprobacion; requiere backend local `uvicorn` o desplegado para probar emparejamiento real).
- **Bloqueos**: Ninguno para el paso 21. Pendientes de aprobacion externa: despliegue en VM (paso 06), validacion en realme GT Neo 2.
- **Siguiente paso**: No avanzar. Siguiente archivo `prompts/22-pantalla-busqueda.md`.

## Paso 22: Pantalla de busqueda de viajes
- **Estado**: Completado (26 tests unitarios OK, lint OK, APK debug compilado; registro completo en `prompts/PROGRESS.md`).
- **Decisiones**:
  - Estaciones con alias/tildes/grupos resueltas por el backend (`GET /api/v1/search/stations`); el cliente las muestra y distingue los grupos.
  - Debounce 300 ms en sugerencias de estaciones (cancela Job previo; consulta vacia resuelve sin red).
  - Validacion cliente: estaciones seleccionadas de la lista (codigo), origen != destino, fecha >= hoy y <= hoy+62 dias.
  - Ausencia de plazas NO es error: `trains` vacio -> estado vacio "No hay trenes para esta ruta y fecha"; `searchError` solo ante fallos de red/servidor (503/5xx/IOException). Nunca confundir error con ausencia.
  - Precio desconocido representado como tal (`train.price == null` -> "No disponible", nunca 0 euros).
  - Modos de seguimiento mapeados al backend: `FollowUpMode` FIRST/LAST/ALL/SPECIFIC (valor serial `apiValue`); SPECIFIC requiere seleccion de un tren.
  - Crear seguimiento reutiliza ruta y fecha (no resetea el formulario); nunca se marca exito si la llamada falla; dialogo de confirmacion con ID.
  - Anadido `material-icons-core` para la flecha de volver (`Icons.AutoMirrored.Filled.ArrowBack`).
  - DatePicker M3 en `SearchDatePickerDialog` (estado compartido con boton confirmar).
- **Archivos modificados / creados**: core/model/FollowUpMode.kt, feature/search/{SearchViewModel,SearchScreen}.kt, navigation/AppNavHost.kt (Destinations.SEARCH), feature/home/HomeScreen.kt (boton "Buscar viajes"), strings.xml, libs.versions.toml, app/build.gradle.kts; tests: SearchViewModelTest.kt (12), FakeRenfeApi.kt (3 handlers nuevos), ApiContractTest.kt (+2 contratos: search/trains y create/followups).
- **Pruebas ejecutadas y resultados**:
  - `testDebugUnitTest` -> BUILD SUCCESSFUL; **26 tests / 0 fallos** (ApiContract 5, AuthInterceptor 3, PairingVM 6, SearchVM 12). SearchVM cubre debounce, seleccion, exito, estado vacio, validaciones (sin seleccion / mismo origen-destino / fecha pasada), error 503 (no se marca como ausencia), IOException, 409, create OK reutilizando ruta/fecha, specific requiere tren.
  - `assembleDebug` -> BUILD SUCCESSFUL; `app-debug.apk` (11.129.732 bytes), versionName 0.1.0.
  - `lintDebug` -> BUILD SUCCESSFUL (solo warning cosmico de deprecacion `menuAnchor()` sin argumentos).
  - **No validado en dispositivo real** (pendiente de aprobacion).
- **Bloqueos**: Ninguno para el paso 22. Pendientes de aprobacion externa: despliegue en VM (paso 06), validacion en realme GT Neo 2.
- **Siguiente paso**: No avanzar. Siguiente archivo `prompts/23-*.md`.
## 2026-09-13 - Paso 23 - Gestion de seguimientos
- **Estado**: Implementado + probado localmente (43 tests, 0 fallos) + APK debug compilado. Sin validacion en dispositivo real.
- **Cambios Android** (backend ya cumple; solo se anadio `?lifecycle=` como query opcional en RenfeApi):
  - Listado con filtros de ciclo de vida (todos/activos/pausados/vencidos/eliminados), carga al volver del detalle. Detalle con acciones: pausar/reanudar/renovar/confirmar aviso/eliminar. Nunca exito falso ante fallo de servidor. Confirmar no borra; eliminar requiere dialogo de confirmacion y es HTTP 204 idempotente.
  - Disponibilidad derivada de los episodios; ultima comprobacion valida = observed_at del episodio mas reciente (sin endpoint dedicado; si el backend necesite last_check real, anadir `updated_at` en paso futuro). Caducidad y fechas mostradas en Europe/Madrid (MadridFormat; `Instant.from(OffsetDateTime.parse(...))`).
  - Estados obsoletos/expirados mostrados como error (lifecycle expired -> Vencido). 503 de Renfe no confundido con listado vacio.
  - Navegacion: FOLLOWUPS y FOLLOWUP_DETAIL/{followupId}; Home boton "Mis seguimientos".
  - Archivos creados: feature/followups/{FollowUpsViewModel,FollowUpDetailViewModel,MadridFormat,FollowUpsScreen,FollowUpDetailScreen}.kt; tests: FollowUpsViewModelTest.kt (5), FollowUpDetailViewModelTest.kt (8).
  - Archivos modificados: core/model/FollowUpMode.kt (AvailabilityStateValue, FollowUpLifecycle, FollowUpAlertState), core/network/RenfeApi.kt (listFollowUps query lifecycle), navigation/AppNavHost.kt, feature/home/HomeScreen.kt, strings.xml (followups_*), FakeRenfeApi.kt (8 handlers), ApiContractTest.kt (+4 contratos: list filtro, detail episodios, pause POST ruta, delete 204).
- **Pruebas**:
  - testDebugUnitTest -> BUILD SUCCESSFUL; **43 tests / 0 fallos** (ApiContract 9, AuthInterceptor 3, PairingVM 6, SearchVM 12, FollowUpsVM 5, FollowUpDetailVM 8).
  - assembleDebug -> BUILD SUCCESSFUL; app-debug.apk (11.249.604 bytes), versionName 0.1.0.
  - lintDebug -> BUILD SUCCESSFUL (solo warning deprecacion menuAnchor() heredado).
  - No validado en dispositivo real (pendiente de aprobacion).
- **Bloqueos**: Ninguno para el paso 23. Pendientes: despliegue VM (paso 06), validacion en realme GT Neo 2.
- **Siguiente paso**: No avanzar. Siguiente archivo prompts/24-*.md.
## 2026-09-13 - Paso 24 - Notificaciones locales y FCM en Android
- **Estado**: Implementado + probado localmente (68 tests, 0 fallos) + APK debug compilado. FCM real marcado como pendiente (paso 33: proyecto Firebase/google-services.json). Sin validacion en dispositivo real.
- **FCM sin google-services**: sin plugin de Google Services; toda interaccion con Firebase/Messaging va protegida (FirebaseApp.getApps().isNotEmpty() + try/catch). La app funciona sin configuracion; registro de token y servicio activos solo con configuracion real.
- **Contrato backend**: PUT /api/v1/fcm/token (autenticado, no ruta publica), body {fcm_token} (patron 20..4096), respuesta {registered}; 409 si dispositivo inactivo. Se ha respetado docs/notificaciones-fcm.md + backend/app/api/fcm.py (sin tocar backend).
- **Payload data-only** (AlertPayload): type alert|test, event_id, followup_id, episode_id, title, body, channel_id (disponibilidad_plazas|resumen_y_servicio), priority, origin/destination, travel_date, departure, arrival, price, observed_at, expires_at. Test usan "-" placeholder; timestamps ISO-8601 con offset -> Instant.from(OffsetDateTime.parse(...)); "-"/ausente -> null.
- **Canales**: disponibilidad_plazas (HIGH, sonido+vibracion) y resumen_y_servicio (LOW, silencioso), creados en Application; openSystemSettings para ajustes (banner Home).
- **Dedupe persistente**: PrefsEventIdStore/EventLedger (SharedPreferences id|epoch, cap 500, ventana 30 dias). Episodio con observed_at > 30 min stale o expires_at pasado se rechaza; test se entrega siempre.
- **Acciones**: Abrir -> EXTRA_FOLLOWUP_ID -> pendingFollowupId (StateFlow) -> AppNavHost -> followup/{id}. Confirmar/Pausar -> NotificationActionReceiver (BroadcastReceiver) -> FollowUpActionWorker (WorkManager unico, KEEP, backoff 15s) con acknowledge/pause autenticados; reintentos acotados (NetworkPolicy; 404/409 exito idempotente, 401 revocacion, max 3); fallo final -> showActionFailed en canal servicio.
- **Token**: FcmTokenRegistrationWorker (unico REPLACE, backoff 30s) al arrancar emparejado, al emparejar (distinctUntilChanged) y desde onNewToken. Sin polling.
- **POST_NOTIFICATIONS**: solicitud unica (flag in prefs); si denega -> banner Home + "Abrir ajustes" + refresh on resume (LifecycleResumeEffect). Sin duplicados foreground/background (dedupe en ruta unica del servicio). Sin wake locks/WebSocket/servicio permanente.
- **Archivos creados (main)**: core/notifications/{AlertPayload,EventIdStore,NotificationChannels,NotificationDisplayer,FcmTokenGateway,NetworkPolicy,FcmTokenRegistrationWorker,FollowUpActionWorker,NotificationActionReceiver,RenfeMessagingService}.kt.
- **Archivos modificados**: RenfeNotifierApp.kt (canales, observa emparejamiento), MainActivity.kt (permiso + pendingFollowupId), AppNavHost.kt (navega al detalle), HomeScreen.kt (banner denegacion), RenfeApi.kt + ApiModels.kt (registerFcmToken PUT, FcmTokenRequest/Response), AndroidManifest.xml (POST_NOTIFICATIONS, .RenfeNotifierApp, servicio MESSAGING_EVENT exported=false, receiver acciones), strings.xml (home_notifs_*, notif_*), libs.versions.toml + build.gradle.kts (firebase-bom 33.7.0, firebase-messaging, work 2.9.1, coroutines-play-services, fragment-ktx 1.8.5).
- **Tests**: creados AlertPayloadParserTest (11), EventLedgerTest (6), NetworkPolicyTest (6); FakeRenfeApi registerFcmTokenHandler; ApiContractTest +2 (PUT fcm_token+registered, 409 lanza HttpException).
- **Pruebas**:
  - testDebugUnitTest -> BUILD SUCCESSFUL; **68 tests / 0 fallos** (ApiContract 11, AuthInterceptor 3, PairingVM 6, SearchVM 12, FollowUpsVM 5, FollowUpDetailVM 8, AlertPayloadParser 11, EventLedger 6, NetworkPolicy 6).
  - assembleDebug -> BUILD SUCCESSFUL; app-debug.apk (12.619.098 bytes), versionName 0.1.0.
  - lintDebug -> BUILD SUCCESSFUL sin errores (47 warnings: versiones nuevas + menuAnchor + recursos sin uso); corregido InvalidFragmentVersionForActivityResult (fragment-ktx) y ObsoleteSdkInt en canales.
  - Correcciones: constantes requestCode en NotificationDisplayer; test de 409 a cuerpo void (JUnit); poda EventLedgerTest via restore.
  - No validado en dispositivo real (pendiente de aprobacion). FCM real no validable sin Firebase (paso 33).
- **Bloqueos**: FCM real pendiente (proyecto Firebase + google-services.json). Pendientes de aprobacion externa: despliegue VM (paso 06), validacion en realme GT Neo 2.
- **Siguiente paso**: No avanzar. Siguiente archivo prompts/25-*.md.
## 2026-09-13 - Paso 25: Diagnóstico y ajustes (backend + Android) COMPLETADO
- Backend: monitoring.py (contadores de consultas lógicas, peticiones HTTP y bytes) registra en SchedulerService._check_group; GET /api/v1/diagnostics añade search_stats. Comprobado: pytest 166 passed.
- Android: core/diagnostics/ (ServerContactStore + HeartbeatInterceptor + DeviceEnvironment con play-services-base 18.5.0), feature/diagnostics/ (ViewModel + computeStages en 5 etapas + Screen con notificación de prueba y cifras), ajustes de tema/avisos en PreferencesRepository (SettingsSource) + MainActivity tema + FcmTokenGateway gate de avisos; nuevo destino y botón Home.
- Pruebas: gradlew testDebugUnitTest → 82 tests / 0 fallos (ApiContract 13, DiagnosticsViewModelTest 12); assembleDebug → APK 12.685.305 bytes; lintDebug sin errores (warnings heredados + versiones).
- Bloqueos: FCM real (paso 33) para validar entrega y notificación de prueba extremo a extremo; despliegue VM y validación en realme GT Neo 2 pendientes de aprobación. No avanzar al paso 26.

## 2026-09-13 - Paso 26 - CI del backend (workflow) COMPLETADO
- Workflow nuevo `.github/workflows/backend-ci.yml`: disparo push + pull_request sobre main; jobs detect-changes (dorny/paths-filter, paths backend/** y el workflow) → backend-ci → all-checks-ok (if always(); aprueba con success/skipped para que ningún check obligatorio quede bloqueado por filtros). Python 3.11 (coincide con requirements.lock), instalación solo desde el lock, timeout 10 min, concurrency con cancel-in-progress, permissions contents: read, acciones fijadas por SHA (checkout v4.4.0, setup-python v5.6.0, paths-filter v3.0.4). Sin secretos, sin Renfe ni producción.
- Higiene: el backend nunca estaba formateado con ruff format; se ejecutó `ruff format .` (32 ficheros, mecánico) para que `ruff format --check` sea verde.
- Validación local: YAML parseado con PyYAML (válido; ojo: PyYAML YAML 1.1 parses "on" como booleano, GitHub usa YAML 1.2). Comprobaciones replicadas: ruff check . → OK; ruff format --check . → 65 files; mypy app tests → OK (64 ficheros); pytest → 166 passed (2 warnings heredadas de starlette/anyio, no bloqueantes).
- No se ha hecho push ni se ha habilitado el workflow en GitHub (aprobación explícita y aviso previo: un push a main dispararía CI). Bloqueos externos: despliegue VM (paso 06), realme GT Neo 2, FCM real (paso 33). No avanzar al paso 27.

## 2026-09-13 - Paso 27 - CI de Android (workflow) COMPLETADO
- Workflow nuevo `.github/workflows/android-ci.yml`: disparo push + PR sobre main; jobs detect-changes (paths-filter android/** + workflow) → android-ci → all-checks-ok (if always(); aprueba success/skipped). JDK 17 Temurin, Gradle 8.9, AGP 8.5.2 (versiones locales). Caché: setup-java cache gradle + gradle/actions/setup-gradle v4.2.2. Pipeline: lintDebug → testDebugUnitTest (82 tests, 10 suites) → assembleDebug. APK debug artefacto retención 1 día. Acciones por SHA (checkout v4.4.0, setup-java v4.7.1, setup-gradle v4.2.2, paths-filter v3.0.4, upload-artifact v4.6.2). Permisos contents:read, concurrency cancel-in-progress, timeout 15 min. Sin secretos; debug compila sin google-services.json (guards runtime). Documentado Firebase en CI en docs/ci-cd.md §2.3.1 (Base64 secret + workflow manual).
- Validación local: YAML OK; lintDebug BUILD SUCCESSFUL; testDebugUnitTest 82 passed; assembleDebug BUILD SUCCESSFUL (APK 12.685.305 bytes).
- No push ni habilitación remota (aprobación explícita; push a main dispararía CI). Bloqueos: VM (paso 06), realme GT Neo 2, FCM real (paso 33). No avanzar al paso 28.

## 2026-09-13 - Paso 28 - Backup y recuperación COMPLETADO
- Backup WAL-aware (sqlite3 online backup API), verify (PRAGMA integrity_check + SHA256), restore seguro (valida antes de sobrescribir, --force, ejecuta migraciones), version (schema_migrations v5).
- CLI: backup/restore/verify/version en app.cli; usa bases temporales, no toca BD bot/VM.
- docs/recovery.md: procedimientos, ciclo prueba, integración despliegue, checklist.
- Tests: 166 passed (sin regresiones); validación ciclo completo OK.
- Bloqueos: validación VM (paso 06) pendiente aprobación. No avanzar paso 29.

## 2026-09-13 - Paso 29 - CD Backend (workflow + script) COMPLETADO
- workflow_dispatch con inputs deploy_sha + confirm ("DEPLOY"); valida SHA en main + checks CI success (backend-ci + all-checks-ok) vía GitHub API; aborta antes de infra si falla.
- Concurrency production-deploy max-parallel:1 (sin cancelación durante migraciones).
- OIDC + IAP: google-github-actions/auth + setup-gcloud + gcloud compute ssh --tunnel-through-iap (sin claves SSH, VM sin IP pública).
- Script deploy_backend.sh transaccional: backup SQLite .backup + integrity_check + SHA256 → checkout SHA → migraciones (app.db.migrate) → docker compose up -d → health check 30s → rollback automático en cualquier fallo (código, BD, servicio).
- Secretos en GitHub Secrets (GCP_PROJECT_ID, WIF, SA, VM_NAME, VM_ZONE, DEPLOY_USER); nada en imágenes/artefactos.
- Módulo migrate.py CLI para migraciones.
- Job validate-local: bash -n script, mock commit validation, sin secretos.
- Validación: YAML OK, bash -n OK, migrate OK, mock commit OK, pytest 166 passed.
- Bloqueos: VM (paso 06) pendiente aprobación; OIDC+IAP no probado end-to-end; realme GT Neo 2, FCM real (paso 33). No avanzar paso 30.

## 2026-09-13 - Paso 30 - Firma y distribución APK COMPLETADO
- Workflow android-release.yml: workflow_dispatch (inputs version_name, version_code, confirm "RELEASE") + tags v*.*.* → valida commit en main + CI success (backend-ci, android-ci, all-checks-ok) vía GitHub API.
- Keystore en secret base64 (ANDROID_KEYSTORE_BASE64 + 3 passwords); decodificado en runner.temp, limpieza shred/rm en always().
- assembleRelease con signingConfig condicional (solo si env vars presentes); apksigner verify; SHA256 generado.
- Artefactos retención 5 días + Release privada GitHub (solo tags) con APK + .sha256.
- Sin PRs; concurrency max-parallel:1; docs/keystore.md (generación keytool, backup 2+ ubicaciones, rotación, verificación).
- Validación: YAML OK, assembleRelease local sin firmar BUILD SUCCESSFUL, lint+tests 82 passed, pytest 166 passed.
- Bloqueos: generación keystore + secrets GitHub pendiente aprobación; release real no ejecutado. VM (06), realme, FCM (33) pendientes. No avanzar paso 31.

## 2026-09-13 - Paso 31 - Validación local integrada COMPLETADO
- Backend: pytest 166 passed (parser/Plaza H/fechas/matching, estados/recordatorios/caducidad/reinicios, agrupación/deduplicación, auth/validación, migraciones/backup/restore). Suite focalizada 59 passed.
- Android: 82 tests/0 fallos (10 suites), lint OK, APK debug 12.685.305 bytes.
- CLI: backup (WAL-aware+SHA256), verify (integrity_check), version (v5), restore (--force, post-restore migraciones), migrate → todo OK.
- Workflows (4): pinning SHA verificado (checkout v4.4.0, setup-python v5.6.0, setup-java v4.7.1, setup-gradle v4.2.2, paths-filter v3.0.4, upload-artifact v4.6.2, auth v2.1.3, setup-gcloud v2.1.0, create-release v1.1.4, upload-release-asset v1.0.2). android-release sin trigger PR.
- docs/validation.md creado con evidencia completa, checklist y pendientes.
- Bloqueos: VM (06), Firebase (33), keystore (30), push (regla 4.2), realme/FCM (35). No avanzar paso 32.

## 2026-09-14 - Paso 32 - Mediciones y fallback Selenium COMPLETADO
- Mediciones locales: 1 y 5 seguimientos, con/sin agrupación. 1 seg: 0.06s, 5 req, 12 KB; 5 seg (5 grupos): 0.06s, 10 req, 25 KB; 5 seg (1 grupo): 0.15s, 5 req, 12 KB. Ahorro agrupación 50%. Concurrencia 1 vs 2 similar.
- Proyección VM e2-micro: RAM base 43 MB, <1 MB/ciclo, CPU <15%. Latencia real Renfe ~300ms → 1 grupo 1.5s/ciclo, 5 grupos 7.5s/ciclo.
- Selenium NO necesario: HTTP DWR basta; Selenium añade 150-300 MB RAM (Chrome) en e2-micro (1 GB), riesgo OOM. Margen app sola: 781 MB.
- Mediciones reales VM pendientes (requieren aprobación): latencia real Renfe, CPU/RAM 1h, GC Python, SQLite WAL concurrente, bytes facturados, arranque docker compose.
- Fixes idempotencia migraciones: CREATE TABLE IF NOT EXISTS, IF NOT EXISTS índices, plaza_h en migración 1, migración 2 solo índice, manejo errores "duplicate column"/"already exists" en apply_migrations, initialize verifica tablas reales.
- Tests: 166 passed (backend), 82/0 fallos (Android), CLI ciclo completo OK, measure_resources.py 5 escenarios OK.
- Bloqueos: VM (06), Firebase (33), keystore (30), push (regla 4.2), realme/FCM (35). No avanzar paso 33.

## 2026-09-14 - Paso 33 - Plan de configuración real COMPLETADO
- Plan detallado en docs/real-config-plan.md: Firebase Spark (gratis), VM e2-micro Free Tier + OIDC/WIF + IAP, TLS autofirmado/Cloudflare, CD OIDC+IAP, release tags/dispatch, keystore local + backup físico 2+ ubicaciones, secrets GitHub (12, categorizados ALTA/MEDIA/BAJA).
- Coste 0 € validado: GCP Free Tier (e2-micro), Firebase Spark, GitHub Actions 2000 min, Tailscale personal.
- Checklist §8 (9 ítems) para aprobación explícita antes de paso 34. Reversión global documentada.
- Bloqueos: aprobación explícita por ítem (Firebase, VM, secrets, keystore, CD, release, dispositivo real, mediciones VM). Push CI pendiente regla 4.2. No avanzar paso 34.
