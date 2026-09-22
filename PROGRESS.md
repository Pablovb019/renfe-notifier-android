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

## Paso 18: API funcional (estaciones, bsquedas, seguimientos, FCM, diagnstico)
- **Estado**: Completado (registro completo en prompts/PROGRESS.md)
- **Decisiones**:
  - **Anti-SSRF**: las bsquedas solo aceptan cdigos de estacin validados contra el catlogo local (StationCatalog.by_code); ningn campo admite URLs del cliente (cumple docs/security.md).
  - app/renfe/search.py (TrainSearchEngine) como lgica pura para evitar import circular con deps.py.
  - app/api/search.py: GET /api/v1/search/stations?q= y POST /api/v1/search/trains (fecha en [hoy, hoy+62] Europe/Madrid, origen?destino, TrainOut con identity del dominio 
eal:...; estacin desconocida ? 400, RenfeClientError ? 503).
  - app/api/followups.py: CRUD + pause/resume/renew/acknowledge/delete bajo /api/v1/followups. followup_id generado en servidor; mode specific exige specific_train_id; 404 si no existe y 409 si el estado no lo permite; delete lgico idempotente; pause/delete/acknowledge cancelan recordatorios (queue.cancel_for_followup). Confirmar no borra el seguimiento (REQ-07.4).
  - app/api/fcm.py: PUT /api/v1/fcm/token registra/renueva el token FCM del dispositivo.
  - app/api/diagnostics.py: GET /api/v1/diagnostics/health (pblico), GET /api/v1/diagnostics (contadores) y POST /api/v1/diagnostics/test-notification (emisor inyectado; 503 sin emisor, 409 sin token FCM).
  - app/notifications/base.py: protocolo NotificationSender.send_test (FCM real en paso 19).
  - Migracin v5 (devices.fcm_token), PairingRepository.update_fcm_token/count_devices, PairingService.register_fcm_token, FollowUpRepository.count(lifecycle)/count_episodes.
  - Lifespan de main.py expone repositorio/cola/catlogo/engine en app.state; 
edact() enmascara tokens FCM largos (ahora con :).
- **Archivos modificados / creados**:
  - Creados: ackend/app/renfe/search.py, ackend/app/api/search.py, followups.py, cm.py, diagnostics.py, ackend/app/notifications/, ackend/tests/helpers.py, ackend/tests/test_search_api.py, test_followups_api.py, test_fcm_api.py, test_diagnostics_api.py.
  - Modificados: app/api/deps.py, app/main.py, app/__init__.py (APP_VERSION), app/db/migrations.py (v5), app/pairing/{domain,database,service}.py, app/followups/database.py, app/middleware/logging.py, 	ests/__init__.py, aserciones de esquema [1,2,3,4,5].
- **Pruebas ejecutadas y resultados**:
  - 
uff check .: All checks passed.
  - python -m mypy app tests: no issues found in 59 source files.
  - python -m pytest: 134 superados (107 anteriores + 27 nuevos). Cubren: estaciones/bsquedas con MockTransport (auth, validaciones de fecha/estaciones, TrainOut tipado); CRUD y acciones de seguimientos (id servidor, validaciones de mode, estados/409, acknowledge sin borrar, delete idempotente); FCM (auth, registro/renovacin, 422); diagnstico (health pblico, contadores, test-notification 401/503/409/200 con FakeSender) y fcm_token/count en repositorio.
  - No se realizaron pruebas reales contra Renfe, envos FCM ni validaciones en dispositivo/VM.
- **Bloqueos**: Ninguno para el paso 18. El bloqueo preventivo de despliegue en VM del paso 06 permanece.
- **Siguiente paso**: Paso 19 (segn prompts/).

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

## 2026-09-15 - Linting fix y backend-ci OK (post-paso 33) COMPLETADO
- Linting corregido en 5 archivos: scripts/measure_resources.py, backend/app/db/migrate.py, backend/app/main.py, backend/app/cli.py, backend/app/db/connection.py, backend/app/db/migrations.py
- Fixes: DTZ011, ASYNC251, F541, F401, I001, F541, W292, formato ruff
- backend-ci: ✅ SUCCESS (GitHub Actions) - ruff check, ruff format, mypy, pytest (166 passed)
- Bloqueos: mismos que paso 33 (aprobaciones checklist §8, push CI regla 4.2, keystore, VM, Firebase, realme/FCM)
- No avanzar paso 34.


## 2026-09-16 - Bloqueos revisados y CI Android verde COMPLETADO
- Revisados los 5 bloqueos: Firebase, VM/OIDC/IAP (secret GDP_SERVICE_ACCOUNT renombrado a GCP_SERVICE_ACCOUNT por usuario), keystore y push/CI quedan RESUELTOS. Unico pendiente real: paso 35 (dispositivo real + FCM).
- Fix mayor: secret con typo GDP_SERVICE_ACCOUNT que romperia backend-cd (ahora GCP_SERVICE_ACCOUNT).
- Fix android-release.yml: clave invalida max-parallel en concurrency reemplazada por cancel-in-progress: false (commit e931b8b).
- Fix android-ci Permission denied (exit 126): gradlew sin bit ejecutable (100644) -> chmod +x (100755), commit 37a8493.
- Verificacion real: backend-ci (ruff, format, mypy, pytest 166) [ok] run 35148529308 y android-ci (lintDebug + unit tests + assembleDebug) [ok] run 35148684913.
- Bloqueos restantes: paso 35 (realme GT Neo 2 + FCM real). No avanzar mas sin instrucciones.


## 2026-09-16 - Paso 34 - Configuracion real autorizada COMPLETADO
- Checklist §8 aprobado parcialmente (items 1-5): Firebase+google-services.json, VM/OIDC/IAP (sin Tailscale), secrets GitHub, keystore+backup, secrets release.
- Aplicada configuracion pendiente del item 1/5: plugin com.google.gms.google-services 4.4.2 condicional + decodificacion/limpieza de google-services.json en android-release.yml (el secret existia pero era inerte).
- Verificado sin typos: secretos en workflows == secretos existentes. YAML valido. Sin google-services/keystore en git.
- CD NO ejecutado (items 6-9 no aprobados). Build gradle local no validado (sin JDK); pasa a CI con proximo push autorizado.
- Bloqueos: items 6-9 pendientes de aprobacion, push pendiente de autorizacion, validacion CI del plugin condicional pendiente.
- No avanzar paso 35.


## 2026-09-16 - Paso 34 - Validacion local build (JDK 17) COMPLETADO
- JDK 17 Microsoft detectado en PATH; validado build gradle local:
- assembleDebug sin google-services.json: BUILD SUCCESSFUL (38 tasks) - el plugin condicional no rompe CI debug.
- processDebugGoogleServices con google-services.json de prueba temporal: BUILD SUCCESSFUL; genera values.xml con google_app_id/project_id/gcm_defaultSenderId. Archivo temporal eliminado.
- Fin de paso 34; bloqueos iguales (items 6-9 no aprobados, push pendiente autorizacion).


## 2026-09-16 - Item 6 - backend-cd dry-run + despliegue real EN CURSO
- Actualizado GCP: API iamcredentials habilitada, secret GCP_PROJECT_ID corregido (numero->ID textual), rol roles/iap.tunnelResourceAccessor otorgado a la SA (fixes del despliegue).
- Dry-run backend-cd (run 35153841604): SUCCESS completo (commit 1c3152e) - confirmacion, SHA en main, checks CI por nombre, auth OIDC (WIF), setup gcloud; despliegue skipped.
- Despliegue real (dry_run=false): run 35154146218 fail (--project requiere ID textual), run 35154616983 fail (IAP 4033 not authorized, falta rol tunnel), run 35154980863 progreso: tunel OK via OS Login, script aborta en prerequisitos: sqlite3 NO instalado en la VM.
- backend-ci: linea limpia de warnings third-party (backend/pyproject.toml filterwarnings): 166 passed en CI (run 35151734916) y local sin warnings.
- Rationale fixes: endpoint combinado /status se auto-contamina con check runs del propio backend-cd (commit 1c3152e: gate valida jobs CI por nombre).
- Commits en main: 89cdee4 (paso 34 + dry_run), ffac7f6 (warnings), 1c3152e (gate checks por nombre).
- Bloque y siguiente paso: instalar prerequisitos en la VM (sqlite3, verificar docker/docker-compose/git/curl) y relanzar backend-cd real.


## 2026-09-16 - Item 6 - Bootstrap primera instalacion backend (UVICORN+SYSTEMD) EN CURSO
- Runtime: NO Docker (no existian Dockerfile/docker-compose.yml del backend nuevo). Usuario eligio uvicorn+systemd.
- VM preparada manualmente por el usuario: repo clonado (PAT lectura), .venv python3.14 + requirements.lock, .env produccion, unit /etc/systemd/system/renfe-notifier-backend.service, sudoers NOPASSWD restringido, /data chown al SA OS Login (sa_104384329745603569192).
- Evidencia: servicio active (running), PID 16512, memoria 61.2M, health localhost:8000 -> status ok.
- Cambios repo: scripts/deploy_backend.sh reescrito a systemd (quita docker, primeras instalaciones crean BD vacia, rollback=systemctl restart) + nuevo scripts/renfe-notifier-backend.service (plantilla).
- Pendiente: commit+push autorizado (activaria CI) y relanzar backend-cd real con nuevo SHA.


## 2026-09-16 - Item 6 - backend-cd real EXITOSO (CD cerrado)
- Workflow backend-cd (run 35158433609, SHA cf313d58): success. En VM: backup SQLite, checkout cf313d58, migraciones aplicadas, systemctl restart renfe-notifier-backend, health 200 -> DESPLIEGUE EXITOSO.
- Cadena completa: confirmacion DEPLOY + checks CI + OIDC/WIF + IAP + script systemd. Item 6 del checklist (real-config-plan.md seccion 8) CERRADO.
- Siguientes: item 7 (android-release tag v0.1.0), item 8 (realme + FCM real), item 9 (mediciones VM) - requieren aprobacion.


## 2026-09-16 - Item 7 - android-release: fix checks nominal aplicado
- Mismo bug auto-contaminacion que en backend-cd corregido en android-release.yml (valida backend-ci/android-ci/all-checks-ok por nombre via API check-runs, no estado combinado). YAML OK.
- Pendiente: commit+push autorizado y tag v0.1.0 sobre CI verde (compila APK firmado + release privada).


## 2026-09-17 - Item 7 - android-release: SUCCESS (v0.1.0)
- Run 35265221397 (SHA f9e9b7a): success. APK firmado + verificado (apksigner), checksum SHA256, Release privada creada con gh release create.
- Fixes aplicados: checks:read permission, android-actions/setup-android build-tools + PATH, gh release create (reemplaza create-release deprecated).
- Item 7 del checklist (real-config-plan.md seccion 8) CERRADO.
- Siguientes: item 8 (instalar APK en realme GT Neo 2 + FCM real), item 9 (mediciones VM).


## 2026-09-17 - Item 8 - Emparejamiento + FCM real CERRADO
- Releases lanzadas del arreglo de crash y conexion: v0.1.1 (b0b857f), v0.1.2 (48d3b9f), v0.1.3 (f895220), v0.1.4 (ff33d0e), v0.1.5 (a925112). Todas con android-release success.
- Crash "se exige HTTPS salvo hosts locales" con la IP de produccion: causa real en ApiModule.validateBaseUrl (allowHttpHosts no incluia la IP), no en network_security_config (aunque se fijo includeSubdomains=false por lint). Fix db8b220.
- Conectividad: backend escuchaba solo en 127.0.0.1. Fix: unit systemd bind 0.0.0.0 (7aa749d) + regla firewall GCP allow-renfe-backend (tcp:8000, 0.0.0.0/0, tag renfe-backend).
- IP efimera cambia al parar la VM: reservada IP estatica 34.26.252.164 y reasignada (app apunta ahi: build.gradle.kts BACKEND_URL, network_security_config, ApiModule.allowHttpHosts).
- FCM 403 PERMISSION_DENIED (ACCESS_TOKEN_SCOPE_INSUFFICIENT): VM tenia scopes restringidos. Fix: set-service-account --scopes=cloud-platform + rol roles/firebase.admin a 337831457324-compute@developer.gserviceaccount.com + API fcm.googleapis.com.
- SenderId mismatch en FCM: la app usaba el proyecto Firebase renfe-notifier-android, pero el backend enviava a renfe-notifier-bot. Fix: RENFE_NOTIFIER_FCM_PROJECT_ID=renfe-notifier-android en .env de la VM + rol firebase.admin en ese proyecto.
- Validacion FCM end-to-end: "Enviar Notificacion de Prueba" desde Diagnostico y ajustes -> notificacion recibida correctamente en realme GT Neo 2 (com.pablovb019.renfenotifier 0.1.5).
- Ajuste de versiones alineadas: cada tag lleva su versionName/versionCode (v0.1.3+).
- Siguientes: item 9 (mediciones VM) pendiente de aprobacion.


## 2026-09-17 - Item 9 - Mediciones VM reales (paso 32) CERRADO
- Autorizado 1 GET controlado a venta.renfe.com desde la VM para latencia real.
- `scripts/measure_resources.py` parametrizado: `--db` (BD temporal migrada, NO toca /data/renfe_notifier.db), `--output`, `--environment`; mide solo ficheros .db/.db-wal/.db-shm y añade GC Delta; sys.path incluye backend/ para venvs sin editable install. Commits 8a58448 y 0a4ca09 (CI backend/android verdes).
- Scheduler con mocks (BD /tmp/renfe-measure-vm.db) en e2-micro: RAM base ~47 MB, delta max +0.9 MB, CPU hasta 15.2%, GC delta 0, disco 0 KB, 5 req/12450 bytes por grupo y 10 req/24900 bytes sin agrupar.
- Latencia real Renfe (GET autorizado, 1 peticion): DNS 0.030s, connect 0.121s, TLS 0.317s, TTFB 0.424s, total 0.424s, HTTP 200, 11083 bytes.
- Health + restart: primer request tras restart 26ms, siguientes ~2.1ms de mediana (10 req, 200 OK). Ruta real es /health (el router no lleva prefijo; /api/v1/health devuelve 404).
- Resultados completos en docs/measurements.json (run vm-e2-micro-real).
- Selenium descartado de nuevo: con latencia real ~0.42s/peticion y agrupacion, el costo por ciclo es viable en e2-micro sin Chrome (-150-300 MB RAM).
- Item 9 del checklist (real-config-plan.md seccion 8) CERRADO.


## 2026-09-17 - Paso 35 - Escenario 1 VALIDADO en real; faltan escenarios 3-5
- Autorizacion recibida: 1 busqueda DWR real controlada (escenario 1) + FCM end-to-end/acciones (3-4) + resiliencia realme (5).
- 1er intento: generateId #2 fallo con `handleBatchException: Failed to find parameter: windowName`.
- 2o intento (tras corregir handshake): `getTrainsList` devolvio DWR U014 (sesion reiniciada) porque el POST de busqueda no llevaba Content-Type de formulario.
- **Correccion implementada** (commits a3f2e8a, 7654c08, 613bd5a): `app/renfe/client.py` con payloads DWR reales (windowName/instanceId, c0-eN+Object_Object, page buscarTrenEnlaces.do, cookie Search, scriptSessionId _tokenify) y busqueda como formulario; `app/renfe/parser.py` con estructura anidada real (`listviajeViewEnlaceBean`, `tarifaMinima`/`razonNoDisponible`, "NaN"->sin precio).
- **Verificado localmente y en CI**: 168 tests, ruff format/check OK, mypy OK; backend-ci y android-ci verdes.
- **3er intento (VALIDADO, commit 613bd5a)**: Madrid 60000 -> Barcelona 71801 el 2026-09-19, plaza_h=False; 5 POSTs HTTP 200; `ParseStatus: OK`, 14 trenes reales con horas/precios; 2,247 s y 234016 B. Sin reservar ni crear seguimientos.
- Escenarios 3-5 (FCM/acciones/Doze/reinicio/ahorro) pendientes de ejecutar en el realme GT Neo 2.
- Siguiente: escenario 3-4 con seguimiento real breve y acciones; despues escenario 5. No avanzar al paso 36.


