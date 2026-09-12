# Registro de Progreso

## Hit 1: Estructura del proyecto, pruebas iniciales y CI
- **Fecha**: 2026-09-12
- **Acciones realizadas**:
  - Estructura de carpetas para backend y Android creada.
  - Archivos placeholders:
    - `backend/requirements.txt`
    - `backend/main.py`
    - `backend/test_main.py` (prueba unitaria que pasa)
    - `src/main/java/com/example/renfe_notifier/MainActivity.kt`
    - `src/test/java/com/example/renfe_notifier/ExampleUnitTest.kt` (prueba unitaria que pasa)
  - Licencia MIT añadida.
  - README.md actualizado con descripción breve.
  - .gitignore actualizado con exclusiones para backend y Android (secciones añadidas).
  - Archivo de workflow de CI creado (`.github/workflows/ci.yml`) que ejecuta lint de Python y pruebas unitarias del backend, y verifica presencia de archivos Kotlin y de tests.
- **Pruebas ejecutadas**:
  - Backend: `python -m unittest test_main.py` → OK.
  - Android: No se pudieron ejecutar localmente por falta de entorno de compilación, pero las pruebas unitarias están listas para ser validadas en CI.
- **Próximos pasos**: Autorización para iniciar el Hit 2 (Adaptador Renfe, SQLite y lógica de seguimiento).

## Hit 2: Adaptador Renfe, SQLite y lógica de seguimiento
- **Fecha**: 2026-09-12
- **Acciones realizadas**:
  - Se creó el adaptador Renfe basado en HTTP/DWR (sin inicialización automática de Selenium/Selenium, evitando arranque innecesario del navegador).
    - Archivo: `backend/renfe/checker.py`
    - Funcionalidad: `check_trip` usa solo HTTP/DWR, reutiliza sesiones por consulta, carga estaciones desde `data/stations.json`.
  - Se creó la capa de persistencia SQLite con WAL y `busy_timeout`.
    - Archivo: `backend/db/database.py`
    - Funcionalidad: esquema de followups y tokens FCM, operaciones CRUD, manejo de conexiones seguras.
  - Se creó el planificador ligero que ejecuta comprobaciones cada 30 segundos.
    - Archivo: `backend/scheduler/followup_scheduler.py`
    - Funcionalidad: usa `APScheduler` para llamar al checker y actualizar la base de datos (disponibilidad, contadores de consultas).
  - Se creó un script de prueba de integración que verifica la interacción entre el checker y la base de datos.
    - Archivo: `backend/test_integration.py`
  - Se creó un conjunto mínimo de datos de estaciones para pruebas y funcionamiento.
    - Archivo: `data/stations.json` (contiene dos estaciones de ejemplo).
  - Se actualizó el archivo `backend/requirements.txt` con las dependencias necesarias para este hit:
    - `requests`, `json5`, `selenium`, `pyvirtualdisplay`, `apscheduler`, `python-dotenv`.
  - Se verificó que las pruebas de backend siguen pasando y que la prueba de integración se ejecuta sin errores.
- **Pruebas ejecutadas**:
  - Backend: `python -m unittest test_main.py` → OK.
  - Integración: `python backend/test_integration.py` → Éxito (se añadió, consultó y eliminó un followup correctamente).
  - Android: No se pudieron ejecutar localmente por falta de entorno de compilación, pero las pruebas unitarias están listas para ser validadas en CI.
- **Próximos pasos**: Esperar autorización para iniciar el Hit 3 (API autenticada y app Android básica).

## Estado actual
- **Repositorio original** (`Pablovb019/renfe-notifier-bot`) **intacto y sin cambios**.
- **Nuevo repositorio** (`renfe-notifier-android`) contiene la estructura de base, código de backend parcialmente implementado, archivos de posición para Android, licencia, README, progreso y configuración de CI.
- **No se ha desplegado nada** en la VM de Google Cloud ni se ha detenido el bot original.
- **Se han ejecutado las comprobaciones posibles localmente** (pruebas de backend y prueba de integración pasan); el resto será validado en CI cuando se disponga del entorno completo.

## Pendientes después de este hit
1. **Backend**  
   - Exponer API REST mínima (FastAPI o similar) para registro de token FCM, CRUD de followups, estadísticas y forzado de consulta.  
   - Integrar Firebase Admin SDK para envío de notificaciones FCM (pendiente de hit 4).  
   - Añadir manejo de errores y reintentos más robustos.  
2. **Android**  
   - Construir las UI principales (lista de seguimientos, pantalla de creación, detalle, ajustes).  
   - Implementar ViewModel y Repositorio que consuman la API del backend (Retrofit).  
   - Manejar el registro del token FCM y la recepción de mensajes (FirebaseMessagingService).  
   - Diseñar notificaciones de dos tipos: disponibilidad (alta prioridad, con acciones) y resumen diario (normal).  
   - Añadir preferencias (modo claro/oscuro, hora de resumen) mediante DataStore.  
3. **CI/CD**  
   - Ajustar el workflow de CI para que también compile el proyecto Android (descargando el Gradle Wrapper o usando `gradle wrapper` en el step de setup).  
   - Añadir un workflow de despliegue manual (artefactos) que suba un zip del backend y el APK de firma como release; el despliegue en la VM será manual mediante descarga de dicho artefacto (evitando almacenar secrets de SSH en el repo).  
   - Definir reglas de protección de rama (requiere CI aprobada antes de merge a `main`).  
4. **Documentación y pruebas**  
   - Escribir pruebas unitarias más significativas para backend y Android.  
   - Añadir pruebas de integración ligera (mock del backend) para la app.  
   - Actualizar `README.md` con instrucciones de construcción y ejecución local.  

---  
*Nota: Se ha respetado el requisito de 0 € de gasto, se ha evitado arrancar Firefox/Selenium sin necesidad, se ha distinguido disponibilidad de confirmación del aviso, y se ha aclarado que el objetivo de 30 segundos es solo para la consulta backend, no una garantía de entrega end‑to‑end.*  
**Co‑Authored-By: Claude Opus 5 (1M context) <noreply@anthropic.com>**