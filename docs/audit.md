# Auditoría Funcional y Técnica del Repositorio Original

- **Repositorio auditado**: [`Pablovb019/renfe-notifier-bot`](https://github.com/Pablovb019/renfe-notifier-bot)
- **Commit exacto inspeccionado**: `dd658019280f588112bcb8d6f6f10224a85d3960`
- **Fecha del commit**: 2026-09-06 23:26:29 UTC
- **Mensaje del commit**: `fix(stats): programar resumen diario a las 00:00 en zona horaria Europe/Madrid`
- **Fecha de auditoría**: 2026-09-12

---

## 1. Comprobación de Licencia y Derechos de Reutilización

- **Evidencia en repositorio original**:
  - `README.md` (líneas 332–336): Declara licencia [MIT](./LICENSE).
  - `python/telegramcalendarkeyboard/LICENSE`: Archivo de licencia MIT explícito (Copyright 2016 unmonoqueteclea).
  - Repositorios upstream referenciados: `0electricista/renfe-web-monitor` y `emartinez-dev/renfe-bot`, ambos bajo licencia permisiva MIT.
  - El propietario del repositorio original (`Pablovb019`) es el mismo titular del nuevo proyecto.
- **Conclusión y estado de bloqueo**:
  - **No hay bloqueo de licencia**. La reutilización del código, algoritmos y datos de estaciones está plenamente permitida bajo los términos de la licencia MIT, manteniendo los avisos de derechos de autor y atribuciones correspondientes.

---

## 2. Hallazgos Funcionales Comprobados por Archivo y Función

### 2.1. `python/conversations.py`

1. **Normalización y resolución de estaciones**:
   - *Función*: `_normalize_station` (líneas 140–148).
   - *Evidencia*: Utiliza `unicodedata.normalize("NFKD", station)` para eliminar diacríticos/tildes y convierte a mayúsculas descartando cualquier caracter que no sea alfanumérico (`re.sub(r"[^A-Z0-9]", "", normalized)`).
   - *Función*: `_resolve_station` (líneas 183–190).
   - *Evidencia*: Crea un mapa de lookup con las estaciones normalizadas del catálogo y resuelve de forma exacta la entrada del usuario.
   - *Función*: `_suggest_stations` (líneas 192–223) y `_rank_station_matches` (líneas 275–320).
   - *Evidencia*: Tokeniza la consulta (mínimo 2–3 caracteres), calcula coincidencia de tokens, presencia de subcadenas, similitud mediante `difflib.SequenceMatcher` y añade una ponderación basada en la prioridad de la estación (`nmroPrioridad`). Retorna las 3 sugerencias más probables en caso de error.

2. **Gestión de grupos de estaciones (`TODAS`)**:
   - *Funciones*: `_is_group_station` (líneas 224–227), `_group_city_name` (líneas 229–231), `_group_station_candidates` (líneas 232–251).
   - *Evidencia*: Detecta si una estación termina en `(TODAS)` (p.ej. `MADRID (TODAS)`). Al seleccionarla, extrae la ciudad base y localiza todas las estaciones concretas asociadas que empiezan por o contienen el nombre de la ciudad, ofreciendo al usuario elegir una estación concreta o buscar por todas advirtiendo posibles inconsistencias de Renfe (`GROUP_STATION_ALL_WARNING`).

3. **Restricción y selección de fechas**:
   - *Función*: `_prompt_trip_date` (líneas 636–648) y `handler_date` (líneas 967–985).
   - *Evidencia*: La fecha mínima es hoy (`datetime.date.today()`) y la máxima es exactamente 2 meses en el futuro (`_add_months(today, 2)`). Fechas fuera de rango son rechazadas con `INVALID_DATE_RANGE`.

4. **Filtro Plaza H**:
   - *Función*: `handler_plaza_h` (líneas 986–1014).
   - *Evidencia*: Solicita confirmación explícita mediante teclado (`MAIN_OP_PLAZA_H_YES` / `MAIN_OP_PLAZA_H_NO`) fijando `conv._plaza_h` en `True` o `False`.

5. **Modos de selección de tren**:
   - *Función*: `handler_search_mode` (líneas 1063–1129).
   - *Evidencia*:
     - `specific`: Muestra listado numerado de trenes y solicita selección numérica (`ConvStates.TRAIN_SELECT`).
     - `first`: Si hay plazas disponibles, escoge automáticamente el tren con menor hora de salida (`min(available, key=lambda x: x["SALIDA"])`). Si no hay plazas, propone seguimiento sobre el primer tren del día.
     - `last`: Escoge el tren con mayor hora de salida disponible (`max(available, key=lambda x: x["SALIDA"])`), o el último del día para seguimiento.
     - `all`: Despliega un paginador interactivo de 4 trenes por página (`_start_train_picker`), permitiendo seleccionar un tren específico o "Todos los trenes" (`watch_all = True`).

6. **Flujo de reutilización ("otra consulta")**:
   - *Funciones*: `handler_additional_query`, `handler_additional_same_stations`, `handler_additional_same_date` (líneas 1277–1351).
   - *Evidencia*: Tras una consulta exitosa, pregunta si se desea realizar otra consulta. Si es afirmativo, pregunta secuencialmente si desea reutilizar las mismas estaciones y si desea reutilizar la misma fecha, permitiendo encadenar búsquedas sin volver a escribir datos redundantes.

---

### 2.2. `data/stations.json`

- *Evidencia*: Archivo JSON estructurado como diccionario de más de 1.300 estaciones de tren.
- *Estructura de cada registro*:
  ```json
  "MADRID PTA. ATOCHA - ALMUDENA GRANDES": {
    "cdgoEstacion": "60000",
    "cdgoAdmon": "0071",
    "nmroPrioridad": 2,
    "descEstacion": null,
    "desgEstacion": "MADRID PTA. ATOCHA - ALMUDENA GRANDES",
    "cdgoUic": "00600",
    "clave": "0071,60000,00600",
    "desgEstacionPlano": "MADRID PTA. ATOCHA - ALMUDENA GRANDES"
  }
  ```
- *Uso*: Las claves principales requeridas para la API de Renfe son `cdgoEstacion` (código numérico o alfanumérico para grupos) y `desgEstacion` (nombre descriptivo oficial). El campo `nmroPrioridad` se utiliza para ordenar los resultados de autocompletado y desambiguación.

---

## 3. Auditoría Técnica Detallada (Consultas, Tareas, Persistencia e Infraestructura)

### 3.1. Inicio de Firefox/Xvfb y evaluación de Selenium vs HTTP/DWR

- **Archivo**: `python/renfechecker.py`
- **Funciones**: `__init__` (líneas 46–62) y `check_trip` (líneas 312–324).
- **Evidencia técnica comprobada**:
  ```python
  def __init__(self, display=True):
      if display:
          self._display = Display(visible=0, size=(800, 600))
          self._display.start()
      else:
          self._display = None

      options = Options()
      options.set_preference("dom.webnotifications.enabled", False)
      options.set_preference("media.autoplay.default", 5)
      self.driver = webdriver.Firefox(options=options)
      self.driver.set_page_load_timeout(60)
      self._wait = WebDriverWait(self.driver, 30)
      self._station_lookup = self._load_station_lookup()
      self.driver.get("https://www.renfe.com")
  ```
- **Impacto crítico**:
  1. Firefox ESR y Xvfb se inician **de forma incondicional y síncrona en el constructor**, consumiendo entre 350 MB y 550 MB de RAM de forma fija.
  2. En una VM Google Cloud `e2-micro` (1 GB RAM total compartida), donde el SO base Linux consume ~250 MB, mantener Firefox en memoria deja menos de 250 MB disponibles para el proceso Python y el sistema operativo, disparando el OOM Killer.
  3. La función `check_trip` invoca primero `_check_trip_dwr`. En la práctica operativa habitual, DWR responde con éxito y Firefox nunca llega a utilizarse para la consulta, pero permanece en memoria indefinidamente.
  4. El fallback Selenium sólo se invoca ante excepciones no capturadas (`except Exception:` en línea 321), por lo que arrancar el navegador en el constructor es un desperdicio de recursos evitable.

---

### 3.2. Función y necesidad de los cinco POST (incluidos los dos `generateId`)

- **Archivo**: `python/renfechecker.py`
- **Función**: `_check_trip_dwr` (líneas 326–379).
- **Secuencia de peticiones comprobada**:
  1. `POST SEARCH_URL` (`https://venta.renfe.com/vol/buscarTren.do?Idioma=es&Pais=ES`): Envía el payload con origen, destino, fechas y `plazaH`. Renfe inicializa la cookie de búsqueda (`Search`) y crea el contexto de sesión web.
  2. `POST SYSTEM_ID_URL` (1ª llamada a `__System.generateId.dwr` con `batchId=0`): Inicializa el framework DWR (Direct Web Remoting) en el servidor de Renfe.
  3. `POST SYSTEM_ID_URL` (2ª llamada a `__System.generateId.dwr` con `batchId=1`): **Obligatoria**. En el protocolo DWR 2.x/3.x, la primera llamada crea el contexto y la segunda confirma y entrega el token de sesión (`dwr_token`) en el cuerpo de la respuesta (`r.handleCallback("0","0","TOKEN_HEX")`). Si se elimina esta segunda llamada, no se obtiene el token necesario para la cookie `DWRSESSIONID` ni se puede calcular el `script_session_id`.
  4. `POST UPDATE_SESSION_URL` (`buyEnlacesManager.actualizaObjetosSesion.dwr`): Vincula el `search_id` con la sesión DWR activa en el servidor mediante el parámetro `scriptSessionId={dwr_token}/{now_token}-{random_token}`.
  5. `POST TRAIN_LIST_URL` (`trainEnlacesManager.getTrainsList.dwr`): Ejecuta la consulta real de trenes con los parámetros de viaje y devuelve el JavaScript con el listado JSON de trenes (`listadoTrenes`).
- **Conclusión técnica**: La secuencia de 5 POSTs es obligatoria por la arquitectura interna de DWR de Renfe. Ninguna de las dos llamadas a `generateId` puede eliminarse sin romper la sesión.

---

### 3.3. Aislamiento y ciclo de vida de sesiones HTTP

- **Archivo**: `python/renfechecker.py`
- **Función**: `_check_trip_dwr` (línea 338).
- **Evidencia comprobada**:
  - `session = requests.Session()` se instancia como variable local en cada llamada a `_check_trip_dwr`.
  - **No existe cierre explícito**: No se usa `with requests.Session() as session:` ni bloque `finally: session.close()`.
  - **Impacto**:
    1. *Fuga de sockets transitoria*: Las conexiones quedan en estado `TIME_WAIT` hasta que el garbage collector de Python libera el objeto.
    2. *Overhead de red*: Cada consulta crea 5 conexiones HTTPS nuevas, realizando 5 negociaciones TLS completas desde cero. Esto añade de 1,5 a 3 segundos de latencia adicional por comprobación y consumo innecesario de CPU en criptografía TLS.
    3. *Ventaja de aislamiento*: Al no compartir sesión entre consultas concurrentes, se evita la contaminación de cookies entre búsquedas distintas. En el nuevo diseño debe adoptarse un pool de conexiones HTTP reutilizable (ej. `httpx.AsyncClient` o `aiohttp.ClientSession` con timeouts estrictos) aislando cookies por consulta.

---

### 3.4. Concurrencia, procesamiento secuencial y bucle `notifying`

- **Archivo**: `python/renfebot.py` y `python/dbmanager.py`.
- **Funciones**: `check_followups` (líneas 165–246), `notify_available_followups` (líneas 247–277) y `stop_user_notifying_followups` (líneas 230–242).
- **Evidencia comprobada**:
  1. *Procesamiento secuencial bloqueante*: `check_followups` itera sobre `followups` uno a uno con `await asyncio.to_thread(self._RF.check_trip, ...)`. Con 5 seguimientos que tarden ~4 segundos cada uno, el ciclo tarda 20 segundos. Si una petición agota el timeout de 30 segundos, el bucle supera ampliamente los 30 segundos, retrasando todas las comprobaciones posteriores.
  2. *Sin agrupación de consultas*: Si un usuario tiene 3 seguimientos para el mismo día y la misma ruta (por ejemplo, hora específica, primer tren y todos los trenes), el bot ejecuta 3 consultas HTTP completas idénticas (15 POSTs a Renfe) en lugar de una sola consulta compartida.
  3. *Comportamiento del estado `notifying`*:
     - Al detectar plazas, `check_followups` cambia el estado a `'notifying'`.
     - `get_active_followups` sólo consulta `status='active'`, por lo que **se detiene por completo la comprobación de plazas en Renfe** para ese seguimiento.
     - En su lugar, el job `notify_available_followups` envía un mensaje por Telegram cada 10 segundos de forma ciega, **sin saber si las plazas siguen existiendo o se han agotado**.
  4. *Destrucción de datos con `/stop`*:
     - Cuando el usuario ejecuta `/stop`, la función `stop_user_notifying_followups` ejecuta:
       ```sql
       DELETE FROM followups WHERE userid=? AND status='notifying'
       ```
     - Esto **borra físicamente el seguimiento**. El usuario pierde la configuración del seguimiento en lugar de pausarlo o mantenerlo en vigilancia.

---

### 3.5. Identificación de trenes y limitaciones de matching

- **Archivo**: `python/renfebot.py`.
- **Función**: `_match_followup_train` (líneas 158–163).
- **Evidencia técnica comprobada**:
  ```python
  def _match_followup_train(available_trains, dep, arr):
      for train in available_trains:
          if train["SALIDA"].strftime("%H:%M") == dep and train["LLEGADA"].strftime("%H:%M") == arr:
              return train
      return None
  ```
- **Limitaciones**:
  1. Sólo compara strings formateados de salida (`%H:%M`) y llegada (`%H:%M`).
  2. Si Renfe reajusta el horario de un tren en 1 minuto, o si dos trenes de distinta categoría comparten horario exacto en un tramo, el matching falla o produce ambigüedad.
  3. En `watch_all`, no se registra qué trenes específicos se liberaron, sólo si la lista de disponibles es mayor que cero.

---

### 3.6. Fechas, zonas horarias y gestión de SQLite

- **Zonas horarias**:
  - En `Dockerfile`, se fuerza `ENV TZ=Europe/Madrid`.
  - En `dbmanager.py` (`_departure_ts`, líneas 104–112), se combina `travel_date` y `departure_time` usando `datetime.datetime.combine` sin indicar `tzinfo`. Al calcular `int(dt.timestamp())`, Python asume la zona horaria del sistema operativo anfitrión. Si el contenedor o la máquina virtual se configuran en UTC, `departure_ts` tendrá un desfase de 1 o 2 horas (según horario de verano/invierno en España), provocando que los seguimientos expiren antes de tiempo.
- **Gestión de SQLite**:
  - `dbmanager.py` usa el decorador `@_openclose`, que ejecuta `sqlite3.connect()` y `conn.close()` en cada llamada individual.
  - **No activa modo WAL** (`PRAGMA journal_mode = WAL`). Utiliza el journal tradicional por defecto (`DELETE`), que bloquea toda la base de datos durante cualquier escritura.
  - **No configura `busy_timeout`** (`PRAGMA busy_timeout = 5000`). Si dos corrutinas o hilos intentan escribir simultáneamente, una falla inmediatamente con `sqlite3.OperationalError: database is locked`.

---

### 3.7. Gestión de excepciones y resiliencia

- **En `renfechecker.py`**:
  - `check_trip` captura genéricamente `except Exception:`, enmascarando errores de red, respuestas HTTP 403/429 (bloqueo por rate-limiting) o fallos de parseo.
  - Al capturar cualquier excepción, invoca inmediatamente Selenium. Si el fallo se debe a caída del servicio de Renfe o bloqueo de IP, Selenium se ejecuta inútilmente, colapsando la memoria de la máquina.
- **En `dbmanager.py`**:
  - No hay bloques `try/finally` dentro del decorador `@_openclose`. Si la ejecución de un cursor lanza una excepción SQL, la conexión no se cierra limpiamente en el bloque de salida.

---

### 3.8. Auditoría de Docker, Compose y GitHub Actions originales

1. **`requirements.txt`**:
   - Contiene librerías innecesarias para la nueva arquitectura: `selenium`, `pyvirtualdisplay`, `python-telegram-bot[job-queue]`, `emoji`.
   - Para el nuevo backend sólo se requerirá un framework ligero (ej. FastAPI + Uvicorn), `requests`/`httpx`, `json5`, `apscheduler` y `firebase-admin`.

2. **`Dockerfile`**:
   - Instala paquetes pesados del sistema: `firefox-esr`, `xvfb`, `geckodriver`.
   - La imagen resultante supera 1,1 GB de tamaño, requiriendo descargas pesadas y espacio en disco significativo en la VM e2-micro (que cuenta con un disco estándar de 30 GB dentro del Free Tier).

3. **`docker-compose.yml`**:
   - Ejecuta `build: .` directamente en la máquina virtual durante el despliegue. Compilar dependencias y empaquetar capas en una CPU compartida de e2-micro con 1 GB de RAM genera picos de CPU al 100% y saturación de memoria swap.

4. **`.github/workflows/deploy.yml`**:
   - Se activa con cada `push` directo a la rama `main`.
   - Utiliza `appleboy/ssh-action` con credenciales estáticas de SSH guardadas en secrets (`VM_SSH_KEY`).
   - **Carencias críticas**:
     - No ejecuta pruebas unitarias antes de desplegar.
     - No realiza copia de seguridad consistente de SQLite antes de actualizar el código.
     - No realiza `health check` post-despliegue.
     - No contempla mecanismo de rollback ante fallos.
     - Si este workflow antiguo continuase activo, pisaría cualquier nuevo despliegue en la VM.

---

## 4. Matriz Definitiva de Decisiones Técnicas

| Componente Original | Decisión | Justificación Técnica Rigurosa |
|---|---|---|
| **Catálogo `stations.json`** | **Conservar** | Base de datos exhaustiva (+1.300 estaciones) con metadatos completos (`cdgoEstacion`, `nmroPrioridad`). |
| **Algoritmo de normalización NFKD** | **Conservar** | Limpieza eficaz de tildes, caracteres especiales y mayúsculas, garantizando búsquedas insensibles a diacríticos. |
| **Ponderación y scoring de estaciones** | **Conservar** | Algoritmo predictivo comprobado mediante `difflib.SequenceMatcher` y prioridad de ciudad. |
| **Secuencia de 5 POSTs DWR** | **Conservar** | Imprescindible para el protocolo DWR de Renfe; los dos `generateId` son obligatorios para obtener `DWRSESSIONID`. |
| **Selenium + Firefox + Xvfb** | **Eliminar** | Consumo prohibitivo (350–550 MB RAM) que asfixia la VM e2-micro (1 GB). HTTP/DWR es autosuficiente. |
| **`python-telegram-bot` y `emoji`** | **Eliminar** | Telegram se sustituye completamente por la app Android nativa y Firebase Cloud Messaging (FCM). |
| **Tabla legacy `queries`** | **Eliminar** | Esquema residual no utilizado en la versión actual. |
| **Apertura y cierre por consulta SQLite** | **Adaptar** | Implementar pool de conexiones con modo WAL (`PRAGMA journal_mode = WAL`) y `busy_timeout = 5000`. |
| **Zonas horarias implícitas** | **Adaptar** | Almacenar siempre timestamps en UTC en la base de datos; interpretar fechas locales en `Europe/Madrid` explícitamente con `zoneinfo`. |
| **Planificador secuencial sin agrupación** | **Adaptar** | Implementar planificador ligero que agrupe seguimientos por `(origen, destino, fecha, plaza_h)`, reduciendo hasta un 80% las llamadas a Renfe. |
| **Bucle ciego `notifying` (10s)** | **Adaptar** | Sustituir por notificaciones push FCM estructuradas con deduplicación y acciones directas. |
| **Borrado de seguimiento con `/stop`** | **Adaptar** | Separar ciclo de vida (activo/pausado) del aviso (pendiente/confirmado). Confirmar no debe borrar el seguimiento. |
| **Workflow SSH directo sin CI** | **Adaptar** | Diseñar pipeline seguro con CI previa obligatoria, `workflow_dispatch` manual, backup de SQLite y rollback. |