## 2026-09-18 - Paso 35 - Pruebas en realme (adb) y fix de notificacion de prueba
- **Hallazgo de arquitectura (no es fallo del paso)**: el backend no ejecuta el planificador ni entrega avisos reales (`SchedulerService` sin instanciar; `AlertQueue.enqueue/due_events` y `FcmNotificationSender.send_alert` sin llamadores; `create_followup` no encola). Coherente con `prompts/34` ("no actives sondeo del nuevo backend") y pendiente para la transicion (paso 37). Por eso la etapa "Deteccion" de la app muestra "Revisar" (logical_queries=0) y el escenario "alerta real con acciones" no es validable aun.
- Realme GT Neo 2 (RMX3370) conectado por USB; app v0.1.5/code6 (firstInstall 2026-09-17 23:09).
- Diagnostico en dispositivo: Google Play Services OK, permiso de notificaciones concedido, canal de avisos activo; dispositivos=1, seguimientos=0, episodios=0, consultas logicas=0, peticiones HTTP=0, bytes=0 B.
- Canales confirmados por el sistema: `disponibilidad_plazas` (importance=4, sonido/vibracion) y `resumen_y_servicio` (importance=2); ambos `mBypassDnd=false`. Desactivar/reactivar el canal responde en la app.
- Notificacion de prueba FCM: 1er envio recibido (id `projects/renfe-notifier-android/messages/0:1789686040309188%7ee20577f9fd7ecd`); los siguientes NO aparecian.
- **Defecto real detectado**: `send_test` reutilizaba `event_id="test"` fijo y la app deduplica por `event_id` 30 dias (`FcmTokenGateway.kt:36`, `EventIdStore.kt:56-57`), descartando silenciosamente las pruebas repetidas. Evidencia (adb, DND desactivado `zen_mode=0`): logcat muestra recepcion FCM `FirebaseInstanceIdReceiver ... act=com.google.android.c2dm.intent.RECEIVE pkg=com.pablovb019.renfenotifier` y `dumpsys notification` sin ninguna notificacion de la app.
- **Correccion implementada**: `backend/app/notifications/fcm.py` `send_test` usa `event_id=f"test-{uuid4().hex}"` por envio; test nuevo `test_send_test_uses_unique_event_id_per_send`. Verificado local: ruff format/check OK, mypy OK, 169 tests OK.
- Pendiente en el realme: reintentar prueba tras desplegar; pantalla apagada, Doze, reinicio, Wi-Fi/datos, ahorro de bateria, cierre desde recientes vs forzar detencion y actualizacion con la misma firma. Acciones Confirmar/Pausar pendientes (requieren aviso real o envio controlado tipo alert). No se inventaran resultados sin verificacion real.
- **Fix desplegado y validado**: la VM estaba en HEAD desacoplado (el `git pull` no aplicaba); se paso a `main` (d95733f) y `systemctl restart`. Notificacion de prueba enviada 2 veces -> **llegaron 2** (antes solo la primera).
- **Defecto de audio detectado**: el canal `disponibilidad_plazas` usa `AudioAttributes.USAGE_ALARM` (`NotificationChannels.kt`), por lo que suena por el stream de alarmas e **ignora** el modo vibracion/silencio/volumen de notificaciones (confirmado por el sistema: `mAudioAttributes usage=USAGE_ALARM` y `mUserLockedFields=4`).
- **v0.1.6 (code7)**: intento de fix con `USAGE_NOTIFICATION` + migracion borrar/recrear con el mismo id. **NO funciono** en el realme: Android conserva los ajustes bloqueados por el usuario (`mUserLockedFields=4`, sonido) al recrear un canal con el mismo id; el canal segui en `usage=USAGE_ALARM`. La actualizacion con la misma firma si se valido (firstInstall 2026-09-17 23:09 sin cambios, lastUpdate 2026-09-18 01:31, v0.1.6/code7).
- **v0.1.7 (code8) - fix definitivo**: canal de avisos con **id versionado** `disponibilidad_plazas_v2` en `NotificationChannels.kt` y `AlertPayload.kt`, migracion que borra el canal antiguo, y backend `CHANNEL_ALERT = "disponibilidad_plazas_v2"` (`fcm.py`) + docs (`notificaciones-fcm.md`, `architecture.md`) + tests Android. Version 0.1.7/code8.
- **Validado en realme (v0.1.7/code8 instalado encima de v0.1.6; firstInstall 2026-09-17 23:09 intacto)**: `disponibilidad_plazas_v2` creado con `usage=USAGE_NOTIFICATION` y `mUserLockedFields=0` (importancia 4); el canal antiguo `disponibilidad_plazas` queda `mDeleted=true`; `resumen_y_servicio` intacto. La actualizacion con la misma firma quedó validada dos veces (v0.1.6 y v0.1.7).
- **Nota**: el primer intento del workflow android-release para v0.1.7 falló por resolución de dependencias (error_prone_annotations-2.18.0.jar/failureaccess-1.0.1.jar, transitorio), se re-ejecuto y quedó OK.
- **Pendiente de verificacion funcional (accion del usuario)**: confirmacion auditiva de que respeta vibracion/silencio (poner el movil en silencio/vibracion y pulsar "Enviar notificacion de prueba"). Pendientes tambien: pantalla apagada, Doze, reinicio, Wi-Fi/datos, ahorro, cierre desde recientes vs forzar detencion. El backend aun envia `channel_id` antiguo hasta el redeploy (afecta solo al contrato; la app mapea al canal v2).
- **End-to-end validado en realme (16:49)**: con el movil desbloqueado se pulso "Enviar notificacion de prueba" en la pantalla Diagnostico; llegó el FCM (`FirebaseInstanceIdReceiver ... c2dm.intent.RECEIVE`), el sistema vibró y `dumpsys notification --noredact` muestra `NotificationRecord` activo con `channel=disponibilidad_plazas_v2` (importance=4). La entrega pasa por el canal v2 con `USAGE_NOTIFICATION`.
- **Redeploy del backend (VM) OK** (usuario): `git pull --ff-only` a `e53f2c7`, restart, `active (running)`, health `{"status":"ok","uptime_s":13.8}`; **v0.1.6 y v0.1.7** ya en el repo de la VM. El payload ahora envia `channel_id=disponibilidad_plazas_v2`.
- **Validacion auditiva del canal v2 OK** (usuario, realme): en **vibracion** solo vibra; en **silencio** ni sonido ni vibracion; en **sonido** sonido de notificacion + vibracion. El canal de avisos respeta el modo del sistema.
- **Herramienta de envio controlado**: nuevo subcomando `renfe-notifier-cli test-notification` (`backend/app/cli.py`) que envia la prueba al primer dispositivo activo con token FCM (lee la BD en la VM, no imprime tokens) y `backend/tests/test_cli.py` (5 tests). Necesario para enviar con pantalla apagada, bloqueado, Doze, reinicio o sin conexion, sin depender del boton de la app. Verificado: ruff, mypy OK, 174 tests OK, CI verde (`34bcbbb`). Primer envio con pantalla apagada por adb NO salio (la app se congelo antes de completar la peticion) -> motivo de la herramienta.
- **MATRIZ DE ENTREGA REALME (paso 35, realme GT Neo 2, v0.1.7/code8)** — todas enviadas por CLI desde la VM con prioridad alta de FCM (fcm.py:85, `priority="high"`, TTL 300 s) y verificadas por adb:
  1. **Activo/desbloqueado**: OK (confirma el usuario).
  2. **Pantalla apagada/bloqueado**: OK -> llego con pantalla off + keyguard (mAwake=false, mDreamingLockscreen=true), sin limitante.
  3. **Doze profundo**: OK -> `dumpsys deviceidle force-idle` (mState=IDLE) y la notificacion llego (nuevo `NotificationRecord` id=1393262305, canal v2, importance=4).
  4. **Reinicio con app cold**: OK -> app sin proceso tras boots y el envio la desperto (PID 20473) y publico id=50876914.
  5. **Sin conexion -> recuperacion**: OK -> con `Active default network: none`, FCM acepto el mensaje (0:1789834395903752), NO se publico nada en 120 s sin red, y al reactivar Wi-Fi/datos llego (id=818237636) dentro del TTL.
  6. **Forzar detencion**: NO llega nada (mensaje 0:1789834680150836) mientras la app este `am force-stop` (pidof vacio y 0 registros). Limite de Android (FCM no despierta apps forzadas hasta que el usuario las abre). Registrado para documentar.
  7. **Ahorro de bateria Realme**: la notificacion SI se publica (id=1353263872) pero **sin sonido** (mSettingBatterySaverEnabled=true). Al desactivar el ahorro, el mismo envio vuelve a sonar/vibrar -> A/B confirmado: el ahorro silencia las alertas. La app NO esta en la whitelist de Doze (`dumpsys deviceidle whitelist` sin renfenotifier).
- **Recomendacion para el usuario**: anadir la app a las excepciones de optimizacion de bateria de Realme (ajustes de bateria) para que con ahorro activo las alertas suenen; aveisar que tras "Forzar detencion" no habra notificaciones hasta abrir la app.

## Paso 36: Plan de transicin
- **Estado**: Completado (documento de plan; NO autoriza transicin).
- **Fecha**: 2026-09-19
- **Decisiones**:
  - Se reviso el estado global: backend nuevo desplegado (systemd, /data/renfe_notifier.db, IP 34.26.252.164), FCM real validado y matriz de entrega completa en realme.
  - Bloqueo clave documentado: el backend nuevo NO ejecuta aun el planificador (`SchedulerService.run_forever`) ni entrega avisos reales (`send_alert` sin llamadores); corresponde al paso 37.
  - Plan de sustitucin/rollback redactado en `docs/transicion.md` con 11 secciones: backup del bot y SQLite (inventario VM), base nueva sin importar, automatizaciones que podran sobrescribir (deploy.yml original, compose/cron VM, VM_SSH_KEY), detencion controlada, arranque (cableado pendiente), consultas/avisos reales y mediciones, condiciones de fallo y restauracion inmediata, proteccion de backups y clave de firma, sondeo unico sin duplicados, e intervenciones del usuario.
  - No se modifico el repositorio original ni se deshabilito nada; no se ejecuto transicion alguna.
- **Archivos creados**: `docs/transicion.md`.
- **Pruebas ejecutadas**: revision de docs/audit.md (workflow antiguo SSH directo), scripts/deploy_backend.sh y unit systemd, backend-cd.yml, recovery.md, prompts/PROGRESS.md (items 6-9 cerrados) y confirmacion por grep de que scheduler/send_alert no estan cableados en main.py.
- **Bloqueos / siguiente paso**: aprobacion explcita del usuario para cada bloque de `docs/transicion.md`; luego paso 37 (37-transicion-autorizada.md). Detenido a la espera de instrucciones.

