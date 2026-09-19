# Arquitectura del Sistema: renfe-notifier-android

## 1. Objetivos y Principios de Diseño

1. **Presupuesto Estricto de 0,00 €**: Consumo contenido en las cuotas gratuitas del Free Tier permanente de Google Cloud (VM `e2-micro`), Firebase Spark (FCM) y GitHub Actions.
2. **Huella de Memoria Mínima**: La VM cuenta únicamente con 1 GB de RAM compartida. El backend completo (proceso Python + base de datos + planificador) debe operar de forma holgada en **menos de 100 MB de RAM**, con un límite estricto de contenedor fijado en **256 MB**, dejando más de 650 MB libres para el sistema operativo.
3. **Eliminación Total de Navegadores**: Se descarta por completo Selenium, Firefox ESR y Xvfb, sustituyéndolos por un adaptador HTTP/DWR nativo en Python.
4. **App Nativa Moderna**: Android nativo con Kotlin y Jetpack Compose en un único módulo estructurado por funcionalidades.
5. **Fiabilidad en Notificaciones**: Recepción desacoplada mediante Firebase Cloud Messaging (FCM) en segundo plano sin dependencias de servicios permanentes en primer plano ni polling en el teléfono móvil.

```
┌─────────────────────────────────────────────────────────────┐
│                 Dispositivo: realme GT Neo 2                │
│                   (Android 13 / Realme UI 4)                │
│                                                             │
│  ┌───────────────────────────────────────────────────────┐  │
│  │               App Nativa Jetpack Compose              │  │
│  │  - Búsqueda (estaciones, fecha, Plaza H, selector)    │  │
│  │  - Lista y detalle de seguimientos (CRUD)             │  │
│  │  - Ajustes, diagnóstico y emparejamiento              │  │
│  └─────────────────────────┬─────────────────────────────┘  │
│                            │                                │
│       API REST (HTTPS)     │       Notificaciones Push      │
│     (Tailscale / DuckDNS)  │       (FCM Data Payload)       │
│                            │               ▲                │
└────────────────────────────┼───────────────┼────────────────┘
                             │               │
                             ▼               │
┌────────────────────────────────────────────┼────────────────┐
│             Google Cloud VM: e2-micro      │  Firebase      │
│             (1 GB RAM, us-central1/east1)  │  Cloud         │
│                                            │  Messaging     │
│  ┌──────────────────────────────────────┐  │  (Plan Spark)  │
│  │         Backend Modular Python       │  │                │
│  │  ┌────────────┐     ┌─────────────┐  │  │        ▲       │
│  │  │  FastAPI   │     │ Planificador│  │  │        │       │
│  │  │  (Uvicorn) │     │ (Agrupador) │──┼──┼────────┘       │
│  │  └─────┬──────┘     └──────┬──────┘  │  │  HTTP v1 Push  │
│  │        │                   │         │  │                │
│  │  ┌─────▼───────────────────▼──────┐  │  │                │
│  │  │ SQLite Persistente (Modo WAL)  │  │  │                │
│  │  └────────────────────────────────┘  │  │                │
│  │  ┌────────────────────────────────┐  │  │                │
│  │  │    Cliente Nativo HTTP/DWR     │──┼──┼────────┐       │
│  │  └────────────────────────────────┘  │  │        │       │
│  └──────────────────────────────────────┘  │        │       │
└────────────────────────────────────────────┘        │       │
                                                      ▼       │
                                            ┌─────────────────┴─┐
                                            │ Servidores Renfe  │
                                            │   (venta.renfe)   │
                                            └───────────────────┘
```

---

## 2. Arquitectura del Backend

### 2.1. Elección de Framework: Justificación de FastAPI + Uvicorn
- **Opciones evaluadas**:
  - *Django*: Descartado por su alto consumo de memoria base (>120 MB en reposo) y exceso de componentes innecesarios (ORM pesado, admin web, etc.).
  - *Flask + Gunicorn*: Modelo síncrono por defecto. Para manejar concurrencia requiere múltiples workers, multiplicando el uso de RAM (~35 MB por worker × 4 = 140 MB).
  - **FastAPI + Uvicorn (1 worker)**: **Opción Seleccionada**.
    - *Consumo de memoria*: ~35–50 MB de RAM en ejecución.
    - *Concurrencia asíncrona nativa (`async`/`await`)*: Permite atender peticiones de la app móvil y llamadas de red con un único proceso sin crear hilos adicionales.
    - *Validación tipada con Pydantic*: Garantiza validación estricta de entradas en los endpoints (fechas, códigos de estación, IDs) sin código repetitivo.
    - *Documentación OpenAPI automática*: Facilita pruebas y contratos con la app móvil.

### 2.2. Módulos del Backend (Monolito Modular)
El backend se organizará en una estructura limpia y sin sobre-ingeniería:

1. `backend/app/main.py`: Punto de entrada de la aplicación FastAPI y ciclo de vida (`lifespan`) para arranque y parada controlada del planificador y base de datos.
2. `backend/app/api/`: Controladores REST autenticados:
   - `search.py`: Autocompletado y catálogo de estaciones, consulta puntual de trenes.
   - `followups.py`: Endpoints CRUD de seguimientos (crear, listar, pausar, reanudar, eliminar, forzar comprobación).
   - `pairing.py`: Flujo de emparejamiento del dispositivo y registro/actualización del token FCM.
   - `diagnostics.py`: Comprobación de salud (`health check`), versión, estado de base de datos y envío de notificación push de prueba.
3. `backend/app/renfe/`: Adaptador de comunicación con Renfe:
   - Implementa la secuencia comprobada de 5 peticiones POST DWR.
   - Reutiliza sesiones HTTP mediante contexto cerrado (`async with httpx.AsyncClient`).
   - Normalización de estaciones y lectura del catálogo `stations.json`.
   - Parser DWR basado en `json5` con extracción de trenes, precios y disponibilidad (estándar y Plaza H).
4. `backend/app/db/`: Capa de persistencia SQLite:
   - Inicialización con `PRAGMA journal_mode = WAL` y `PRAGMA busy_timeout = 5000`.
   - Migraciones versionadas secuenciales sin dependencias externas.
   - Operaciones transaccionales limpias con cierre garantizado.
5. `backend/app/scheduler/`: Planificador de comprobaciones:
   - **Propietario único**: Un solo worker/bucle asíncrono gestiona el reloj de comprobación (cada ~30 s).
   - **Agrupador inteligente**: Agrupa los seguimientos activos por la clave `(origen, destino, fecha, plaza_h)`. Una sola llamada a Renfe evalúa múltiples seguimientos simultáneamente.
   - **Control de solapamiento**: Si una comprobación tarda más de 30 segundos, no se acumulan llamadas; la siguiente se ejecuta inmediatamente al terminar la anterior.
6. `backend/app/notifications/`: Servicio de notificaciones:
   - Cliente Firebase Admin SDK (HTTP v1).
   - Armado de payloads estructurados (`data payload`) con `event_id`, datos de los trenes disponibles y marca temporal.
   - Deduplicación de avisos en servidor para evitar reenvíos innecesarios.

---

## 3. Modelo de Datos y Persistencia SQLite

### 3.1. Separación de Estados en el Ciclo de Vida
A diferencia del bot original (donde `/stop` eliminaba el registro y el estado `notifying` detenía la comprobación real), el nuevo modelo separa formalmente:
1. **Estado del Seguimiento (`status`)**:
   - `active`: En vigilancia activa periódica.
   - `paused`: En pausa temporal por el usuario (no se consulta).
   - `expired`: Fecha del viaje superada o caducidad mensual alcanzada sin renovar.
   - `deleted`: Borrado lógico por el usuario (o purgado físico controlado).
2. **Disponibilidad Detectada (`availability`)**:
   - `unknown`: No comprobado aún.
   - `unavailable`: Comprobado sin plazas.
   - `available`: Comprobado con plazas libres.
3. **Estado del Aviso (`notification_state`)**:
   - `idle`: Sin plazas detectadas.
   - `pending_alert`: Plazas detectadas, notificación generada.
   - `acknowledged`: Aviso recibido y confirmado por el usuario en el móvil.

### 3.2. Esquema Relacional de Tablas