## Paso 37: Transicin aprobada - VERIFICACION DE PRECONDICIONES
- **Estado**: Detenido en la compuerta (precondiciones P1 y P4 no cumplidas; P3 backup pre-corte pendiente).
- **Fecha**: 2026-09-19
- **Verificacion (evidencia)**:
  - P1 Aprobacin explicita del plan: **NO concedida**. En la sesion anterior el usuario eligio "Commit docs y parar", que excluye la transicion. docs/transicion.md 11 exige aprobacion por bloques.
  - P2 Bloqueos de seguridad/coste: ausentes. Presupuesto 0 euros; no se contrataron recursos. Firewall 8000 abierto a Internet queda como consideracion (no bloqueante de la transicion en si).
  - P3 Backup + recuperacion: procedimiento documentado (docs/recovery.md, docs/transicion.md #2). Backup fresco previo al corte y su descarga fuera de la VM: PENDIENTE (accion en VM del usuario).
  - P4 Commit exacto autorizado con CI: **NO existe**. El planificador no esta cableado (SchedulerService sin instanciar en main.py, send_alert sin llamadores). ultimo commit 14e0243 (docs-only, CI verde). Pendiente: commit con cableado+CI, autorizado, para desplegar.
  - P5 APK instalado y configuracion validada: OK. Realme conectado (serial 1ecdc196), APK v0.1.7 code8 (lastUpdateTime 2026-09-19 16:46). Emparejamiento y FCM ya validados (paso 35).
- **Decision**: Segun la regla del paso 37 ("Si falta algo, detente"), se detiene la ejecucion. No se ha ejecutado transicion alguna ni se ha tocado la VM.
- **Siguiente paso**: obtener aprobacion explicita por bloques del usuario (inventario/backups VM, cablear scheduler+avisos reales, desplegar commit autorizado, detener bot, consulta/medicion real) y entonces ejecutar la transicion con validacion de aviso en el telefono.
- **Pruebas ejecutadas**: adb devices (serial 1ecdc196), dumpsys package (v0.1.7/code8), git log HEAD, gh check-runs de 14e0243 (backend-ci/android-ci skipped, detect-changes success, all-checks-ok success), grep previo de cableado del planificador.

## Paso 37: Transicion aprobada - BLOQUE A (cablear avisos reales)
- **Estado**: En curso - Bloque A implementado y verificado localmente (ruff, mypy, 188 tests OK). Pendiente: commit autorizado con CI (P4) y bloques B/C/D.
- **Fecha**: 2026-09-19
- **Decisiones** (aprobacion por cuestionario: usuario autorizo A, B, C y D):
  - Producir un aviso real al detectar un episodio nuevo: `SchedulerService` acepta ahora una `AlertQueue` opcional (+ `initial_delay_s`, `max_attempts`) y encola `ReminderEvent` con id determinista `followup:episode` y `episode_id` de la fila persistida, solo cuando `outcome.new_episode`.
  - `AlertDeliveryService` (`backend/app/reminders/delivery.py`): consume `due_events`, construye `AlertMessage` con el catalogo (`backend/app/notifications/alerts.py`, canal `disponibilidad_plazas_v2`, prioridad alta, collapse key `followup:{id}`) y envia a todos los dispositivos activos con token FCM. Reglas: seguimiento inactivo/caducado -> cancelar; token invalido -> limpiar token y `mark_failed`; error retryable (429/5xx) -> queda pendiente; error no retryable (401/403/400) -> `mark_failed`; sin sender -> ciclo inerte.
  - Cableado en `create_app`: ambas tareas solo arrancan si `scheduler_enabled` (nuevo setting, por defecto `false`) para que tests y desarrollo no sondeen Renfe ni encolen; `SchedulerService` usa `interval_s`/retraso/tope de settings y un adaptador Station->codigos hacia `TrainSearchEngine`.
- **Archivos modificados / creados**: `backend/app/scheduler/service.py`, `backend/app/reminders/delivery.py` (nuevo), `backend/app/notifications/alerts.py` (nuevo), `backend/app/main.py`, `backend/app/config.py`, `backend/.env.example`, `backend/tests/test_scheduler_service.py`, `backend/tests/test_reminders_delivery.py` (nuevo, 12 tests).
- **Pruebas ejecutadas y resultados**:
  - `ruff check app tests` -> All checks passed.
  - `ruff format --check app tests` -> 70 files already formatted.
  - `mypy app tests` -> no issues found in 69 source files.
  - `pytest` -> 188 passed (176 previos + 12 nuevos: encolado en scheduler sin/con cola, entrega, builder de payload, token invalido, sin dispositivos, cancelaciones por ciclo de vida y caducidad, retryable/no-retryable y run_forever).
  - No se ejecuto el planificador ni se envio ningun aviso real; sigue pendiente activarlo en produccion (scheduler_enabled) en el Bloque C.
- **Bloqueos / siguiente paso**: P3 (backup fresco en VM) y bloques B/C/D pendientes del usuario en la VM. El planificador sigue DESACTIVADO en produccion (scheduler_enabled=false) hasta el bloque C. Siguiente: coordinar con el usuario los bloques B (inventario+backup), C (corte/deploy+stop bot) y D (validacion telefonica).

## Paso 37: Transicion aprobada - BLOQUE A CI VERDE (b133a9c)
- **Estado**: Bloque A completado y verificado por CI (autorizacion explicita del usuario). Commit `b133a9c` pusheado a origin/main.
- **Pruebas/CI ejecutadas y resultados (evidencia)**:
  - `gh api commits/b133a9c/check-runs` -> 6 checks todos completos: `all-checks-ok` success (x2), `backend-ci` success, `android-ci` skipped, `detect-changes` success (x2). Estado del commit: 0 protocol de statuses legacy (solo check-runs; correcto).
  - `git status` local -> limpio tras el push.
- **Siguiente paso**: esperar instrucciones del usuario para bloques B (inventario + backup VM), C (deploy del commit + stop del bot antiguo + activar scheduler) y D (validacion telefonica). P3 (backup pre-corte) sigue pendiente en la VM.

## Paso 37: Transicion aprobada - BLOQUE B (impacto de plan por dato del usuario)
- **Estado**: Bloque B en curso - preparados comandos de inventario (§2.1). El usuario aporto un dato que modifica el plan: **el bot antiguo ya no esta corriendo** en la VM, aunque se ejecuto durante un tiempo y deja residuos que limpiar.
- **Decisiones adoptadas**:
  - docs/transicion.md: §5 pasa de "detener el bot" a "verificar que nada lo relance" (se conserva la orden de detencion real si el inventario encontrara un proceso activo).
  - Nuevo §5bis "Limpieza de artefactos residuales": tras backups verificados y fuera de la VM (P3), se **archivan** (no destruyen) los residuos del bot (repo, .env antiguo, cron/timer/compose, SQLite) en /data/archive con SHA256; el .env antiguo nunca se archiva en el paquete (tokens de Telegram). Solo con permiso explicito. No se toca el repo Git original (reglas 01/16).
  - §11 actualizado: la aprobacion 3 ahora incluye detener/limpiar residuos.
- **Archivos modificados / creados**: `docs/transicion.md` (§2.1 nota, §5, nuevo §5bis, §11), `prompts/37-bloque-b-inventario.md` (nuevo, bloque de comandos de solo lectura para pegar en la VM).
- **Pruebas ejecutadas**: ninguna en VM (acciones del usuario). La revision del plan no toca codigo; sin riesgo de CI.
- **Bloqueos / siguiente paso**: ejecutar el inventario en la VM (bloque de comandos preparado) y pegar la salida aqui para verificar. Luego §2.2 backups del bot antiguo + §2.3 backup nuevo + descarga fuera de la VM (P3).

## Paso 37: Transicion aprobada - BLOQUE B - INVENTARIO §2.1 (ejecutado en VM)
- **Estado**: inventario principal ejecutado por el usuario (salida pegada). Hallazgos: bot antiguo NO corriendo (confirma dato), solo corre el backend nuevo.
- **Evidencia (resumen con la salida completa registrada en esta sesion)**:
  - systemd: `renfe-notifier-backend.service` `active (running)` (PID 23869, uvicorn, arrancado 2026-09-19 14:52 UTC; 2h 20min uptime). Sin otras units de app renfe (el resto son devices de disco que casan por el nombre de la instancia).
  - docker: permission denied (sin acceso al daemon; no se pudo listar). cron: sin crontab para SA. timers renfe: ninguno.
  - Repo bot antiguo: `~/renfe-notifier-bot` NO existe; homes presentes: `pablovb01_gmail_com`, `ubuntu`, `sa_104384329745603569192`. En /data solo `renfe_notifier.db` y `backup_20260916_223656.db`.
  - Backups backend nuevo: ONLY `backup_20260916_223656.db` (pre-corte, sin backup fresco => se hará en §2.3).
  - Procesos: único uvicorn del backend (23869). Sin bot.
  - .env/compose antiguos: find original no devolvió rutas (alcance maxdepth 2 y permisos; pendiente inventario-2).
- **Decisiones adoptadas**:
  - El bot residencial probablemente está bajo otro home (`pablovb01_gmail_com` o `ubuntu`) o con permisos que SA no lee -> nuevo bloque de comandos `prompts/37-bloque-b-inventario-2.md` con `sudo` (solo lectura) para localizar repo, SQLite, `.env` (solo ruta), compose, cron de otros usuarios, timers y procesos python ajenos.
  - Sin backend backup fresco -> P3 pendiente hasta §2.3.
- **Archivos**: `prompts/37-bloque-b-inventario-2.md` (creado).
- **Pruebas**: ninguna modifica el sistema; todo de solo lectura en VM (acciones del usuario).
- **Bloqueos / siguiente paso**: ejecutar inventario-2 en la VM y pegar salida para confirmar ubicación/residuos del bot; luego §2.2 (backup consistente SQLite del bot + integrity + SHA256) y §2.3 (backup nuevo backend con app.cli backup + descarga fuera de VM = P3).

## Paso 37: Transicion aprobada - BLOQUE B - INVENTARIO-2 y -3 (bot antiguo NO existe en la VM)
- **Estado**: inventarios -2 y -3 ejecutados. Se confirma con busquedas ampliadas en todo el FS que **no existe rastro del bot antiguo en la VM** (no repo/venv/BD/.env/logs/containers/units/cron/timers/procesos). Solo corre el backend nuevo.
- **Evidencia (inventario-3, salida pegada)**:
  - A) find '*renfe*bot*'/*telegram*' en FS (sin /usr): vacio.
  - B) Solo /data/renfe_notifier.db, /data/backups/backup_20260916_223656.db, /tmp/renfe-measure-vm.db (medicion del paso previo, esperada); resto son BDs de sistema/snap/man/docker.
  - C) Imagen docker unica: hello-world (sin bot).
  - D) Units: solo renfe-notifier-backend.service (activa). /etc/systemd/system solo unit propia + symlinks standard.
  - E) /var/log sin traces del bot.
  - F) bash_history de pablovb01: sin comandos renfe/bot visibles (sin secretos expuestos).
- **Decisiones adoptadas**:
  - El bot antiguo NO esta, ni corriendo ni parado: el §2.2 (backup del bot) pasa a NO APLICA; el §5bis (limpieza de residuos) NO APLICA salvo retirar PENDIENTE la imagen `hello-world` docker (residual, no relacionada con el bot; valorar en §5bis, requiere aprobacion). §5 (detencion/verificacion)queda vacuo: nada que detener ni que pueda relanzar el bot en la VM.
  - Unico riesgo residual de relanzamiento: `deploy.yml` del repo ORIGINAL (§4/§10) - pendiente decision del usuario.
  - Backup pendiente: solo el del backend nuevo (§2.3 + P3 descarga fuera de la VM).
- **Archivos / creados**: `prompts/37-bloque-b-inventario-2.md`, `prompts/37-bloque-b-inventario-3.md`, `prompts/37-bloque-b-backup.md` (bloque §2.3).
- **Pruebas**: solo lectura en VM (acciones del usuario). Sin cambios de estado.
- **Bloqueos / siguiente paso**: ejecutar §2.3 con `prompts/37-bloque-b-backup.md` (backup fresco `app.cli backup` + verify + SHA256), y P3: descargar el backup y la BD fuera de la VM y confirmar SHA256 local. Decision del usuario sobre `deploy.yml` del repo original (intervencion §10): despues del backup.

## Paso 37: Transicion aprobada - BLOQUE B - §2.3 BACKUP FRESCO (ejecutado y verificado)
- **Estado**: backup del backend nuevo creado y verificado en la VM. P3 (descarga fuera de la VM) pendiente del usuario.
- **Evidencia (salida pegada, 2026-09-19)**:
  - `app.cli version` -> esquema 5.
  - `app.cli verify` -> OK: /data/renfe_notifier.db - integrity_check: ok.
  - `app.cli backup` -> `/data/backups/backup_20260919_171740.db`, SHA256 `92b885f4f31ef26cd774ddb8adc575444000801582c83355919758671076edfa`, esquema 5.
  - `sha256sum` de ambos backups: `backup_20260916_223656.db` = 78f52be9...c8; `backup_20260919_171740.db` = 92b885f4...fa (COINCIDE con el CLI).
  - Health: 200. `systemctl is-active renfe-notifier-backend` -> active.
  - BD activa: /data/renfe_notifier.db 98304 bytes (WAL sin archivos -wal/-shm listados).
- **Decisiones adoptadas**: backups conservados en /data/backups con SHA256 anotado. El SHA256 de referencia para restauracion es el del CLI: **92b885f4f31ef26cd774ddb8adc575444000801582c83355919758671076edfa**.
- **Pruebas**: las del bloque (version/verify/backup/sha256/health) - todo en VM por el usuario.
- **Bloqueos / siguiente paso**: P3 - descargar `backup_20260919_171740.db` (y opcionalmente la BD activa) FUERA de la VM y confirmar la ruta local y su SHA256; con eso Bloque B completo. Luego decision del usuario sobre deploy.yml del repo original (§4/§10) y seguir con Bloque C.

## Paso 37: Transicion aprobada - BLOQUE B COMPLETO (P3 descarga fuera de la VM)
- **Estado**: P3 ejecutado. Backup del backend nuevo descargado fuera de la VM y SHA256 verificado localmente. **Bloque B (inventario §2.1 + backups) COMPLETO.**
- **Evidencia (2026-09-19)**:
  - VM alojada en el proyecto GCP `renfe-notifier-bot` (no `renfe-notifier-android`); la cuenta local gcloud `pablovb01@gmail.com` no tiene compute habilitado en el proyecto equivocado. Dusto: `--project renfe-notifier-bot`.
  - Firma SSH del host `34.26.252.164` registrada (ed25519 SHA256:RoYpXifW9rJPOZWA+4DvNn4UmyOWwxXgN03q2wWxQns).
  - Descarga OK: `backup_20260919_171740.db` (96 kB) -> `C:\Users\pablo\Downloads\backup_20260919_171740.db`.
  - `Get-FileHash` local = 92B885F4F31EF26CD774DDB8ADC575444000801582C83355919758671076EDFA (coincide con el SHA256 del CLI en la VM).
- **Decisiones adoptadas**: el backup queda conservado en /data/backups (VM) y en local (Windows). Referencia SHA256 **92B885F4F31EF26CD774DDB8ADC575444000801582C83355919758671076EDFA** (mayusculas local / minusculas CLI) para restauracion (§8).
- **Pruebas**: scp (gcloud, proyecto renfe-notifier-bot) + Get-FileHash local + hash CLI ya verificado en §2.3.
- **Bloqueos / siguiente paso**: §4/§10 (decision del usuario sobre `deploy.yml` del repo original - riesgo de relanzar el bot en cada push al repo antiguo) y Bloque C (deploy commit autorizado en VM, activar scheduler, verificar unico propietario).

## Paso 37: Transicion aprobada - P3 ALMACENAMIENTO EXTERNO (USB BitLocker)
- **Estado**: P3 completado del todo. Los backups del backend nuevo estan en almacenamiento fuera de la VM.
- **Evidencia (dato del usuario, 2026-09-19)**: los ficheros de la VM `renfe_notifier.db` (BD activa) y `backup_20260919_171740.db` (backup con SHA256 92B885F4...EDFA) estan guardados en un USB configurado con Bitlocker.
- **Decisiones adoptadas**: se cumple §2.3/P3 (copia fuera de la VM + encriptada + SHA256 anotado). Ruta local previa: C:\Users\pablo\Downloads\backup_20260919_171740.db (punto intermedio antes de pasar al USB).

## Paso 37: Transicion aprobada - §4/§10 INTERVENCION REPO ORIGINAL (autorizada y ejecutada)
- **Estado**: el usuario autorizo deshabilitar `deploy.yml` y revocar `VM_SSH_KEY` en el repo original `Pablovb019/renfe-notifier-bot`. Ejecutado en GitHub.
- **Evidencia**:
  - Workflow `Deploy to Google Cloud VM` (`.github/workflows/deploy.yml`) -> estado `disabled_manually`.
  - Secret `VM_SSH_KEY` eliminado del repo original. Secrets restantes: `VM_HOST`, `VM_PROJECT_PATH`, `VM_USER` (sin la clave SSH; no suponen acceso por sí solos). Public key ID 3380204578043523366.
- **Decisiones adoptadas**: la cuenta local `gcloud` `pablovb01@gmail.com` opera sobre proyectos `renfe-notifier-android`, `renfe-notifier-bot` y `gen-lang-client-0528226058`; la VM vive en `renfe-notifier-bot`.
- **Archivos modificados / creados**: `docs/real-config-plan.md` (§2 nota "DATO REAL DE INFRAESTRUCTURA" con proyecto GCP renfe-notifier-bot, IP, zona, OS Login/SA, firma host SSH), `docs/transicion.md` (§1 nota proyecto GCP).
- **Pruebas**: `gh api workflows/devplue.yml disable` + `gh secret delete VM_SSH_KEY` verificados vía API (sin interactuar con la VM).
- **Bloqueos / siguiente paso**: Bloque C (deploy del commit autorizado en la VM, activar `scheduler_enabled`, verificar unico propietario).

## Paso 37: Transicion aprobada - BLOQUE C FASE 1-2 (deploy + scheduler ACTIVADO)
- **Estado**: Bloque A deployado en la VM y planificador ACTIVADO con exito. Unico propietario verificado. Pendiente: crear seguimiento de prueba desde la app (§6.4) y Bloque D.
- **Evidencia (2026-09-19, salidas pegadas)**:
  - `git pull --ff-only origin main`: la VM paso de 34bcbbb a `dfaf14f` (fast-forward 15 ficheros), HEAD=origin/main. Bloque A presente: `scheduler_enabled` en config.py:41 y `AlertDeliveryService` en main.py:28/108.
  - `.env`: `RENFE_NOTIFIER_SCHEDULER_ENABLED=true` anadido (antes solo ENVIRONMENT/HOST/PORT/SECRET_KEY/DATABASE_PATH/FCM_PROJECT_ID/FCM_APP_PACKAGE). Se mostraron solo nombres de claves.
  - `systemctl restart renfe-notifier-backend`: proceso viejo 23869 parado limpio; nuevo PID **26879**. Log 17:37:08: "Planificador y entrega de avisos activados." / "Arrancando renfe-notifier-backend" / "Application startup complete." / "Uvicorn running on http://0.0.0.0:8000". Health 200 (tras 5s; el primer curl dio 000 por arranque en curso, en estado Ds de disco).
  - Un unico proceso uvicorn (26879, sin tracebacks/errores en journal).
  - `app.cli devices --list`: 1 dispositivo activo `43308a07ef24c81d | realme RMX3370 | creado 2026-09-17 21:29 | activo` (sin exponer token). Health 200. Esquema v5.
  - Nota: quedaron ficheros sin seguir en el repo de la VM `v0.1.0`..`v0.1.5` (APK antiguos); anotados para limpieza opcional posterior (no tocados).
- **Decisiones adoptadas**: el sensor/entrega de avisos reales queda ARRANCADO en produccion (un unico propietario). Sin errores arranque. Resta validar el flujo completo real.
- **Pruebas**: las de deploy/restart/log/health/devices en la VM (usuario); verificadas aqui.
- **Bloqueos / siguiente paso**: §6.4 - crear un seguimiento de prueba desde la app (realme) y confirmar el flujo completo (deteccion -> episodio -> alert_events -> FCM -> notificacion v2). Luego Bloque D (medicion recursos en VM y validacion telefonia).

## Paso 37: Transicion aprobada - BLOQUE C §6.4 BLOQUEADO - CRASH APP (scroll resultados busqueda)
- **Estado**: DIAGNOSTICADO y FIX implementado y probado localmente. Bloque C §6.4 sigue pendiente de re-ejecutar (la app instalada v0.1.7 crasheaba al crear un seguimiento).
- **Evidencia (2026-09-19, adb realme GT Neo 2, serial 1ecdc196)**:
  - Crash reproducido 2 veces en la app `com.pablovb019.renfenotifier` al hacer scroll hacia abajo en los resultados de una busqueda (tras introducir origen, destino, fecha y buscar). PIDs 28325 y 7250.
  - Excepcion en logcat: `java.lang.IllegalArgumentException: Key "real:MD|11:08:00|12:13:00" was already used.` + "If you are using LazyColumn/Row please make sure you provide a unique key for each item." Rastro Compose (LayoutModifierNodeCoordinator.measure).
- **Causa raiz**: la `identity` de un tren (backend) se construye como `real:{identifier}` con `identifier = service|salida|llegada` (parser.py:146, domain.py:65-70). Renfe puede devolver **dos servicios con los mismos horarios** (p. ej. dos `MD` 11:08->12:13), produciendo identities identicas. La LazyColumn de SearchScreen usaba `key = { it.identity }` (SearchScreen.kt:231), clave duplicada -> crash.
- **Decisiones adoptadas**: fix en dos capas. (1) Backend: dedup de trenes por `identifier` preservando orden en `POST /api/v1/search/trains` (`_unique_trains_by_identifier` en `backend/app/api/search.py:115`); asi la app v0.1.7 ya instalada recibe resultados unicos sin reinstalar. (2) Android: `itemsIndexed` con key unica `"${identity}#$index"` (SearchScreen.kt:231) como defensa ante backends antiguos.
- **Archivos modificados / creados**:
  - `backend/app/api/search.py` (dedup `_unique_trains_by_identifier`, tipo `Iterable`)
  - `backend/tests/test_search_api.py` (test dedup + fixture DUP_TRAIN_LIST)
  - `android/app/src/main/java/com/pablovb019/renfenotifier/feature/search/SearchScreen.kt` (`itemsIndexed` + import)
- **Pruebas ejecutadas y resultados (local)**:
  - Backend: `pytest` -> **189 passed** (188 previos + 1 nuevo dedup); `ruff check app` -> All checks passed; `mypy app` -> no issues (43 source files).
  - Android: `:app:compileDebugKotlin` -> BUILD SUCCESSFUL (warning preexistente menuAnchor); `:app:testDebugUnitTest --tests feature.search.*` -> BUILD SUCCESSFUL; `:app:lintDebug` -> BUILD SUCCESSFUL. JDK hallado: `C:\Program Files\Microsoft\jdk-17.0.20.101-hotspot` (no estaba en PATH; JAVA_HOME no definido).
- **Bloqueos / siguiente paso**: SIN COMMIT NI PUSH AUN (pendiente autorizacion). Proximo: commit+push (advierte CI), deploy del commit en la VM (git pull + restart), y re-ejecutar §6.4 en el realme con la app v0.1.7 (el dedup backend evita el crash sin reinstalar). Opcional: build/sign v0.1.8 con el fix Android.

## Paso 37: Transicion aprobada - CRASH APP RESUELTO EN PRODUCCION (deploy a1398fd en VM)
- **Estado**: commit `a1398fd` (fix dedup backend + key unica Android) pusheado con CI verde y DESPLEGADO en la VM. El fix llega a la app v0.1.7 instalada sin reinstalar.
- **Evidencia (2026-09-19, salidas pegadas)**:
  - `git pull --ff-only`: dfaf14f -> a1398fd (fast-forward, 5 ficheros, +145/-3), HEAD=origin/main=a1398fd.
  - `systemctl restart` directo fallo por polkit interactivo ("Access denied... requires interactive authentication"); con `sudo systemctl restart` OK.
  - PID viejo 26879 parado limpio ("Shutting down" / "Backend detenido correctamente"). Nuevo PID **27418** (unico uvicorn).
  - Log 18:05:30: "Planificador y entrega de avisos activados." / "Arrancando renfe-notifier-backend" / "Application startup complete." / "Uvicorn running on http://0.0.0.0:8000". Health 200 (`{"status":"ok","uptime_s":22.4}`).
- **Decisiones adoptadas**: el reinicio del servicio en la VM exige `sudo` (vía /etc/sudoers.d) por configuracion polkit interactiva; queda anotado para proximos deploys.
- **Pruebas**: git pull/restart/log/health/pgrep en la VM (usuario), verificadas aqui.
- **Bloqueos / siguiente paso**: §6.4 - re-ejecutar el seguimiento de prueba desde la app (realme v0.1.7): buscar, hacer scroll sin crash, crear seguimiento y validar flujo completo (deteccion -> episodio -> alert_events -> FCM -> notificacion canal v2 con acciones). Luego Bloque D.

## Paso 37: Transicion aprobada - LOTE VALIDACION 6.4 (5 fixes: disponibilidad, dialogo creado, no-seguimiento con plazas, papelera, nombre raro)
- **Estado**: 5 fixes implementados y probados localmente (backend + Android). Sin commit ni push aun (pendiente autorizacion).
- **Evidencia (2026-09-19)**: el usuario reporto 4 problemas tras re-ejecutar 6.4 con la app v0.1.7: (1) todos los trenes de Sevilla-San Bernardo->Jerez aparecian "Disponible" aunque en Renfe solo habia plaza H (y plaza H no activada); (2) el dialogo "seguimiento creado" mostraba el popup pero el boton Aceptar no hacia nada (no navegaba a la pagina principal); (3) al eliminar un seguimiento y confirmar, seguia apareciendo en el listado; (4) el detalle mostraba un "nombre raro" (identity del tren).
- **Causas raiz**:
  - Disponibilidad: `_parse_dwr_availability` (backend/app/renfe/parser.py:200-210) ignoraba `soloPlazaH`; el bot heredado los trata como disponibles SOLO si se pidio plaza_h. Replicada su logica exacta (`renfechecker.py:252-263`): base_available = no completo and razon in ("","8") and tarifaMinima valida; plaza_h_only = bool(soloPlazaH); disponible si (base and plaza_h_only) con h, o (base and not plaza_h_only) sin h.
  - Dialogo creado: `TextButton` del AlertDialog "seguimiento creado" tenia el onClick vacio (SearchScreen.kt:475), comentario "se mantiene la ruta..." -> nada. Se conecta a `onCreatedAccepted` + navegacion a Home.
  - Borrado: el delete es LOGICO (followups.py:283-295 marca lifecycle=deleted) pero el listado general (`?lifecycle=` sin filtro) incluia los deleted; el usuario quiere papelera (solo visibles con filtro "Eliminados").
  - Nombre raro: FollowUpDetailScreen.modeText() mostraba `specificTrainId` (identity "real:MD|11:08:00|12:13:00"). Se muestra horario "11:08 -> 12:13" extrayendo los dos ultimos campos de la identity.
- **Decisiones adoptadas (usuario, question tool)**:
  - Aplicar el principio heredado en TODOS los modos (specific y first/last/all): si hay trenes con plazas, NO se crea seguimiento; aviso + vuelta a Home.
  - Replicar el comportamiento del bot para Plaza H: `soloPlazaH=true` sin plaza_h => NO disponible (permite seguimiento); con plaza_h solo cuentan los soloPlazaH como disponibles.
  - Borrado: papelera (el default "Todos" excluye los eliminados; filtro "Eliminados" los muestra).
  - Nombre raro: mostrar horarios en vez del identity crudo.
- **Archivos modificados / creados**:
  - Backend: `app/renfe/parser.py` (`_parse_availability`/`_parse_dwr_availability` con `plaza_h_requested`, replica heredada), `app/api/followups.py` (listado sin filtro excluye DELETED), `tests/test_dwr_parser.py` (2 tests soloPlazaH), `tests/test_followups_api.py` (papelera en test delete).
  - Android: `feature/search/SearchViewModel.kt` (estado `availableTrainNotice`, guard `hasAvailableTrain`, `onCreatedAccepted` limpia ambos), `feature/search/SearchScreen.kt` (dialogo Aceptar conectado a onCreatedAccepted + dialogo "ya tiene plazas", cableado en AppNavHost... en SearchScreen), `res/values/strings.xml` (2 strings nuevos, 1 variante horario), `feature/followups/FollowUpDetailScreen.kt` (modeText con horarios), `test/.../SearchViewModelTest.kt` (helper con availability parametrizable + 3 tests nuevos).
- **Pruebas ejecutadas y resultados (local)**:
  - Backend: `pytest` -> **191 passed** (189 + 2 nuevos); `ruff check app` -> All checks passed; `mypy app` -> no issues (43 source files).
  - Android: `:app:compileDebugKotlin` -> BUILD SUCCESSFUL (warning preexistente menuAnchor); `:app:testDebugUnitTest` -> BUILD SUCCESSFUL (incluye 3 tests nuevos de "no seguimiento con plazas" y first); `:app:lintDebug` -> BUILD SUCCESSFUL.
- **Bloqueos / siguiente paso**: commit+push (avisar CI) y deploy en la VM (git pull + `sudo systemctl restart`). Re-ejecutar 6.4 completo en el realme v0.1.7: (a) una ruta con solo plaza H debe mostrar "Sin plazas"; (b) crear seguimiento y verificar Aceptar vuelve a Home; (c) crear desde un tren con plazas NO debe crear seguimiento (aviso + Home); (d) eliminar y verificar que desaparece del listado; (e) detalle especifico muestra horario, no identity. Requiere re-build del APK para ver los fixes Android (v0.1.8) o al menos backend para disponibilidad/papelera.

## Paso 37: Transicion aprobada - LOTE VALIDACION 6.4 PUSHEADO (CI verde) - PENDIENTE DEPLOY
- **Estado**: commit `68f779d` en main con los 5 fixes. CI verde: backend-ci success, android-ci success (job all-checks-ok incluido en backend-ci). Deploy en la VM PENDIENTE (requiere git pull + sudo systemctl restart en la VM por el usuario).
- **Evidencia**: `098007a` (lote fixes) + `68f779d` (style ruff format) pusheados; `gh run list` para 68f779d -> backend-ci completed/success, android-ci completed/success.
- **Nota**: el primer push fallo en CI solo por `ruff format --check` (2 ficheros sin formatear); corregido con `ruff format` (solo estilo, sin cambios logicos; 191 passed de nuevo) en commit aparte 68f779d (sin amend ni force-push).
- **Pruebas locales previas**: backend 191 passed + ruff check + mypy (43 ficheros); android compileDebugKotlin/testDebugUnitTest/lintDebug BUILD SUCCESSFUL.
- **Bloqueos / siguiente paso**: DEPLOY en VM: `git pull --ff-only origin main` y `sudo systemctl restart renfe-notifier-backend` (el restart exige sudo por polkit); verificar health 200 y log. Los fixes backend (disponibilidad soloPlazaH y papelera) quedan activos sin reinstalar la app. Los fixes Android (dialogos, no-seguimiento con plazas, horarios en detalle) requieren APK nuevo (v0.1.8) para el realme. Luego re-ejecutar 6.4.

## Paso 37: Transicion aprobada - LOTE VALIDACION 6.4 DESPLEGADO EN VM (68f779d)
- **Estado**: commit `68f779d` (5 fixes) desplegado en la VM con exito. Planificador activo, unico proceso, health 200.
- **Evidencia (2026-09-19, solo algunos datos (del usuario respecto a la VM))**: el usuario ejecuto `git pull --ff-only origin main` (a1398fd..68f779d, fast-forward, 11 ficheros +348/-25) y `sudo systemctl restart renfe-notifier-backend`.
  - Health: `curl http://localhost:8000/health` -> `{"status":"ok","uptime_s":59.3}` (la ruta es `/health`, no `/api/v1/health`).
  - Log: **PID viejo 27418 parado limpio** ("Backend detenido correctamente"); **nuevo PID 27685** unico uvicorn: "Planificador y entrega de avisos activados." + "Arrancando renfe-notifier-backend" + "Application startup complete." + "Uvicorn running on http://0.0.0.0:8000".
  - `pgrep -af uvicorn`: unico proceso 27685 (`.venv/bin/python3 ... uvicorn app.main:app`).
- **Decisiones adoptadas**: los fixes backend (disponibilidad soloPlazaH y papelera) quedan activos en produccion sin reinstalar la app. Los fixes Android (dialogo Aceptar->Home, no-seguimiento con plazas, horarios en detalle) requieren APK nuevo (v0.1.8) para el realme.
- **Pruebas**: git pull/restart/health/log/pgrep en la VM (usuario), verificadas aqui.
- **Bloqueos / siguiente paso**: build/sign APK v0.1.8 con fixes Android, instalar en realme, y re-ejecutar 6.4 completo: (a) ruta con solo plaza H -> "Sin plazas" tras el fix backend; (b) Aceptar vuelve a Home; (c) tren con plazas -> no crea seguimiento; (d) eliminar -> desaparece de TODOS y solo queda en "Eliminados"; (e) detalle especifico muestra horario 11:08 -> 12:13. Luego Bloque D.

## Paso 37: Bloque C 6.4 VALIDADOS EN REALME (v0.1.8/code9) + lote v0.1.9
- **Estado**: los 5 fixes del lote 6.4 validados en el realme con la app v0.1.8 (code9). Nuevos hallazgos de UX/estabilidad de la revalidacion.
- **Evidencia / reporte del usuario (2026-09-19, realme, app v0.1.8/code9)**:
  - (1) soloPlazaH: ruta Sevilla-San Bernardo->Jerez aparece "Sin Plazas" (fix confirmado, verificado vs app de Renfe).
  - (2) Aceptar del dialogo -> vuelve a Home (OK). PERO el popup "Seguimiento ECjNBa_FIGOt63q creado" muestra el id interno -> se elimina el id del string.
  - (3) tren con plazas -> no crea seguimiento, aviso "Este tren ya tiene plazas" + Home (OK).
  - (4) papelera: eliminar desaparece del listado; filtro Eliminados lo muestra (OK). PERO el dialogo de confirmacion de borrado muestra el id interno -> se elimina.
  - (5) detalle en modo concreto muestra "Tren de 11:08 a 12:13" (OK confirmado).
  - Nuevo bug UX: autocompletado de estaciones. Al mantener pulsado el boton de borrar solo se borra un caracter porque el desplegable predictivo roba el foco (ExposedDropdownMenuBox). Fix: DropdownMenu con PopupProperties(focusable=false).
  - Nuevo reporte: error aleatorio "Renfe no respondio" (503). La misma busqueda falla a veces y al reintentar funciona. Sospecha: rate-limit/temporal de Renfe desde la IP de cloud. Fix diagnosticador: log del backend ahora registra la clase de error exacta (RenfeTransportError/RenfeBudgetExceededError/RenfeRateLimitedError/RenfeResponseError), pendiente de observar en produccion.
- **Decisiones adoptadas**: popups sin ids internos; estaciones con Dropdown no-focal; diagnostico del 503 por logs (sin consultas de prueba contra Renfe).
- **Pruebas**: tests Android (testDebugUnitTest OK), compileDebugKotlin OK, lintDebug OK; backend 191 passed + ruff + mypy (43 files) OK.
- **Bloqueos / siguiente paso**: commit+push (CI) y deploy; build/sign v0.1.9 para validar borrado fluido y popups sin id; observacion del log del 503.

## 2026-09-19 - Lote v0.1.9/v0.1.10: popups sin id, autocompletado en flujo, release con polling
- **Estado**: v0.1.9 (code10) instalada y validada; v0.1.10 (code11) instalada y comportamiento de autocompletado confirmado por el usuario. Ranking backend por relevancia implementado, probado y desplegado.
- **Decisiones / cambios**:
  - Popups de "seguimiento creado" y "eliminar seguimiento" sin el id interno (strings.xml).
  - Autocompletado: `ExposedDropdownMenuBox` robaba el foco (impedia borrar con pulsacion larga) -> `DropdownMenu` + `PopupProperties(focusable=false)` y luego **sugerencias en flujo** (sin popup): siempre debajo del campo, `heightIn(max=192.dp)` (~4 visibles) con scroll interno (estilo Google Maps), sin "pepear" por tecla (`SearchViewModel` limpia `*Suggestions=emptyList()` al cambiar consulta).
  - `android-release.yml`: paso "Verificar checks CI superados" cambiado de curl unico a **polling** (120 intentos x 10 s, fallo rapido ante failure/cancelled, exige success/skipped por job) + timeout del job a 40 min. Permite enviar tag y commit en **un único push** sin riesgo de que CI esté en_progress al validar.
  - Ranking de sugerencias: nuevo `station_match_score()` en `stations.py` (exacto > prefijo del nombre > palabra > subcadena, prioridad de Renfe como desempate; pesos 100/50/25/10 + weight 50/(1+priority)). `search.py` usa `-score` + nombre.
- **Versiones**: v0.1.9/code10 commit `6401e54`/`86851e2`; fix autocompletado+workflow+bump v0.1.10/code11 commit `ab46b0f` (push unico main+tag, CI verde, polling esperó ~4 min); ranking commit `8d2bb94`.
- **Pruebas / evidencia**:
  - v0.1.9: SHA256 cfb38200..., firma CN=Pablo Vilar (digest c241a29e...3950), instalada realme, popups sin id OK, borrado fluido OK.
  - v0.1.10: SHA256 37a59734..., firma identica, instalada realme (code11), autocompletado en flujo debajo del campo + scroller **validado por el usuario** ("este comportamiento es el que quiero").
  - Release v0.1.10 creada por run `35468489784` (polling de 20:48:03 a 20:52:12 antes de compilar).
  - Backend ranking: 197 passed (6 scorer tests nuevos), ruff check/format OK, mypy OK (43 files); CI verde para `8d2bb94`.
  - Deploy VM: git pull a `8d2bb94`, `sudo systemctl restart`, PID 28741 unico, health 200, "Planificador y entrega de avisos activados." Las sugerencias del realme (v0.1.10) ya usan el nuevo orden **sin reinstalar**: server-side.
- **Bloqueos / siguiente paso**: **VALIDADO** el orden de sugerencias por relevancia en el realme (reporte del usuario, 2026-09-19: "probado"). Las sugerencias (server-side) salen ya ordenadas sin reinstalar la app. Pendiente Bloque D (medicion recursos VM). Detenido a la espera de instrucciones.

## Bloque D - Medición de recursos en VM con planificador activo (2026-09-19)
- **Estado**: COMPLETADO. Medición de 1 h (720 muestras c/5 s) del proceso real en producción (PID 28741) + re-ejecución de `measure_resources.py` (FakeSearch, BD temporal) en la VM. Hallazgo crítico: el planificador falla ~100 % de los ciclos contra Renfe.
- **Decisiones adoptadas**: 
  - Recursos: RAM del servicio vivo ~107 MB mediana / 116 MB p95 (plateau acotado; supera objetivo aspiracional <100 MB pero holgura total en e2-micro 1 GB). CPU idle 0.2 % mediana, picos 12 % máx. Disco 0 KB. Dentro de holgura para coste 0 €.
  - Funcional: 27 errores en 1 h (1 seguimiento activo Sevilla→Jerez). 26x "La segunda respuesta generateId no contiene token DWR" + 1 budget + 1 HTTP 503. El regex `_TOKEN_CALLBACK` no hace match en 2ª respuesta generateId → flujo DWR roto desde IP GCP. **El notificador NO detecta disponibilidad en producción ahora mismo.**
- **Archivos modificados / creados**:
  - `scripts/measure_resources.py` (ya existía), `docs/measurements.json` (nueva run `vm-e2-micro-real-bloque-d`), `docs/bloque-d-evidence/` (evidencia cruda copiada de la VM: `bloque-d-live.json` 68 kB, `bloque-d-live.log` 28 kB, `bloque-d-measure-results.json` 3 kB), `PROGRESS.md` (este registro).
- **Pruebas / evidencia**:
  - Bloque 3 (FakeSearch): RAM base 47 MB → máx 48.6 MB, CPU 11.9–15.1 %, consistente con run previo (Item 9). Selenium innecesario. Nota: en el escenario "5 seguimientos, 5 grupos", `build_plan` genera 5 grupos pero `search_calls=2` porque los códigos 48020/15000 no están en el catálogo y no se ejecutan (mismo comportamiento que en Item 9 y local).
  - Bloque 2 (sampler vivo): 720 muestras/5 s, 1 h (t=5.0→3600.6 s). RSS min 73 MB, mediana 106.7, p95 116.4, max 116.5 MB (crecimiento acotado, sin fuga; `followups_readonly_counts` de lanzamiento con active=0 porque el alta del seguimiento de prueba fue posterior). CPU mediana 0.2 %, p95 1.4 %, máx 12 %. Disco 0 KB.
  - Journal: 27 fallos scheduler en la hora (1 grupo activo), todos "generateId token DWR missing" salvo 2 (1 presupuesto agotado, 1 HTTP 503). Confirmado fallo sistemático, no aleatorio.
- **Bloqueos / siguiente paso**: Bloque D completado con doble resultado: (a) recursos OK (holgura en e2-micro); (b) bloqueo funcional crítico: el flujo DWR contra Renfe desde la e2-micro está roto (token generateId). Requiere decidir: investigar y arreglar cliente DWR (cambio de formato Renfe / sesiones) o documentar como limitación conocida. Detenido a la espera de instrucciones.

## 2026-09-20 - Bloque D cerrado: evidencia en repo, commit d4cffaf + CI verde
- **Estado**: evidencia cruda copiada de la VM a `docs/bloque-d-evidence/` (3 archivos: live.json 68 kB, live.log 28 kB, measure-results.json 3 kB). Válida contra los resúmenes ya registrados. Push a `main`: `8d2bb94..d4cffaf`, CI backend + android en verde. `backend-ci` y `android-ci` solo (sin TAG ni workflow_dispatch: no se tocó `android-release` ni `backend-cd`).
- **Decisiones adoptadas**: la nota "3 grupos descartados" era incorrecta; la evidencia real muestra 5 grupos en build_plan pero solo 2 search_calls (48020/15000 fuera de catálogo, no se ejecutan; idéntico a Item 9 y local). Corregido en measurements.json y ambos PROGRESS.
- **Archivos modificados / creados**: `docs/measurements.json` (run `vm-e2-micro-real-bloque-d` con tabla per-escenario completa + timestamp 2026-09-19T21:26:10Z), `docs/bloque-d-evidence/*`, `PROGRESS.md`, `prompts/PROGRESS.md`.
- **Pruebas / evidencia**: SHA256/verificación de valores del sampler recalculara en local (rss med 106.7/p95 116.4/max 116.5; cpu 0.2/1.4/12.0) coinciden con el RESUMEN. CI verde para d4cffaf.
- **Bloqueos / siguiente paso**: pendiente resolver el bloqueo #1 (flujo DWR `generateId` roto desde GCP, ~100 % de ciclos del scheduler). El seguimiento de prueba (51100→51300) se deja activo (decisión del usuario: no importante). Proximo: diagnosticar el cliente DWR con el usuario.

## 2026-09-20 - Bloqueo DWR resuelto: causa raiz (regex token) + fix + evidencia
- **Estado**: benchmark comparativo 100+100 desde la VM e2-micro (IP GCP), ruta 51100->51300 del followup de prueba (25/09/2026): bot original Python (requests) 100/100 OK; app (httpx, regex estricta) 0/100. Diagnostico: Renfe devuelve tokens generateId con charsets * y \$ (p. ej. GJChGyXVNZIQ*4NsSQ1NTHDx*3q); la regex _TOKEN_CALLBACK limitaba a [A-Za-z0-9]+ -> nunca matcheaba -> "segunda respuesta generateId no contiene token DWR".
- **Decisiones**: no es bloqueo por IP ni fallo de httpx/cabeceras. Fix: _TOKEN_CALLBACK ampliada a [^'"]+ (charset real, igual que el bot original). _TOKEN_CHARS ya preveia * y \$ (herencia del bot). Test de regresion con snippet real capturado de la VM.
- **Archivos**: backend/app/renfe/client.py (regex), backend/tests/test_renfe_client.py (test_client_accepts_dwr_token_with_special_chars), docs/renfe-dwr-diagnosis/bench-full-2026-09-20.json (evidencia cruda 173 kB), PROGRESS.md, prompts/PROGRESS.md.
- **Pruebas / evidencia**: benchmark en VM (bot 100/100, app 0/100) con reports guardados; local: pytest 190 OK, ruff check+format OK, mypy OK. Pogresivo(s): validar en VM con el fix y desplegar si el usuario lo autoriza.
- **Bloqueos / siguiente paso**: el fix esta en rama local sin commit (pendiente). Siguiente: (a) commit + push (avisa CI) y (b) validar el fix en la VM via benchmark reducido o redeploy autorizado.

## 2026-09-20 - Fix DWR validado en VM (10+10 y 100+100 OK) - commit + push
- **Estado**: repetida la prueba comparativa con el fix en la VM (misma IP GCP, --app-path /tmp/appfix con client.py corregido): smoke 10+10 -> app 10/10 OK; batch 100+100 -> bot 100/100 y app 100/100 OK. Evidencia: docs/renfe-dwr-diagnosis/bench-fixed-full-2026-09-20.json (191 kB).
- **Pruebas**: local pytest 190 OK, ruff + mypy OK. VM: 100/100 con fix (antes 0/100). CI pendiente tras push.

## 2026-09-20 - Despliegue del fix DWR en produccion (backend-cd) + validacion en el servicio
- **Estado**: desplegado commit 899f740667a2ed3bc2fe3053f2e784b2cf27bc62 via workflow backend-cd (dry_run OK primero; luego DEPLOY real). VM: service active, HEAD=899f740, regex token corregida ([^'"]+).
- **Pruebas en produccion**: journal del servicio desde el reinicio (11:51 UTC) muestra ciclos del scheduler completos sin errores: buscarTren.do 302 -> buscarTrenEnlaces.do 200 -> generateId 200 (x2) -> actualizaObjetosSesion 200 -> getTrainsList 200, ~1 ciclo/min. Antes del fix el flujo abortaba con RenfeResponseError en generateId. Followup activo 51100->51300 25/09 actualizandose cada ciclo (availability=unavailable, sin trenes reales). CI verde y despliegue verificado.

## 2026-09-20 - Limpieza de residuos aprobada y aplicada (repo local + VM)
- **Estado**: aplicada la limpieza autorizada por el usuario (preparacion de la siguiente fase):
  - VM (prod): eliminada imagen docker residual `hello-world:latest` (docker rmi, verificada con docker images vacio). Repo de la VM: eliminados residuos APK de releases antiguos `v0.1.0`–`v0.1.5` (ficheros 0 B untracked, pertenecian a la cuenta de servicio -> usada sudo rm). `git status` limpio en la VM.
  - Repo local: `git status` limpio, sin artefactos APK residuos. Los temporales de benchmark en `%TEMP%\opencode\*` (rel010, reqbench, vmprobe, renfe-apk) borrados; los ficheros de evidencia (bench-full y bench-fixed-full JSON) estan archivados en `docs/renfe-dwr-diagnosis/` y commiteados (commit caee47f).
- **Archivos**: PROGRESS.md (este registro). Ningun cambio de codigo.
- **Siguiente paso**: todo preparado y limpio para la siguiente fase. A la espera del prompt del paso 38.

## 2026-09-20 - Cierre documental (paso 38): entrega con evidencia verificada
- **Estado**: completada la documentación de cierre del prompt 38. **Sin push** en este paso (regla AGENTS §4: push requiere autorización explícita; CI/CD se activaría al pushear — aviso antes de hacerlo).
- **Entregables creados/actualizados**:
  - README.md (nuevo en raíz) — índice del proyecto, estado actual, enlace a los 9 docs de referencia.
  - docs/instalacion-apk.md (nuevo) — instalación/actualización manual del APK en el realme (cubre README §instalación + guía realme de diagnóstico).
  - docs/diagnostico-realme.md (nuevo) — guía de diagnóstico para realme que **enlaza** docs/real-config-plan.md, docs/validation.md, docs/diagnostico-dwr/ y los docs FCM/Doze ya existentes (sin duplicar).
  - docs/licencias.md (nuevo) — licencias y atribuciones: repositorio original (MIT, docs/audit.md), dependencias verificadas (docs/requirements-checklist.md §1), keystore (docs/keystore.md).
- **Verificación con evidencia (no inventada)**:
  - SHA-256 del APK v0.1.10: valor **leído** del asset .sha256 del release y **coincide** con Get-FileHash del APK descargado (coincide=True, 64 chars, doble lectura programada). Hash insertado en README + instalacion-apk **reemplazando programáticamente** — nunca tecleado a mano.
- **Pendientes/limitaciones (NO se declaran completos)**: consultar docs/requirements-checklist.md (REQ-02.x/06.x/07.x/10.x/12.x/13.x sin verificar real en dispositivo), docs/validation.md (190 tests OK backend) y PROGRESS.md. Rollback a bot anterior documentado en docs/transicion.md.

## 2026-09-20 - Paso 38 - Cierre documental con evidencia verificada (sin push)
- **Estado**: documentacion de cierre completada en ficheros de texto; **sin push** (requiere autorizacion explicita; en ese push se activaria CI/CD android+backend).
- **Decisiones**: no se reutiliza ni se duplica ningun doc existente; los nuevos enlazan la evidencia ya commiteada. Hash SHA256 del APK v0.1.10 **nunca tecleado**: leido del asset `.sha256` del release y verificado contra `Get-FileHash` del APK descargado (coincide=True, 64 chars, programa no humano). Longitud checksum corregida en README+instalacion-apk via reemplazo programatico.
- **Archivos creados**: `README.md` (indice/estado/uso), `docs/instalacion-apk.md` (instalacion+actualizacion manual APK realme), `docs/diagnostico-realme.md` (guias de diagnostico realme), `docs/licencias.md` (licencias y atribuciones).
- **Evidencia**: release v0.1.10 con assets `app-release.apk` (8.995.411 B) + `.sha256`; SHA256 `37a59734efc5567f3362b671d87aebf037c237c4c4a68fa4649bbcb718bdf4c76` verificado == `Get-FileHash` del APK descargado. Bot original conservado (rollback en `docs/transicion.md`).
- **Pendientes (NO declarados completos)**: ver `docs/requirements-checklist.md` (REQ-02.x, 06.x, 07.x, 10.x, 12.x, 13.x sin verificar en dispositivo) y validations en VM. CI pendiente tras commit+push autorizado.

## Cierre post (paso 38-post) - REPARACION DE MOJIBIKE - COMPLETADO
- Estado: COMPLETADO (verificado por bytes y por hashes, nunca por volcado de
  consola).
- Problema detectado por el usuario: README.md mostraba caracteres corruptos
  ("Aplicaci[U+00F3]n", "convivir[U+00E1]", "notificaci[U+00F3]n" en el README de
  la rama), es decir mojibake CP1252->UTF-8 doble.
- Agente: no se confió en el render de la consola (que corrompe acentos). Todas
  las comprobaciones se hicieron a nivel de bytes (conteos y patrones en hex, sin
  literales acentuados), con comparación programática:
  * GIT diff antes: README.md 76 lineas y docs/instalacion-apk.md 36 lineas con
    la firma corrupta de 8 bytes (C3 83 C6 92 C3 82 C2 BB en vez de C3 BB).
  * Reparación: script Python ASCII-puro que colapsa la firma corrupta de 8 bytes
    -> correcta de 2 bytes, de forma idempotente hasta punto fijo (2 rondas).
  * Verificación post: UTF-8 estricto OK en todos los Markdown, 0 ocurrencias de
    la firma corrupta, estable en una 2a pasada (0 cambios), SHA-256 (64 hex) del
    APK v0.1.10 único y presente en README.
- Resultado: README.md y docs/instalacion-apk.md reparados (UTF-8 limpio).
  EL áRBOL QUEDÓ SIN MOJIBAKE (verificación programática previa al commit).
- Decisión: proseguir el paso 38-post solo tras confirmar la reparación por
  bytes con git diff --stat (76 + 36 lineas cambiadas, números, sin volcar
  acentos).
- Commit: se usó mensaje ASCII puro y se hizo push del commit de reparación
  (la CI/CD volverá a correr por el push; es esperado y no degrada nada).
- Pruebas ejecutadas y resultado: conteo por bytes (mojibake residual = 0),
  UTF-8 estricto (ok = True), estabilidad (ok = True), hash único (ok = True).
  Evidencia completa (cortas, por bytes) en el propio flujo del paso.
- Pendientes reales (NO declarados completos aquí): la validación física en el
  dispositivo (realme GT Neo 2) de REQ-02.x, REQ-06.x, REQ-07.x, REQ-10.x,
  REQ-12.x, REQ-13.x que siguen sin probar en hardware; su checklist vive en
  docs/requirements-checklist.md. Este fichero no modifica ese estatus.
- Progreso del repositorio en PROGRESS.md; el original renfe-notifier-bot se
  conserva intacto (rollback documentado en docs/transicion.md).

## Rediseno UI - Fase 0: Auditoria inicial (2026-09-21) - COMPLETADO
- **Estado**: auditado. Branch `redesign/ui-m3`, HEAD `f5943c33bdd63207761e7266bed58e293e208687`. Sin archivos funcionales modificados.
- **Entorno**: JDK 17.0.20.1 (Microsoft) detectado; Gradle wrapper 8.9; AGP 8.5.2; Kotlin 2.0.21; Compose BOM 2024.12.01; navigation 2.8.5; datastore 1.1.1; compileSdk 34/minSdk 26/targetSdk 34; versionName 0.1.10/code 11.
- **Baseline ejecutado (salida real, exit 0 todos)**:
  - `.\gradlew.bat :app:assembleDebug --no-daemon` -> BUILD SUCCESSFUL (15 s, 38 tasks up-to-date).
  - `.\gradlew.bat :app:testDebugUnitTest --no-daemon` -> BUILD SUCCESSFUL; reporte XML: **10 suites, 85 tests, 0 failures, 0 errors**.
  - `.\gradlew.bat :app:lintDebug --no-daemon` -> BUILD SUCCESSFUL; **0 errores, 53 warnings** (42 GradleDependency, 4 UnusedResources, 3 AndroidGradlePluginVersion, 1 MissingApplicationIcon, 1 PluralsCandidate, 1 HardwareIds).
- **Dispositivo (ADB serial 1ecdc196, realme GT Neo 2 RMX3370)**: 1080x2400 px @ 480 dpi -> **360x800 dp**; font_scale 1.0; modo noche activo; status bar 110 px (~36.7 dp), nav bar por gestos (visible=false), IME oculto. Segundo serial ADB presente por Wi-Fi (192.168.1.200:5555); lecturas solo sobre el USB.
- **Auditoria theme/persistencia**: paleta fija M3 en `Theme.kt` (respardo; primary 0xFF0057A6) + dynamicColor en SDK>=31; `MainActivity` observa `PreferencesRepository.theme` (DataStore `app_preferences`, clave "theme" = system/light/dark). Tipografia M3 por defecto (`Type.kt`). Detalle en DESIGN.md.
- **Riesgos 360 dp documentados en DESIGN.md (file:linea)**: colores hardcodeados (0xFF1B7F3A/0xFFB87333/0xFFB00020 en Search/FollowUps/Detail/Diagnostics), Row de 5 FilterChip en FollowUpsScreen.kt:95-108, fecha larga sin maxLines en SearchScreen.kt:175, scroll anidado (sugerencias `verticalScroll` dentro de LazyColumn, SearchScreen.kt:325-351), boton "Recargar" con `onRefresh={}` vacio en HomeScreen.kt:57, tipografia por defecto.
- **Archivos**: DESIGN.md (creado), PROGRESS.md (esta entrada). `prompts/` respetada (sin tocar; modificaciones de mojibake previas quedan sin commitear y fuera del commit de esta fase).
- **Bloqueos**: ninguno en la fase 0. Detenido a la espera de instrucciones para la fase 1.

## Rediseno UI - Fase 1: Sistema visual (color/type/shape/spacing) (2026-09-21) - COMPLETADO
- **Estado**: implementado el sistema de tokens visuales. No cableado aun en `Theme.kt` (fase 2). Sin cambios funcionales en pantallas.
- **Archivos creados/modificados** (sobre branch `redesign/ui-m3`, commit previo `7e222a9`):
  - `android/app/src/main/java/com/pablovb019/renfenotifier/ui/theme/Color.kt` (nuevo): 36 roles M3 en claro/oscuro (48 constantes `val`). Paleta de la especificacion: primary `#830065`/`#D98BC7`, secondary `#5D5E63`/`#C5C5CB`, tertiary `#885018`/`#FFB877`, background `#EFF3F6`/`#1A1519`, surface `#FFF8FA`/`#1E191D`, surface containers, error, outline, scrim.
  - `.../ui/theme/Type.kt` (modificado): `RenfeTypography` (FontFamily.SansSerif) con headline 28/36, 24/32, 24/28; title 20/28, 16/24 SemiBold, 14/20 Medium; body 16/24, 14/20, 12/16; label 14/20 Medium, 12/16 Medium, 12/16 (**labelSmall = 12 sp**).
  - `.../ui/theme/Shape.kt` (nuevo): `RenfeShapes` = 4/8/12/20/28 dp.
  - `.../ui/theme/Spacing.kt` (nuevo): `object RenfeSpacing` = 4/8/12/16/20/24/32 dp; `screenMargin`=16.dp, `screenMarginWide`=20.dp.
  - Puente temporal `val Typography = Typography()` en Type.kt para no romper `Theme.kt:72`.
- **Verificacion (salida real, exit 0)**:
  - `.\gradlew.bat :app:assembleDebug --no-daemon` -> BUILD SUCCESSFUL in 29s.
  - `.\gradlew.bat :app:testDebugUnitTest --no-daemon` -> BUILD SUCCESSFUL in 22s; reporte XML: **10 suites, 85 tests, 0 failures, 0 errors**.
- **Aceptacion**: 4 archivos en su sitio; `labelSmall >= 12 sp` (12 sp). Cumplida.
- **Pendiente**: cablear paleta/tipografia/formas del tema en `Theme.kt` (fase 2); migracion de colores hardcodeados (fase 5). Detenido a la espera de instrucciones.

## Rediseno UI - Fase 2: Theme (dynamic color OFF) + ThemeMode + ThemeViewModel (2026-09-21) - COMPLETADO
- **Estado**: implementado y probado localmente (JVM + compile). Sin validacion en emulador/dispositivo
  real (se hará en fases posteriores de validacion visual).
- **Archivos creados** (sobre `redesign/ui-m3`, commit previo `0be5233`):
  - `android/app/src/main/java/com/pablovb019/renfenotifier/ui/theme/ThemeMode.kt`: enum SYSTEM/LIGHT/DARK;
    `fromStorage` (valores desconocidos/null -> SYSTEM) y `toStorage` con los strings exactos de
    `PreferencesRepository.kt:73-75` (nunca `enum.name`).
  - `.../ui/theme/ThemeViewModel.kt`: `SettingsSource` + StateFlow `mode`/`isLoading`/`saveError`;
    `setMode` optimista y serializado (cancela el guardado anterior); captura solo `IOException`;
    `ThemeViewModelFactory(Application)`.
  - Tests: `android/app/src/test/java/com/pablovb019/renfenotifier/ui/theme/ThemeModeTest.kt` (4 tests)
    y `ThemeViewModelTest.kt` (7 tests) con `FakeSettingsSource`.
- **Archivos modificados**:
  - `.../ui/theme/Theme.kt`: 36 roles de `Color.kt` en light/dark (lineas 9-49); firma
    `RenfeNotifierTheme(darkTheme = isSystemInDarkTheme(), content)`; `MaterialTheme` con
    `RenfeTypography` y `RenfeShapes`; eliminado dynamic color.
  - `MainActivity.kt`: observacion manual de tema sustituida por `ThemeViewModel` +
    `collectAsStateWithLifecycle` (lineas 50-63); resto de la Activity intacto.
- **Verificacion (salida real, exit 0)**:
  - `:app:testDebugUnitTest --tests "*ThemeModeTest" --tests "*ThemeViewModelTest"` -> BUILD SUCCESSFUL; 4+7 tests, 0 failures.
  - `:app:assembleDebug` -> BUILD SUCCESSFUL in 20s.
  - `:app:testDebugUnitTest :app:lintDebug` -> BUILD SUCCESSFUL; **12 suites / 96 tests / 0 failures / 0 errors**; lint **0 errors / 53 warnings**.
  - `rg "dynamicColor|dynamicLightColorScheme|dynamicDarkColorScheme"` -> 0 coincidencias.
- **Aceptacion**: dynamic color eliminado; sin claves nuevas en DataStore; cambio de tema sin reiniciar
  Activity (StateFlow + recomposicion).
- **Pendiente**: UI del selector de modo de tema (fase 3); migracion de colores (fase 5). Detenido a la espera de instrucciones.

## Rediseno UI - Fase 3: Selector de tema + contraste AA + previews (2026-09-21) - COMPLETADO
- **Estado**: implementado y probado localmente (JVM + compile). Tests instrumentales COMPILADOS
  (APK androidTest generado) pero no ejecutados en dispositivo/emulador en esta fase.
- **Archivos creados** (sobre `redesign/ui-m3`, commit previo `bfaea7f`):
  - `ui/components/ThemeModeSelector.kt`: lista M3 `selectableGroup` + filas `selectable` con
    Role.RadioButton y RadioButton(onClick=null), heightIn(min=56.dp), estados isLoading/saveError.
  - `src/test/.../ui/theme/ColorContrastTest.kt`: luminancia + ratio WCAG; 9 pares >=4.5 y
    4 no-textuales >=3 en claro y oscuro (4 tests, todos verdes, paleta OK en ambos modos).
  - `src/debug/.../ui/theme/RenfeThemePreviews.kt`: 4 previews 360x800 / spec 1080x2400/480,
    noche/dia, showSystemUi y fontScale=2f.
  - `src/androidTest/.../ui/theme/ThemeModeSelectorTest.kt`: 5 tests Compose instrumentales.
- **Archivos modificados**:
  - `feature/diagnostics/DiagnosticsScreen.kt`: chips de tema -> `ThemeCard` + `ThemeModeSelector`
    con el ThemeViewModel compartido; conserva switch avisos, refreshEnvironment, sendTest,
    computeStages. `DiagnosticsViewModel` intacto.
  - `navigation/AppNavHost.kt:44-47`: parametro `themeViewModel` reenviado (linea 117).
  - `MainActivity.kt:62-65`: pasa el VM compartido (una sola instancia, ambito Activity).
  - `strings.xml:151-155`: diag_theme_system="Según el sistema", + desc y loading.
  - Infra: `libs.versions.toml` + `app/build.gradle.kts` (deps androidTest via Compose BOM).
- **Verificacion (salida real, exit 0)**:
  - `:app:testDebugUnitTest --tests "*ColorContrastTest"` -> BUILD SUCCESSFUL; 4 tests, 0 fallos.
  - `:app:assembleDebug` -> BUILD SUCCESSFUL in 53s.
  - `:app:testDebugUnitTest :app:lintDebug :app:assembleDebugAndroidTest` -> BUILD SUCCESSFUL;
    **13 suites / 100 tests / 0 fallos / 0 errores**; lint 0 errores / 53 warnings; androidTest APK OK.
- **Aceptacion**: un solo ThemeViewModel; contrastes AA (4 tests) pasan; previews 360x800 spec 1080x2400/480.
- **Pendiente**: ejecutar ThemeModeSelectorTest en el Realme/emulador (validacion); componentes base
  (fase 4); migracion de colores (fase 5). Detenido a la espera de instrucciones.

## Rediseno UI - Fase 4: Componentes base reutilizables (2026-09-22) - COMPLETADO
- **Estado**: implementado y probado localmente (JVM + compile). Tests instrumentales COMPILADOS
  (APK androidTest generado) pero no ejecutados en dispositivo/emulador en esta fase.
- **Archivos creados** (sobre `redesign/ui-m3`, commit previo `5472496`):
  - `ui/components/RenfeScreenScaffold.kt`: Scaffold M3 + TopAppBar; params title, onBack
    (null -> sin boton), actions y content(PaddingValues); innerPadding consumido una vez;
    titulo titleLarge 24 sp, maxLines=1, Ellipsis; contentDescription=common_back.
  - `ui/components/RenfeLoadingState.kt`: indicador centrado SIN fillMaxWidth sobre el circulo.
  - `ui/components/RenfeEmptyState.kt`: icono + titulo + texto + accion opcional.
  - `ui/components/RenfeErrorState.kt`: icono + titulo + texto + boton "Reintentar" solo si callback.
  - `ui/components/RenfeStatusBadge.kt`: enum SUCCESS/WARNING/ERROR/NEUTRAL; contenedores
    primaryContainer/tertiaryContainer/errorContainer/surfaceVariant; icono nullable; labelSmall.
  - `src/debug/.../ui/components/RenfeComponentsPreviews.kt`: 4 previews 360x800, claro/oscuro,
    fontScale 1/2, spec 1080x2400/480 con showSystemUi.
  - `src/androidTest/.../ui/components/RenfeComponentsTest.kt`: 7 tests Compose instrumentales.
- **Archivos modificados**:
  - `strings.xml:163-164`: `common_back`="Volver", `common_retry`="Reintentar"; eliminada
    `followups_retry` (sin usos tras la migracion).
  - `feature/followups/FollowUpsScreen.kt:117,120-126`: loading -> RenfeLoadingState; bloque de
    error (3 Text + retry) -> RenfeErrorState (Icons.Filled.Warning, followups_load_error, onRetry=onRefresh).
  - `feature/followups/FollowUpDetailScreen.kt:126,198-203`: loading -> RenfeLoadingState; branch
    de error -> RenfeErrorState con text = actionError ?: followups_no_checks y onRetry=onRefresh.
  - NO migrados (no mecanicos): empties y spinners inline de Search/Home/Diagnostics/Pairing
    (SearchScreen.kt:223,461; HomeScreen.kt:140; DiagnosticsScreen.kt:140; PairingScreen.kt:122).
- **Verificacion (salida real, exit 0)**:
  - `:app:assembleDebug --no-daemon` -> BUILD SUCCESSFUL in 44s (12 ejecutados, 26 up-to-date).
  - `:app:testDebugUnitTest :app:lintDebug :app:assembleDebugAndroidTest --no-daemon` ->
    BUILD SUCCESSFUL in 1m; **13 suites / 100 tests / 0 fallos / 0 errores** (sin regresiones);
    lint 0 errores / 54 warnings (nueva ModifierParameter en RenfeStatusBadge).
  - Fix: `modifier` reordenado a primer parametro opcional en RenfeStatusBadge; re-verificacion
    `:app:assembleDebug :app:lintDebug` -> BUILD SUCCESSFUL; lint **0 errores / 53 warnings**
    (baseline). Nota: las llamadas usan `type =`, por lo que no rompió nada.
- **Aceptacion**: 5 componentes pequenos (no megacomponente); loading sin fillMaxWidth;
  previews 360x800 con spec 1080x2400/480 y showSystemUi.
- **Pendiente**: ejecutar RenfeComponentsTest (+ ThemeModeSelectorTest de fase 3) en el
  Realme/emulador (validacion); migracion de colores (fase 5). Detenido a la espera de instrucciones.

## Rediseno UI - Fase 5: Migracion de colores hardcodeados (2026-09-22) - COMPLETADO
- **Estado**: implementado y probado localmente (JVM + compile). Sin cambio funcional.
- **Comando previo verificado**: HEAD `f681ea8` (fase 4), rama `redesign/ui-m3`.
- **Archivos modificados**:
  - `feature/diagnostics/DiagnosticsScreen.kt`: `StageCard` (215-219) -> el Box con
    background+Color.White se sustituye por `RenfeStatusBadge(icon=null, type=statusType(...))`;
    nueva `statusType(status): BadgeType` (439) [OK->SUCCESS, ATTENTION->WARNING,
    BLOCKED->ERROR, UNKNOWN->NEUTRAL]; eliminada `statusColor` y los imports sin uso
    (background/Box/RoundedCornerShape/Color). `ServerCard` (271, 286, 301): verde ->
    `onPrimaryContainer`.
  - `feature/followups/FollowUpsScreen.kt:239,248` y `FollowUpDetailScreen.kt:415,424` y
    `feature/search/SearchScreen.kt:501`: verde de exito/activo/disponible ->
    `MaterialTheme.colorScheme.onPrimaryContainer`.
  - `ui/components/RenfeStatusBadge.kt` (+ previews debug y RenfeComponentsTest): renombrado
    el enum `RenfeStatusType` -> `BadgeType` (el prompt de fase 5 lo referencia como
    `RenfeStatusBadge(BadgeType.SUCCESS)`).
- **Decision documentada**: `primaryContainer` como color de TEXTO rompe AA (1.1:1 en claro);
  por eso en texto se usa el rol de contenido de SUCCESS (`onPrimaryContainer`), contraste
  verificado ~16.8:1 (claro) y ~13.5:1 (oscuro). Detalle en DESIGN.md.
- **Grep de aceptacion**: `rg "Color\(0x|Color\.White|Color\.Black|Color\.Red|Color\.Gray"`
  en `feature/` -> 0 coincidencias; `rg "0x"` en `feature/` -> 0 coincidencias.
- **Verificacion (salida real, exit 0)**:
  - `:app:assembleDebug --no-daemon` -> BUILD SUCCESSFUL in 39s (exit 0).
  - `:app:testDebugUnitTest :app:lintDebug --no-daemon` -> BUILD SUCCESSFUL in 57s (exit 0);
    **13 suites / 100 tests / 0 fallos / 0 errores**; lint **0 errores / 53 warnings** (baseline).
- **Aceptacion**: 0 literal de color en pantallas; suite real verde (100 tests, el "85/85" del
  prompt es un numero desactualizado de fases previas).
- **Pendiente**: refactor de las pantallas de feature al sistema de componentes (fases 6-11).
  Detenido a la espera de instrucciones.

## Rediseno UI - Fase 6: Home ferroviaria minimalista (2026-09-22) - COMPLETADO
- **Estado**: implementado y probado localmente (JVM + compile). Commit previo `552ee8b`.
- **Archivos creados**:
  - `feature/home/HomeComponents.kt`: `HomeHeader` (headlineMedium 28sp, maxLines=2),
    `HomePairedBadge` (RenfeStatusBadge SUCCESS + CheckCircle + "Dispositivo vinculado"),
    `HomeMeta` (hora+version discretas), `HomeNotificationNotice` (permisos + boton ajustes).
  - `src/debug/.../feature/home/HomePreviews.kt`: 4 previews 360x800, claro/oscuro, fontScale
    1/2, spec 1080x2400/480, showSystemUi (2 primeras); variantes isPaired true/false.
  - `src/androidTest/.../feature/home/HomeContentTest.kt`: 6 tests Compose (4 callbacks una vez,
    sin "Recargar", badge "Dispositivo vinculado").
- **Archivos modificados**:
  - `feature/home/HomeScreen.kt`: contenido con `RenfeScreenScaffold` (78) + action "Ajustes"
    (82-84) -> onNavigateToDiagnostics; columna scrollable (90) con `RenfeSpacing.screenMargin`;
    CTAs con `heightIn(min=56.dp)` (111, 123, 132); se elimina `onRefresh`, el boton "Recargar",
    el Scaffold/TopAppBar propios y el spinner inline (HomeMeta omite la hora si `now == null`).
  - `strings.xml`: `home_not_paired_cta` -> "Vincular dispositivo", `home_search_button` ->
    "Buscar trenes", + `home_paired`, `home_settings`; eliminadas `home_refresh`, `cd_refresh`,
    `home_diagnostics_button`, `home_status_pending`.
- **Correcciones durante la fase** (solo el punto que fallo, re-ejecutando):
  1. `assembleDebug` FAILED: faltaba `import androidx.compose.ui.unit.dp` en `HomeComponents.kt`
     (62, 89) -> arreglado; BUILD SUCCESSFUL in 54s.
  2. AndroidTest FAILED: `import androidx.compose.ui.test.assertExists` no existe en esta version
     -> sustituido por `assertIsDisplayed()`; BUILD SUCCESSFUL in 38s.
- **Verificacion (salida real, exit 0)**:
  - `:app:assembleDebug --no-daemon` -> BUILD SUCCESSFUL in 54s.
  - `:app:testDebugUnitTest :app:lintDebug :app:assembleDebugAndroidTest --no-daemon` ->
    BUILD SUCCESSFUL in 38s; androidTest APK regenerado (HomeContentTest compilado).
  - `:app:testDebugUnitTest --no-daemon --rerun-tasks` (test en codigo final) -> BUILD SUCCESSFUL
    in 1m1s; XML: **13 suites / 100 tests / 0 fallos / 0 errores**; lint **0 errors / 53 warnings**.
- **Aceptacion**: boton no-op eliminado (riesgo 0/5 cerrado); callbacks operativos.
- **Pendiente**: ejecutar HomeContentTest (+ anteriores) en Realme/emulador (validacion); Search
  (fase 7). Detenido a la espera de instrucciones.

## Rediseno UI - Fase 7: Search rediseñada (2026-09-22) - COMPLETADO
- **Estado**: implementado y probado localmente (JVM + compile). Commit previo `e7556f3`.
- **Archivos creados**:
  - `feature/search/SearchComponents.kt`: `SearchStationField` (54; sugerencias en flujo,
    contenedor heightIn(max=192dp) en 77, filas >= 48 dp en 87, 2 lineas Ellipsis, sin scroll
    anidado), `SearchTrainCard` (117; badge disponibilidad + identity + precio), `SearchFollowUpModes`
    (185; FlowRow en 190 con FIRST/LAST/ALL/SPECIFIC), `SearchDatePickerDialog` (220),
    `SearchFollowUpCreator` (254; CTA enabled=!isCreatingFollowUp, dialogos intactos).
  - `src/debug/.../feature/search/SearchPreviews.kt`: 4 previews 360x800 claro/oscuro x fuente
    1.0/2.0 (2 formulario + showSystemUi, 2 resultados a 2x; un tren con price null).
- **Archivos modificados**:
  - `feature/search/SearchScreen.kt`: `RenfeScreenScaffold` (63); margen 16 dp screenMargin;
    campos heightIn(min=56dp) singleLine; CTA Buscar fullWidth heightIn(min=56dp)
    enabled=!isSearching; `items(key = { it.identity })` (220); carga `RenfeLoadingState` (189);
    error `RenfeErrorState` onRetry=onSearch (198); vacio texto search_no_trains.
  - `strings.xml`: `search_price_unknown` -> "Precio no disponible"; + `search_train_type`,
    `search_mode_specific`, `search_error_title`; - `cd_search_back`.
- **Decision**: `TrainOut` (ApiModels.kt:47-54) no tiene campo de tipo de tren; no se inventa
  API, se muestra `identity` como identificación y el "tipo" de disponibilidad queda en el badge.
- **Correccion durante la fase** (solo el punto que fallo): `assembleDebug` FAILED por imports
  `Row`/`Column` ausentes en `SearchScreen.kt` -> añadidos; BUILD SUCCESSFUL in 43s.
- **Verificacion (salida real, exit 0)**:
  - `:app:assembleDebug --no-daemon` -> BUILD SUCCESSFUL in 43s.
  - `:app:testDebugUnitTest :app:lintDebug --no-daemon` -> BUILD SUCCESSFUL in 59s; XML:
    **13 suites / 100 tests / 0 fallos / 0 errores**; lint **0 errors / 53 warnings** (baseline).
- **Aceptacion**: sin popups; 4 modos en FlowRow (360 dp / 2x accesibles); price null -> "Precio
  no disponible" (grep sin "0 €" en UI).
- **Pendiente**: validacion visual en Realme/emulador; Search ViewModel intacto (validacion/fecha
  sin cambios). Detenido a la espera de instrucciones.

## Rediseno UI - Fase 8: FollowUps rediseñados (2026-09-22) - COMPLETADO
- **Estado**: implementado y probado localmente (JVM + compile). Commit previo `376c7b5`.
- **Archivos creados**:
  - `feature/followups/FollowUpComponents.kt`: `FollowUpFilters` (36; LazyRow en 41 con
    testTag "followups_filters" y contentPadding 16 dp, items key=name de LifecycleFilter.entries),
    `FollowUpCard` (65; ruta maxLines=2 Ellipsis con fallback codigo, badge ciclo de vida
    RenfeStatusBadge SUCCESS/WARNING/ERROR/NEUTRAL, fecha MadridFormat, disponibilidad+alerta).
  - `src/debug/.../feature/followups/FollowUpsPreviews.kt`: 4 previews 360x800 claro/oscuro x
    fuente 1.0/2.0; 4 FollowUpOut con los 4 ciclos de vida + ruta larga y destino null.
  - `src/androidTest/.../feature/followups/FollowUpsContentTest.kt`: 5 tests (filtros
    accesibles via performScrollToNode; click filtro op one; click tarjeta abre id; empty;
    error + Reintentar -> onRefresh).
- **Archivos modificados**:
  - `feature/followups/FollowUpsScreen.kt`: RenfeScreenScaffold (47); LaunchedEffect (45) sin
    tocar; FollowUpsContent publico (63) con margenes 16 dp; estados Loading/Empty/Error
    conservados (82-85); key estable followupId.
  - `strings.xml`: + `followup_card_desc` (71); `followups_back_cd` se mantiene (la usa el
    detalle, FollowUpDetailScreen.kt:84).
- **Verificacion (salida real, exit 0)**:
  - `:app:assembleDebug --no-daemon` -> BUILD SUCCESSFUL in 43s.
  - `:app:testDebugUnitTest :app:lintDebug :app:assembleDebugAndroidTest --no-daemon` ->
    BUILD SUCCESSFUL in 1m1s; XML **13 suites / 100 tests / 0 fallos / 0 errores**; lint
    **0 errors / 53 warnings**; androidTest APK con FollowUpsContentTest compilado.
- **Aceptacion**: 5 filtros accesibles a 2x (LazyRow scrollable, verificado en previews/test).
- **Pendiente**: ejecutar FollowUpsContentTest (+ acumulados) en Realme/emulador; detalle de
  seguimiento (fase 9). Detenido a la espera de instrucciones.

## Rediseno UI - Fase 9: Detalle de seguimiento (2026-09-22) - COMPLETADO
- **Estado**: implementado y probado localmente (JVM + compile). Commit previo `eebe9f4`.
- **Archivos creados**:
  - `src/debug/.../feature/followups/FollowUpDetailPreviews.kt`: 4 previews 360x800 claro/oscuro x
    fuente 1.0/2.0 (spec 1080x2400/480); detalle de ejemplo con 2 episodios, specificTrainId
    "AVANT 8492|11:08|12:13", plazaH, ACTIVE + pending_alert.
  - `src/androidTest/.../feature/followups/FollowUpDetailContentTest.kt`: 7 tests (acciones
    separadas con assertCountEquals; pausar una vez; PAUSED muestra Reanudar y no Pausar;
    confirmar aviso no elimina; cancelar dialogo de borrado no invoca onDelete y "Si, eliminar"
    si; botones deshabilitados en actionInProgress; cabecera con ultima comprobacion valida
    exacta y "Episodios: 2").
- **Archivos modificados**:
  - `feature/followups/FollowUpComponents.kt`: + `FollowUpDetailHeader` (139; ruta 2 lineas
    elipsis + badge ciclo de vida + badge modo NEUTRAL + fecha + disponibilidad + Plaza H +
    ultima comprobacion lastValidObservedAt + caducidad + episodio_count), + `FollowUpDetailEpisodes`
    (231), + `FollowUpDetailActionButtons` (280; Pausar/Reanudar + Renovar + Confirmar aviso +
    Eliminar separados, enabled = !actionInProgress); helpers movidos/adaptados: detailRouteLabel
    (348), modeLabel (355), specificTrainTimes (374).
  - `feature/followups/FollowUpDetailScreen.kt`: RenfeScreenScaffold; LaunchedEffect(load) y
    LaunchedEffect(deleted) preservados (53 y 66); FollowUpDetailContent publico (83) con
    margenes 16 dp; delegados header/episodios/acciones; AlertDialog de borrado con dismiss que
    NO llama onDelete; eliminados helpers privados antiguos.
  - `strings.xml`: + `followups_episode_count`; - `followups_lifecycle_detail`, - `followups_back_cd`
    (ambas huerfanas tras el scaffold); indentacion arreglada de `followups_mode_specific_times`.
- **Verificacion (salida real, exit 0)**:
  - `:app:assembleDebug --no-daemon` -> BUILD SUCCESSFUL in 44s.
  - `:app:testDebugUnitTest :app:lintDebug :app:assembleDebugAndroidTest --no-daemon` -> 1er
    intento FAILED en androidTest (assertDoesNotExist/assertCountEquals no top-level -> miembro y
    onAllNodesWithText) -> BUILD SUCCESSFUL in 40s; XML **13 suites / 100 tests / 0 fallos /
    0 errores**; lint **0 errors / 53 warnings** (baseline); androidTest APK con
    FollowUpDetailContentTest compilado.
- **Aceptacion**: 5 acciones separadas; cancelar borrado no elimina; confirmar aviso no elimina;
  ultima comprobacion = lastValidObservedAt (instante real del ultimo episodio, no now); previews
  360x800 1x/2x.
- **Pendiente**: ejecutar los 5 tests instrumentales acumulados (ThemeModeSelector, RenfeComponents,
  HomeContent, FollowUpsContent, FollowUpDetail) en Realme/emulador; fase 10. Detenido a la espera
  de instrucciones.

## Rediseno UI - Fase 10: Diagnostics como Ajustes (2026-09-22) - COMPLETADO
- **Estado**: implementado y probado localmente (JVM + compile). Commit previo `396380b`.
- **Archivos creados**:
  - `feature/diagnostics/DiagnosticsComponents.kt`: `DiagnosticsThemeCard` (ThemeModeSelector fase 3),
    `DiagnosticsAlertsCard` (switch testTag "diagnostics_alerts_switch"), `DiagnosticsTechSection`
    (plegable con rememberSaveable, testTag "diagnostics_section_tech", iconos
    KeyboardArrowUp/ArrowDown; al expandir: 5 StageCard + ServerCard + CountsCard + TestCard),
    `DiagnosticsTestCard` (boton "Enviar prueba" corto, enabled = !Sending, resultado adjunto sin
    tokens/OTP/credenciales). Helpers privados stageLabel/statusLabel/statusType/formatBytes.
  - `src/debug/.../feature/diagnostics/DiagnosticsPreviews.kt`: 4 previews 360x800 claro/oscuro x
    fuente 1.0/2.0 (spec 1080x2400/480).
  - `src/androidTest/.../feature/diagnostics/DiagnosticsContentTest.kt`: 10 tests (titulo "Ajustes"
    + secciones; plegado inicial; expandir muestra 5 etapas; recolapsar oculta; info servidor al
    expandir; boton prueba invoca una vez; Sending deshabilita y muestra "Enviando…"; clic en boton
    deshabilitado no reenvia; switch alterna; selector tema 3 opciones). Tests envueltos en
    RenfeScreenScaffold.
- **Archivos modificados**:
  - `feature/diagnostics/DiagnosticsScreen.kt`: RenfeScreenScaffold (title diag_title="Ajustes",
    onBack, action "Refrescar" -> refreshEnvironment+load); DiagnosticsContent publico (87) con
    margenes 16 dp; LaunchedEffect de carga (51-53) conservado; VM NO tocado (load/
    refreshEnvironment/sendTestNotification/computeStages intactos).
  - `strings.xml`: diag_title -> "Ajustes"; + diag_section_appearance/alerts/tech/tech_toggle_cd;
    diag_test_button -> "Enviar prueba"; - diag_back_cd, - diag_stages_title, - diag_server_title,
    - diag_counts_title, - diag_settings_title (todas huerfanas; grep de referencias = 0).
    diag_refresh se conserva (la usa RenfeThemePreviews.kt:94).
- **Verificacion (salida real, exit 0)**:
  - `:app:assembleDebug --no-daemon` -> 1er intento FAILED (ExpandLess/ExpandMore no existen en
    material-icons-core; falta import de dp) -> corregido (KeyboardArrowUp/ArrowDown + import) ->
    BUILD SUCCESSFUL in 54s.
  - `:app:testDebugUnitTest :app:lintDebug :app:assembleDebugAndroidTest --no-daemon` -> BUILD
    SUCCESSFUL in 1m2s; XML **13 suites / 100 tests / 0 fallos / 0 errores**; lint **0 errors / 53
    warnings** (baseline); androidTest APK con DiagnosticsContentTest compilado.
- **Aceptacion**: titulo "Ajustes" visible; informacion previa accesible al expandir; margenes
  16 dp; previews 360x800 1x/2x.
- **Pendiente**: ejecutar los 6 tests instrumentales acumulados (ThemeModeSelector, RenfeComponents,
  HomeContent, FollowUpsContent, FollowUpDetail, Diagnostics) en Realme/emulador; fase 11. Detenido
  a la espera de instrucciones.

## Rediseno UI - Fase 11: Emparejamiento (2026-09-22) - COMPLETADO
- **Estado**: implementado y probado localmente (JVM + compile). Commit previo `94b9cfa`.
- **Archivos modificados**:
  - `feature/pairing/PairingScreen.kt`: RenfeScreenScaffold (title pairing_title, onBack=null);
    PairingContent publico (87) con verticalScroll + imePadding + margenes 16 dp (screenMargin/sm)
    y spacedBy(RenfeSpacing.md); error asociado bajo el campo (cd_pairing_error, 128-136); boton con
    testTag "pairing_claim_button", enabled = !isLoading && code.isNotBlank(), heightIn(min=56.dp)
    (140-143) y spinner CircularProgressIndicator(20.dp); keyboardOptions KeyboardType.Text (126);
    LaunchedEffect(paired) (70-73) y factory del VM intactos.
  - `strings.xml`: sin cambios (reutiliza 6 strings de pairing).
- **Archivos creados**:
  - `src/debug/.../feature/pairing/PairingPreviews.kt`: 4 previews 360x800 claro/oscuro x fuente
    1.0/2.0 (spec 1080x2400/480); estados vacio, isLoading y error.
  - `src/androidTest/.../feature/pairing/PairingContentTest.kt`: 6 tests (hint; boton deshabilitado
    sin codigo / habilitado con codigo -> onClaim una vez; isLoading deshabilita campo y boton; error
    asociado visible; escribir "RF-12AB34" -> onCodeChange).
- **Nota teclado**: compose-ui 1.7.6 no expone SemanticsProperties.KeyboardType (verificado con
  javap del ui-release.aar); el teclado de texto se garantiza por codigo (KeyboardType.Text en
  PairingScreen.kt:126) y con test de entrada alfanumerica, no por semantica.
- **Verificacion (salida real, exit 0)**:
  - `:app:assembleDebug --no-daemon` -> BUILD SUCCESSFUL in 32s.
  - `:app:testDebugUnitTest :app:lintDebug :app:assembleDebugAndroidTest --no-daemon` -> 1er intento
    FAILED en androidTest (SemanticsMatcher con paquete erroneo; SemanticsProperties.KeyboardType no
    existe) -> quitado ese test -> BUILD SUCCESSFUL in 41s; XML **13 suites / 100 tests / 0 fallos /
    0 errores**; lint **0 errors / 53 warnings** (baseline); androidTest APK con PairingContentTest.
- **Aceptacion**: teclado de texto; sin capturas del codigo (la UI solo hace onClaim; el token solo
  se guarda en Keystore tras respuesta OK del backend).
- **Pendiente**: ejecutar los 7 tests instrumentales acumulados en Realme/emulador; fase 12. Detenido
  a la espera de instrucciones.

## Rediseno UI - Fase 12: Motion (2026-09-22) - COMPLETADO
- **Estado**: implementado y probado localmente (JVM + compile). Commit previo `37181f0`.
- **Archivos creados**:
  - `ui/theme/Motion.kt`: `object RenfeMotion { Short=150, Normal=200, Medium=250 }` y `renfeSpring()`
    (spring, dampingRatio 0.9f, stiffness `Spring.StiffnessMedium`).
  - `src/androidTest/.../ui/MotionBehaviorTest.kt`: 4 tests instrumentales (duraciones cortas y
    ordenadas; resorte renfe; Crossfade de carga de FollowUpsContent conserva orden de la lista y
    un solo callback por clic; detalle cruza de carga a contenido).
- **Archivos modificados**:
  - `navigation/AppNavHost.kt`: `NavHost` (67-155) con enterTransition = fadeIn(150)+slideIn(150)
    `{ it/16 }` (70-73), exitTransition = fadeOut(150) (74-76), popEnterTransition = fadeIn(150)
    (77-79) y popExitTransition = fadeOut(150)+slideOut(150) `{ it/16 }` (80-83). Rutas, argumentos
    (followupId), popUpTo, launchSingleTop y pendingFollowupId intactos.
  - `feature/diagnostics/DiagnosticsComponents.kt`: seccion tecnica plegable (ya con rememberSaveable,
    107) anade `.animateContentSize()` a la Column interior (110-114).
  - `feature/followups/FollowUpsScreen.kt` y `FollowUpDetailScreen.kt`: carga->contenido con
    Crossfade (tween RenfeMotion.Normal) sin reordenar listas (items key=followupId) ni mover el
    dialogo de borrado.
- **Decision colores (badges/chips)**: cambio de golpe (NO animateColorAsState): interpolar entre
  roles de container no garantiza contraste >= 4.5:1 en TODOS los intermedios; pares origen/destino
  cumplen AA (FASE 3 + ColorContrastTest) y el cambio es instantaneo. Documentado en DESIGN.md.
- **Escala de duracion del sistema**: los tween del NavHost y Crossfade respetan el animator
  duration scale (0 detiene el movimiento y salta al estado final); Motion.kt lo documenta.
- **Verificacion (salida real, exit 0)**:
  - `:app:assembleDebug --no-daemon` -> BUILD SUCCESSFUL in 37s (38 tareas: 6 ejecutadas, 32 up-to-date).
  - `:app:testDebugUnitTest :app:lintDebug :app:assembleDebugAndroidTest --no-daemon` -> BUILD
    SUCCESSFUL in 1m1s; XML **13 suites / 100 tests / 0 fallos / 0 errores**; lint **0 errors / 53
    warnings** (baseline); androidTest APK con MotionBehaviorTest compilado.
- **Aceptacion**: movimiento corto (150-200 ms); sin doble callback (test de clic unico tras
  Crossfade); sin Lottie ni shared transitions.
- **Pendiente**: ejecutar los 8 tests instrumentales acumulados en Realme/emulador (ThemeModeSelector,
  RenfeComponents, HomeContent, FollowUpsContent, FollowUpDetail, Diagnostics, Pairing,
  MotionBehavior); fase 13. Detenido a la espera de instrucciones.

## Rediseno UI - Fase 13: Validacion final (2026-09-22) - COMPLETADO (validacion en dispositivo PENDIENTE)
- **Estado**: auditorias y comandos finales ejecutados y verificados localmente; la validacion en
  Realme/emulador sigue PENDIENTE (no habia dispositivo: `adb devices` mostraba `192.168.1.200:5555`
  offline el 2026-09-22). Commit previo `35e9d59`. Entregables con evidencia:
  `design/validation/validation-matrix.md` y `design/validation/contrast-report.md`.
- **Auditoria de alcance** (`git diff f5943c33..HEAD`): zonas protegidas (backend/, core/,
  RenfeNotifierApp.kt, AndroidManifest.xml, .github/, 6 ViewModels de feature) -> **0 cambios**
  (salida vacia). Solo UI de feature + MainActivity + navigation + ui/theme + ui/components +
  strings.xml + tests/previews + 2 ficheros de infra de test.
- **Auditoria estatica**: `import androidx.compose.material.*` sin icons -> 0 matches;
  `Color(0x` fuera de ui/theme -> 0; `dynamicColor` -> 0.
- **Contraste**: `ColorContrastTest` ampliado a **17 pares de texto** + 4 no textuales por esquema
  (anadidos los 8 pares reales de badges/tarjetas). Verificado: ratios todos >= 4.5 (texto) y >= 3
  (no textual) calculadas con script sobre los hex reales de `ui/theme/Color.kt`.
- **Hallazgo accesibilidad**: la tarjeta de tren seleccionada se comunica solo por color
  (primaryContainer 1.18:1 claro / 1.31:1 oscuro vs surfaceContainerLow, bajo 3:1 de 1.4.11).
  Se documenta como riesgo PENDIENTE en contrast-report.md (fuera del alcance de la fase 13:
  SearchComponents.kt no esta en la lista de ficheros modificables de la fase).
- **Previews**: las 9 `@Preview` usan `spec:width=1080px,height=2400px,dpi=480`, claro/oscuro y
  fontScale 1.0 (implicito) + 2.0. **fontScale 1.3 y revision visual en Realme -> PENDIENTE**
- **RedesignRegressionTest.kt** (androidTest, 6 tests): renderiza cada `Content` publico con fakes
  (estados falsos + callbacks vacios, sin OTP, sin red, sin Renfe) y verifica elementos clave.
  Compilado con assembleDebugAndroidTest; ejecucion PENDIENTE.
- **Matriz 6 modos / persistencia / fuentes en dispositivo**: PENDIENTE (sin dispositivo). Tabla en
  validation-matrix.md secciones 1-3 (aprobada por estructura; la porcion de dispositivo queda sin
  evidenciar).
- **Verificacion final (salida real, exit 0, comandos UNO a UNO)**:
  - `.\gradlew.bat :app:assembleDebug --no-daemon` -> BUILD SUCCESSFUL in 19s, exit 0.
  - `.\gradlew.bat :app:testDebugUnitTest --no-daemon` -> BUILD SUCCESSFUL in 27s, exit 0;
    XML **13 suites / 100 tests / 0 failures / 0 errors**.
  - `.\gradlew.bat :app:lintDebug --no-daemon` -> BUILD SUCCESSFUL in 41s, exit 0;
    **0 errors / 53 warnings** (baseline).
  - `.\gradlew.bat :app:assembleDebugAndroidTest --no-daemon` -> BUILD SUCCESSFUL in 28s, exit 0
    (compila RedesignRegressionTest).
  - `:app:connectedDebugAndroidTest` -> NO ejecutado (sin dispositivo conectado).
- **Aceptacion**: comandos finales en verde (4/4), M3 exclusivo (auditorias estaticas), contraste AA
  sobre esquemas finales, flujos con fakes compilados, archivados -> DESIGN.md (seccion FASE 13) y
  PROGRESS.md. La validacion en Realme/emulador (matriz 6 modos, persistencia, fuentes 1.0/2.0,
  TalkBack, fontScale 1.3, connectedDebugAndroidTest) queda PENDIENTE con evidencia de la causa
  (sin dispositivo) y NO se claim "validado".
- **Pendiente**: ejecutar en Realme/emulador los 9 tests instrumentales acumulados (8 previos +
  RedesignRegressionTest), la matriz de 6 modos, persistencia, fuentes 1.0/2.0, TalkBack y fontScale
  1.3; resolver (o documentar como aceptado) el hallazgo de seleccion por color. Detenido a la espera
  de instrucciones.

## Release v0.2.0 (2026-09-22) - Paso: bump de version y verificacion local COMPLETADO
- **Estado**: nomenclatura decidida por el usuario = **v0.2.0** (semver; preserva la serie 0.x y
  marca la UI rediseñada como hito). versionCode **12** (anterior 11) y versionName **"0.2.0"**
  (anterior "0.1.10") en `android/app/build.gradle.kts:25-26`.
- **Verificacion local (UNO a UNO, exit 0)**:
  - `.\gradlew.bat :app:assembleDebug --no-daemon` -> BUILD SUCCESSFUL in 36s.
  - `.\gradlew.bat :app:testDebugUnitTest --no-daemon` -> BUILD SUCCESSFUL in 20s;
    XML **13 suites / 100 tests / 0 failures / 0 errors**.
  - `.\gradlew.bat :app:lintDebug --no-daemon` -> BUILD SUCCESSFUL in 46s;
    **0 errors / 53 warnings** (baseline).
- **Commit**: bump de version en `redesign/ui-m3`.
- **Pendiente**: autorizacion explicita del usuario para el merge `redesign/ui-m3` -> `main`
  (AGENTS.md 4.1; este push SI activa CI), CI verde en main, tag `v0.2.0`, Release privada y
  validacion del APK firmado en el realme GT Neo 2.

## Release v0.2.0 (2026-09-22) - Cierre de release COMPLETADO
- **Merge**: PR #1 "Rediseno UI Material 3 + release v0.2.0" (`redesign/ui-m3` -> `main`) con CI de la
  PR verde (**Passed: 5, Failed: 0**, `rtk gh pr checks 1`) y fusionada con **merge commit**
  (`rtk gh pr merge 1 --merge`) -> `origin/main` en `5f90446`.
- **CI en main** tras el merge (push): `android-ci` (run 35778588295) y `backend-ci`
  (run 35778588334) ambos `[ok]` (evidencia en `rtk gh run list`).
- **Tag**: `v0.2.0` (anotado) creado sobre `5f90446` y empujado a origin -> dispara
  `android-release.yml` (run 35779719995, concl=success tras ~4 min).
- **Release**: "Release v0.2.0" privada creada por `github-actions[bot]`
  (https://github.com/Pablovb019/renfe-notifier-android/releases/tag/v0.2.0).
  Assets descargados y verificados: `app-release.apk` (**9.029.115 B**) y
  `app-release.apk.sha256`.
- **Checksum (evidencia, leido del asset .sha256)**:
  `0d4058f24dac3a6ec9ae09e2907cfff13fc5f6eb62ce445711c14816941bd2d2`.
- **README**: seccion "Estado" actualizada a v0.2.0 (codigo 12, APK firmado + checksum,
  PENDIENTE de instalar en realme) en commit de `main` 2026-09-22. PROGRESS.md actualizado
  con esta entrada.
- **Pendiente (sin dispositivo, valida cuando haya Realme)**:
  - Instalar y validar funcionalmente el APK v0.2.0 en el realme GT Neo 2 (9 grupos de tests
    instrumentales, revision visual de la UI M3, TalkBack, fontScale 1.3, persistencia, 6 modos).
  - Riesgo de accesibilidad documentado: seleccion de tren por color (primary/surfaceContainerLow)
    1.18:1 claro / 1.31:1 oscuro < 3:1 (WCAG 1.4.11).
---

## v0.2.1 (2026-09-22) - Fix bug visual del detalle + icono launcher

- **Bug visual detalle - ESTADO: ARREGLADO y verificado (dispositivo real)**
  - Reporte: el usuario (imagen en el realme) vio botones Pausar/Renovar/Eliminar solapando el contenido del detalle ("Renovar" pisa "Viaje: 25/09/2026", "Eliminar" pisa "Solo Plaza H"). El modelo no puede ver imagenes: verificacion por uiautomator + muestreo de pixeles.
  - Evidencia PRE-fix (tanto en v0.2.0 como tras reinstalar el artefacto reconstruido de main): el ScrollView del detalle renderizaba intercalado (Pausar [48,326][1032,470], ruta [96,470], Renovar [48,494][1032,638], Disponibilidad [638], Eliminar [48,662][1032,806]) con fecha, badge y titulos ausentes del arbol. Descartado APK desfasado (dex release v0.2.0 == dex artefacto de main run 35782713389) y remoto/local identicos en la fuente del detalle.
  - Causa raiz adoptada: cada rama del `Crossfade` en `FollowUpDetailContent` emitia una secuencia de composables SIN un root unico (los hijos se apilaban en el contenedor del Crossfade) y las acciones quedaban dentro del Crossfade.
  - Fix (commit `81ed31d`): una unica `Column` como root por rama en el Crossfade; botones de accion como bloque propio FUERA del Crossfade dentro del Column desplazable; `maxLines`/ellipsis en el titulo de episodio.
  - Verificacion local: `assembleDebug`, `testDebugUnitTest`, `lintDebug` sin errores. Instalado en el realme: release v0.2.1 (codigo 13), SHA256 `c5c3575a7b2829136aba19de884321a9292a7cd56cf0527a2ce01c12e0e053b2`, `install -r` Success, `dumpsys` versionCode=13/versionName=0.2.1.
  - Verificacion on-device post-fix (h2.xml): orden limpio y completo - Card cabecera [48,326][1032,1060] con ruta, badge Activo, Viaje, "Tren de 15:54 a 16:54", Disponibilidad, Solo Plaza H, Sin comprobaciones, Caduca, Episodios; Recordatorios [48,1096]; Episodios [48,1270]; "Aun no hay episodios" [48,1363]; Acciones [48,1508]; Pausar [48,1589][1032,1733]; Renovar [48,1757][1032,1901]; Eliminar [48,1925][1032,2069]. Sin solapes ni intercalado.
  - **Confirmado por el usuario en el realme: "el fix del bug visual va perfecto".**

- **Icono launcher - ESTADO: INSTALADO, retoque pendiente (pixeles blancos)**
  - Origen: `C:\Users\pablo\Downloads\logo-gen\logos` (maestras, preview y conjuntos `android/`).
  - Copiados a `android/app/src/main/res/mipmap-{anydpi-v26,mdpi,hdpi,xhdpi,xxhdpi,xxxhdpi}` (adaptive bg+fg+monochrome + 5 densidades) y declarados `android:icon="@mipmap/ic_launcher"` + `android:roundIcon` en el manifest. `aapt dump badging` confirma `icon='res/BW.xml'` (adaptativo). Proyectaba icono generico (robot Android) porque el manifest no tenia icon.
  - Instalado en el realme con v0.2.1.
  - El usuario reporta "un poco de pixeles blancos al logo en la caja de aplicaciones". Analisis de pixeles de los PNG: background 27.119 px blancos, foreground 16.189 px blancos (83% de su area opaca), monochrome sin transparencia; el compuesto mostraba un marco blanquecino en el borde del rombo y una barra vertical blanca interior. Pendiente: localizar el artefacto exacto del compuesto y retocar/regenerar los PNG.

- **Entregables v0.2.1**: commits `81ed31d` (fix + icono) y `3101c2f` (bump versionCode 13 / versionName 0.2.1). CI main verde (android-ci `35786006838`). APK firmado `35786405761` instalado en el realme a las 23:27 (conserva vinculacion). README "Estado" actualizado a v0.2.1 (en curso).
- **Pendiente (siguiente paso)**: resolver los pixeles blancos del icono; despues decidir publicacion de la Release v0.2.1 (tag + assets) y cerrar README con datos reales.---

## v0.2.2 (2026-09-22) - reparacion de icono + cierre del paso

- **Icono launcher - ESTADO: reparado e instalado, pendiente confirmacion visual del usuario**
  - Causa de los "pixeles blancos" localizada en los PNG generados (no en la integracion):
    `ic_launcher_background.png` tenia un marco blanco opaco (254,254,254) alrededor del rombo magenta y el `monochrome` era opaco sin alpha.
  - Reparacion (commit `30b5ab7`): fondo rellenado con el magenta interior en las 5 densidades (0 pixeles blancos verificados por muestreo; puros 126,1,94/127,1,94); `monochrome` regenerado como silueta negra transparente (correcto para iconos tematicos Android 13); `ic_launcher.png` legacy recomputado como compuesto limpio bg+glyph. El foreground (glyph blanco) no se ha tocado. Generado tanto en `res/` de la app como en `logo-gen/logos/android/`.
  - Version: bump a `0.2.2` (codigo 14) via commit `3b312a0` (la 0.2.1 queda como build intermedio instalado, sin release publicada).
  - CI main verde (`35787770773`) y release firmada `35788306280` (SHA256 `66d00e53a18ea6430e9b3e1de6a56e936e62dbea6bc759dd202d98c84b33f4fd`) instalada en el realme a las 23:47 (`versionCode=14`, `versionName=0.2.2`, conserva vinculacion).
  - Regresion on-device del detalle en v0.2.2 (v2.xml): orden limpio identico al fix 0.2.1 (Card cabecera, Recordatorios, Episodios, Acciones, Pausar/Renovar/Eliminar al final, sin solapes).
- **Pendiente unico**: el usuario confirma visualmente en el cajon de aplicaciones que ya no hay pixeles blancos en el logo.
- **Pendiente (siguiente paso segun confirmacion visual)**: publicar la Release v0.2.2 (tag + assets) y cerrar README/PROGRESS con los datos reales del APK.

## v0.2.2 (2026-09-23) - Causa real del icono (clipping por mascara) + icono de notificacion - EN CURSO

- **Diagnostico del problema REAL del icono (sin imagenes; analisis de pixeles + confirmacion del usuario con Deepseek)**:
  - El launcher del realme es `com.android.launcher` (AOSP, Android 13): el icono adaptativo se recorta con **mascara circular de ~142 px** sobre el lienzo 432x432 del foreground.
  - El foreground NO es una "R" sino un **tren blanco con indicador gris plata**: bbox original `[72,87][359,345]` en 432x432 (18.590 blancos + 2.952 grises; 0 pixeles blancos en filas/cols 0-19 -> sin contorno de cuadrado). Esquinas del bbox a ~193 px del centro >> mascara 142 px -> el recorte cortaba el tren ("franja blanca" arriba por el borde del tren, tren cortado y descentrado, masa circular).
  - Aunque "Informacion de aplicaciones" (captura info.png) mostraba el tile magenta 148x148 limpio (recurso correcto), el launcher seguia mostrando el clipping del tren -> por eso el usuario segua viendo el icono mal.
- **Regeneracion aplicada** (script `%TEMP%\opencode\regenicon.ps1`): foreground reescalado 0.663 y **centrado** (bbox nuevo `[120,130][311,301]`, maxDistCentro ~128 px <= zona segura 132 px y <= mascara real 142 px -> tren completo sin recorte); background `#830065` (131,0,101) solido opaco; `monochrome` silueta negra transparente (tematico Android 13); `ic_launcher.png` legacy recomposicionado con esquina `#830065`. Aplicado a las 5 densidades tanto en `res/` como en `logo-gen/logos/android/`. Verificado por `verifyicon.ps1` (bbox, uniformidad, transparencia).
- **Icono de notificacion NUEVO (`ic_stat_train`)**:
  - Antes la notificacion usaba `.setSmallIcon(android.R.drawable.ic_dialog_info)` -> la "I" generica del sistema.
  - Generado desde el foreground corregido (tren centrado) como **silueta blanca pura sobre transparencia** (alpha bilineal, reescalado a ~82% del lienzo) en `drawable-{mdpi,hdpi,xhdpi,xxhdpi,xxxhdpi}/ic_stat_train.png` (24/36/48/72/96 px) en `res/` y copiado a `logo-gen/logos/android/drawable-*/`. Verificado: todos los pixeles opacos son 255,255,255 (noBlanco=0), centrados, borde transparente.
  - `NotificationDisplayer.kt:56` -> `.setSmallIcon(R.drawable.ic_stat_train)` (el import `R` ya existia).
- **Verificacion local**: `.\gradlew.bat :app:assembleDebug` -> BUILD SUCCESSFUL in 11s. `aapt2 dump resources` del APK: `ic_stat_train` en las 5 densidades + los 4 iconos launcher presentes.
- **Git status**: 20 PNG mipmap modificados (5 densidades x {ic_launcher, background, foreground, monochrome}) + `NotificationDisplayer.kt` + 5 carpetas `drawable-*` nuevas.
- **Pendiente (siguiente paso)**: commit de todo; push a main con AVISO (activa CI android-ci); workflow_dispatch de `android-release.yml`; `adb install -r` del APK firmado; validacion visual del usuario (cajon de apps + notificacion real, ya no "I"); luego publicar Release v0.2.2 + cerrar README/PROGRESS.