```sql
-- Metadatos de migraciones
CREATE TABLE schema_migrations (
    version INTEGER PRIMARY KEY,
    applied_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Dispositivo emparejado único
CREATE TABLE paired_devices (
    device_id TEXT PRIMARY KEY,
    device_name TEXT,
    auth_token_hash TEXT NOT NULL,
    fcm_token TEXT,
    paired_at TIMESTAMP NOT NULL,
    last_seen_at TIMESTAMP NOT NULL
);

-- Seguimientos de trenes
CREATE TABLE followups (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    origin_code TEXT NOT NULL,
    origin_name TEXT NOT NULL,
    destination_code TEXT NOT NULL,
    destination_name TEXT NOT NULL,
    travel_date TEXT NOT NULL,          -- Formato YYYY-MM-DD
    departure_time TEXT,                -- HH:MM o NULL si watch_all=1
    arrival_time TEXT,                  -- HH:MM o NULL si watch_all=1
    train_id TEXT,                      -- Identificador compuesto o código si existe
    plaza_h INTEGER NOT NULL DEFAULT 0,
    watch_all INTEGER NOT NULL DEFAULT 0,
    status TEXT NOT NULL DEFAULT 'active', -- active, paused, expired, deleted
    availability TEXT NOT NULL DEFAULT 'unknown',
    last_price REAL,
    last_checked_at INTEGER,            -- Timestamp UTC
    created_at INTEGER NOT NULL,        -- Timestamp UTC
    expires_at INTEGER NOT NULL,        -- Timestamp UTC
    departure_ts INTEGER NOT NULL,      -- Timestamp UTC de salida
    total_checks INTEGER NOT NULL DEFAULT 0,
    daily_checks INTEGER NOT NULL DEFAULT 0,
    last_check_date TEXT                -- YYYY-MM-DD local Europe/Madrid
);

CREATE INDEX idx_followups_active ON followups(status, travel_date);
CREATE INDEX idx_followups_grouping ON followups(origin_code, destination_code, travel_date, plaza_h);

-- Registro de avisos y eventos (para deduplicación e historial)
CREATE TABLE notification_events (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    followup_id INTEGER NOT NULL,
    event_id TEXT UNIQUE NOT NULL,      -- UUID único de evento
    detected_at INTEGER NOT NULL,       -- Timestamp UTC
    fcm_message_id TEXT,
    status TEXT NOT NULL DEFAULT 'sent', -- sent, delivered, acknowledged, dismissed
    acknowledged_at INTEGER,
    FOREIGN KEY(followup_id) REFERENCES followups(id) ON DELETE CASCADE
);
```

---

## 4. Arquitectura de la Aplicación Android

### 4.1. Tecnologías y Stack
- **Lenguaje**: Kotlin 1.9+ / 2.0.
- **UI Framework**: Jetpack Compose con Material Design 3.
- **Arquitectura**: MVVM con StateFlow/SharedFlow y repositorios reactivos.
- **Concurrencia**: Kotlin Coroutines.
- **Red**: Retrofit 2 con OkHttp 4 (TLS 1.3 estricto, timeouts cortos y auth interceptor).
- **Almacenamiento Local**: Jetpack DataStore (preferencias y credenciales seguras).
- **Mensajería Push**: Firebase Cloud Messaging (FCM) Client SDK.
- **Compatibilidad**: Android 13+ (Target SDK 34/35), probado específicamente para **realme GT Neo 2** (Realme UI 4).

### 4.2. Estructura de Paquetes en Módulo Único (`:app`)
Para evitar capas vacías o sobre-ingeniería de multi-módulos en un proyecto personal:

```
com.pablovb019.renfenotifier/
├── core/
│   ├── network/          # Retrofit, interceptor de auth, certificados
│   ├── datastore/        # Preferencias de usuario y tokens
│   ├── notifications/    # FirebaseMessagingService, NotificationManager, Canales
│   └── model/            # Modelos de dominio y DTOs
├── feature/
│   ├── search/           # Pantalla de búsqueda, estaciones predictivas, selector de tren
│   ├── followups/        # Lista de seguimientos activos, tarjetas de estado, detalle
│   ├── settings/         # Emparejamiento, diagnóstico y temas
│   └── common/           # Componentes Compose reutilizables (TopBar, Diálogos, Errores)
└── MainActivity.kt       # Contenedor de navegación Compose
```

### 4.3. Notificaciones y Gestión de Energía en Realme UI 4
- **Canales de Notificación**:
  1. `disponibilidad_plazas_v2` (Canal de Alta Prioridad): Sonido distintivo, vibración, heads-up notification en pantalla. Id versionado: los atributos de sonido de un canal son inmutables y el borrado/recreación con el mismo id restaura los ajustes bloqueados por el usuario (v0.1.5 usaba `USAGE_ALARM`, que ignoraba vibración/silencio).
  2. `resumen_y_servicio` (Canal de Prioridad Baja/Normal): Notificaciones silenciosas para resumen diario o sincronización.
- **Data Payload Completo**: Los mensajes FCM viajan como `data payload` (sin bloque `notification` genérico). Esto permite a `FirebaseMessagingService` procesar el mensaje tanto con la app en primer plano como en segundo plano, mostrando una notificación nativa enriquecida con botones de acción interactivos:
  - `[Abrir Detalle]`
  - `[Confirmar Aviso]` (detiene recordatorios sin borrar el seguimiento)
  - `[Pausar Seguimiento]`
- **Comportamiento ante Doze y Realme UI**:
  - FCM High Priority despierta el dispositivo puntualmente para mostrar la alerta sin necesidad de servicios foreground persistentes que agoten la batería.
  - Se incluirá una guía en la app para configurar "Permitir actividad en segundo plano" en los ajustes de batería de Realme UI.

---

## 5. Exposición de Red y Conexión Cifrada

Para garantizar un acceso **seguro, permanente y a 0,00 €** sin depender de IPs estáticas de pago ni dominios comerciales:

### 5.1. Solución Primaria Recomendada: Tailscale (Mesh VPN WireGuard)
- **Funcionamiento**: La VM de Google Cloud y el teléfono realme GT Neo 2 se unen a una red privada virtual de Tailscale (plan personal gratuito, hasta 3 usuarios y 100 dispositivos).
- **Seguridad**:
  - Los puertos del backend (puerto 8000) **no se abren a Internet**. Únicamente escuchan en la interfaz virtual de Tailscale (`100.x.y.z`).
  - Cero superficie de ataque: los escáneres automáticos de Internet no pueden alcanzar el backend.
  - Cifrado P2P WireGuard de nivel militar sin latencia de intermediarios.
  - MagicDNS asigna un nombre estático y genera certificados TLS válidos de Let's Encrypt de forma automática.

### 5.2. Solución Secundaria Alternativa: DuckDNS + Let's Encrypt
- **Funcionamiento**:
  - Se registra un subdominio gratuito en DuckDNS (ej. `renfe-notif-pablo.duckdns.org`).
  - Un cron job ligero en la VM actualiza la IP efímera de Google Cloud cada 10 minutos.
  - Certbot obtiene certificados TLS oficiales de Let's Encrypt renovados automáticamente.
  - La app Android se comunica vía HTTPS estándar (`https://renfe-notif-pablo.duckdns.org`).
  - El firewall de GCP abre únicamente el puerto 443.

---

## 6. Límites de Recursos y Riesgos en 1 GB de RAM (e2-micro)

### 6.1. Asignación Presupuestaria de Memoria
| Componente | Memoria Estimada | Margen de Seguridad |
|---|---|---|
| Sistema Operativo (Linux Debian / Ubuntu minimal) | ~200 – 250 MB | Estable |
| Uvicorn + FastAPI (1 worker asíncrono) | ~40 – 60 MB | Pico en arranque |
| SQLite (WAL + cache de páginas 2.000 páginas) | ~10 – 15 MB | Muy contenido |
| Tareas asíncronas de red y scheduler | ~10 – 20 MB | En sondeo activo |
| **Total consumido por el sistema** | **~260 – 345 MB** | **< 35% de la RAM** |
| **Memoria libre de reserva** | **~650 – 740 MB** | **Holgura excelente** |

### 6.2. Controles de Protección del Contenedor (Docker Compose)
Para blindar el servidor frente a cualquier fuga imprevista de memoria:
```yaml
services:
  backend:
    deploy:
      resources:
        limits:
          cpus: '0.80'
          memory: 256M
        reservations:
          memory: 64M
```
Con este límite de 256 MB, el contenedor nunca podrá amenazar la estabilidad de la máquina virtual ni provocar el cierre de servicios críticos del sistema.
