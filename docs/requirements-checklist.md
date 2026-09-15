# Lista de Requisitos Comprobables (Requirements Checklist)

Este documento desglosa los requisitos comprobables del proyecto a partir de `prompts/00-especificacion-original.md`, agrupados por secciones. Sirve como matriz de trazabilidad y verificación continua.

---

## 1. Repositorio Nuevo y Privado
- [x] **REQ-01.1**: Repositorio nuevo y privado creado en GitHub (`Pablovb019/renfe-notifier-android`).
- [x] **REQ-01.2**: El repositorio original `Pablovb019/renfe-notifier-bot` permanece intacto y sin modificaciones.
- [x] **REQ-01.3**: El repositorio aloja código de Android, backend y GitHub Actions.
- [x] **REQ-01.4**: Inicializado sin secretos, datos personales, bases de datos ni claves privadas.
- [x] **REQ-01.5**: Conservación de licencias y atribuciones de código reutilizado del repositorio original (Licencia MIT verificada en `docs/audit.md`).
- [x] **REQ-01.6**: No habilitar workflows ni despliegues automáticos durante la creación inicial.

## 2. Objetivo y Contexto Confirmado
- [ ] **REQ-02.1**: App Android nativa para uso personal (1 único usuario) instalable por APK sin publicar en Google Play (Diseñado en `docs/architecture.md`).
- [ ] **REQ-02.2**: Compatibilidad verificada con dispositivo objetivo: realme GT Neo 2 (Realme UI 4, Android 13).
- [ ] **REQ-02.3**: Backend ejecutable en VM Google Cloud Compute Engine e2-micro (2 vCPU compartidas, 1 GB RAM) con contenedor limitado a 256 MB.
- [ ] **REQ-02.4**: Soporte concurrente para entre 1 y 5 seguimientos activos.
- [ ] **REQ-02.5**: Objetivo de sondeo periódico de comprobación en backend de aproximadamente cada 30 segundos.
- [ ] **REQ-02.6**: Sistema de avisos configurables: aviso único o recordatorios periódicos hasta confirmación.
- [ ] **REQ-02.7**: Inicio con seguimientos limpios (sin migración de seguimientos previos).

## 3. Reglas de Coste: Cero Euros
- [x] **REQ-03.1**: Presupuesto total de 0,00 € garantizado sin servicios de pago ni pruebas con cobro posterior (Auditado en `docs/costs.md`).
- [x] **REQ-03.2**: Reutilización de la VM e2-micro existente sin aprovisionar nuevas instancias.
- [x] **REQ-03.3**: Verificación de elegibilidad de Free Tier en región, almacenamiento en disco, IPs y tráfico saliente (`docs/costs.md`).
- [x] **REQ-03.4**: Ausencia total de APIs o dependencias de IA en el código final de la aplicación y backend.
- [x] **REQ-03.5**: Medición empírica de tráfico de red y establecimiento de límites conservadores de consumo (`docs/costs.md`).

## 4. Auditoría Inicial del Repositorio Original
- [x] **REQ-04.1**: Registro del commit exacto inspeccionado del repositorio original: `dd658019280f588112bcb8d6f6f10224a85d3960`.
- [x] **REQ-04.2**: Análisis detallado del código fuente original (`renfechecker.py`, `renfebot.py`, `conversations.py`, `dbmanager.py`, `texts.py`, `stations.json`, tests, Dockerfile, etc.) en `docs/audit.md`.
- [x] **REQ-04.3**: Verificación de hallazgos previos: arranque innecesario de Firefox/Xvfb, secuencia HTTP/DWR, llamadas a `generateId`, concurrencia con `asyncio.to_thread`, estado `notifying`, matching de trenes por horario, y gestión de zonas horarias/conexiones (`docs/audit.md`).
- [x] **REQ-04.4**: Documento de auditoría con problemas, evidencias, impacto, componentes a conservar/adaptar/eliminar, incertidumbres y recomendaciones (`docs/audit.md`).

## 5. Arquitectura Base
- [x] **REQ-05.1**: Stack Android diseñado: Kotlin, Jetpack Compose, ViewModel, coroutines/Flow y arquitectura modular limpia (`docs/architecture.md`).
- [x] **REQ-05.2**: Stack Backend diseñado: Monolito modular ligero en Python (FastAPI + Uvicorn) adaptado al límite estricto de 1 GB de RAM (`docs/architecture.md`).
- [x] **REQ-05.3**: Persistencia diseñada: SQLite con modo WAL, `busy_timeout = 5000`, índices adecuados y migraciones versionadas (`docs/architecture.md`).
- [x] **REQ-05.4**: Transaccionalidad segura en SQLite: sin transacciones abiertas durante llamadas bloqueantes de red (`docs/architecture.md`).
- [x] **REQ-05.5**: Planificador diseñado: Un único hilo/proceso propietario del scheduler para evitar consultas duplicadas (`docs/architecture.md`).
- [x] **REQ-05.6**: Exclusión de componentes pesados innecesarios: sin Celery, Redis, PostgreSQL ni Kubernetes (`docs/architecture.md`).

## 6. Acceso a Renfe y Eficiencia
- [ ] **REQ-06.1**: Reutilización y adaptación del protocolo HTTP/DWR documentado en el bot original (5 POSTs).
- [ ] **REQ-06.2**: Agrupación inteligente de seguimientos por parámetros comunes (origen, destino, fecha, Plaza H) para reutilizar una única consulta para múltiples seguimientos.
- [ ] **REQ-06.3**: Concurrencia global baja controlada (1–2 workers concurrentes como máximo) evitando colisiones.
- [ ] **REQ-06.4**: Descarte de consultas acumuladas si una comprobación tarda más de 30 segundos (sin desbordamiento de cola).
- [ ] **REQ-06.5**: Timeouts estrictos por petición HTTP y presupuesto de tiempo global por ciclo de comprobación.
- [ ] **REQ-06.6**: Estrategia de reintentos con backoff exponencial, jitter y gestión explícita de códigos 429 y 403.
- [ ] **REQ-06.7**: Sin consultas a Renfe si no hay seguimientos activos.
- [ ] **REQ-06.8**: Tratamiento de datos: diferenciar error técnico, respuesta corrupta, dato en caché y ausencia de plazas; no convertir precios desconocidos a cero.
- [x] **REQ-06.9**: Navegador headless (Selenium/Firefox) eliminado del despliegue normal (`docs/audit.md`, `docs/architecture.md`).

## 7. Modelo de Seguimientos y Ciclo de Vida
- [x] **REQ-07.1**: Separación formal de tres estados independientes diseñada en `docs/architecture.md`:
  1. *Ciclo de vida*: activo, pausado, vencido, eliminado.
  2. *Disponibilidad*: desconocida, no disponible, disponible.
  3. *Aviso*: pendiente, enviado, confirmado.
- [ ] **REQ-07.2**: Continuar comprobando plazas aunque se haya enviado un aviso previo.
- [ ] **REQ-07.3**: Disparar nuevo aviso solo ante una nueva aparición válida de plazas tras indisponibilidad previa.
- [ ] **REQ-07.4**: Confirmar un aviso no elimina el seguimiento de la base de datos.
- [ ] **REQ-07.5**: Pausar y eliminar son operaciones independientes y reversibles (pausar/reanudar).
- [ ] **REQ-07.6**: Recordatorios configurables y acotados, cancelables ante confirmación, pausa, vencimiento o agotamiento de plazas.
- [ ] **REQ-07.7**: Identificación unívoca de trenes mediante clave compuesta robusta documentada.
- [ ] **REQ-07.8**: Almacenamiento de timestamps en UTC; renderizado e interpretación de horarios de viaje en `Europe/Madrid`.
- [ ] **REQ-07.9**: Gestión correcta de viajes nocturnos, cambios de día y cambios de horario de verano/invierno.

## 8. Notificaciones Android y Firebase Cloud Messaging (FCM)
- [x] **REQ-08.1**: Integración de FCM mediante Firebase Admin SDK / HTTP v1 desde el backend diseñada en `docs/architecture.md`.
- [x] **REQ-08.2**: Credenciales de servicio exclusivamente en el backend (nunca en el APK).
- [ ] **REQ-08.3**: Gestión adecuada del permiso `POST_NOTIFICATIONS` en Android 13+.
- [x] **REQ-08.4**: Canales de notificación bien definidos (canal de alta prioridad para plazas detectadas, canal informativo para resúmenes).
- [x] **REQ-08.5**: Payload estructurado completo (data payload) que permite mostrar la notificación de inmediato sin requerir consulta de red en el móvil.
- [x] **REQ-08.6**: Identificadores de evento (`event_id`), timestamp de observación y expiración corta en cada mensaje FCM.
- [ ] **REQ-08.7**: Deduplicación de avisos tanto en backend como en la app Android.
- [ ] **REQ-08.8**: Acciones interactivas directas en la notificación: "Abrir seguimiento", "Confirmar aviso", "Pausar".
- [x] **REQ-08.9**: Comportamiento documentado ante Doze, modo ahorro de energía y restricciones de batería de Realme UI en `docs/architecture.md`.

## 9. Seguridad y Conexión Backend-Móvil
- [x] **REQ-09.1**: Comunicación cifrada y autenticada entre app Android y el backend API vía Tailscale WireGuard P2P + HTTPS (Aprobado y diseñado en `docs/security.md`).
- [x] **REQ-09.2**: Emparejamiento seguro y credencial revocable por dispositivo mediante código OTP local (Diseñado en `docs/security.md`).
- [x] **REQ-09.3**: Sin secretos compartidos estáticos hardcodeados en el código fuente o en el APK compilado (`docs/security.md`).
- [x] **REQ-09.4**: Almacenamiento seguro de claves y tokens en el móvil mediante Android Keystore / EncryptedSharedPreferences (`docs/security.md`).
- [x] **REQ-09.5**: Validación rigurosa de entradas en API (estaciones, fechas, parámetros) e impedimento de peticiones a URLs arbitrarias (anti-SSRF).
- [x] **REQ-09.6**: Ofuscación y filtrado estricto en logs: no volcar tokens, credenciales ni cookies (`docs/security.md`).

## 10. Funcionalidad a Conservar
- [ ] **REQ-10.1**: Catálogo de estaciones, sinónimos/alias y búsqueda tolerante a tildes y mayúsculas.
- [ ] **REQ-10.2**: Búsqueda por origen, destino, fecha y opción de filtro Plaza H.
- [ ] **REQ-10.3**: Modos de selección de tren: tren específico, primer tren, último tren, o todos los trenes.
- [ ] **REQ-10.4**: Gestión completa en app: listar, ver detalle, crear, pausar, reanudar y eliminar seguimientos.
- [ ] **REQ-10.5**: Duplicar/reutilizar ruta y fecha para una nueva búsqueda con un toque.
- [ ] **REQ-10.6**: Visualización de estado: plazas disponibles, precio detectado y marca temporal de la última comprobación válida.
- [ ] **REQ-10.7**: Estadísticas operativas separando comprobaciones lógicas frente a peticiones HTTP reales a Renfe.
- [ ] **REQ-10.8**: Pantalla de diagnóstico con test de conexión con backend y test de recepción de notificación FCM de prueba.
- [ ] **REQ-10.9**: Interfaz totalmente en español, soporte accesible y soporte para temas claro y oscuro.
- [x] **REQ-10.10**: Prohibición estricta: bajo ningún concepto la app reservará ni comprará billetes de tren.

## 11. CI/CD con GitHub Actions
- [x] **REQ-11.1**: Verificación de cuota gratuita disponible de minutos (2.000) y almacenamiento (500 MB) en GitHub Actions para el repo privado (`docs/ci-cd.md`).
- [x] **REQ-11.2**: Workflow de CI para backend diseñado: linting con Ruff, mypy y pytest con fixtures (`docs/ci-cd.md`).
- [x] **REQ-11.3**: Workflow de CI para Android diseñado: linting, pruebas unitarias y compilación del APK de pruebas (`docs/ci-cd.md`).
- [x] **REQ-11.4**: Filtrado de ejecución por rutas (`paths-filter`) con agregador para evitar checks bloqueados (`docs/ci-cd.md`).
- [x] **REQ-11.5**: Uso exclusivo de runners Linux estándar de GitHub (`ubuntu-latest`), sin runners autoalojados en la e2-micro (`docs/ci-cd.md`).
- [x] **REQ-11.6**: Caché eficiente de dependencias (pip, Gradle) y retención mínima de artefactos (3 días) en `docs/ci-cd.md`.
- [x] **REQ-11.7**: Workflow de CD de backend diseñado con activación manual (`workflow_dispatch`), validando commit de `main` que haya superado CI (`docs/ci-cd.md`).
- [x] **REQ-11.8**: Despliegue seguro en la VM con backup previo atómico de SQLite, túnel IAP y health check con rollback automático (`docs/ci-cd.md`).
- [x] **REQ-11.9**: Workflow de generación y firma de APK Release protegido con secretos de GitHub y checksum SHA-256 para instalación manual (`docs/ci-cd.md`).

## 12. Pruebas y Validación
- [ ] **REQ-12.1**: Pruebas unitarias de backend con fixtures estáticos (sin consultar servidores de Renfe en CI).
- [ ] **REQ-12.2**: Pruebas unitarias de parser DWR, filtrado Plaza H y algoritmo de matching de trenes.
- [ ] **REQ-12.3**: Pruebas de máquina de estados del seguimiento (transiciones activo/pausado/vencido/eliminado y lógica de avisos).
- [ ] **REQ-12.4**: Pruebas de concurrencia y planificador (evitar doble consulta, descarte tras timeout).
- [ ] **REQ-12.5**: Pruebas unitarias en Android de ViewModels, repositorios y mapeo de datos.
- [ ] **REQ-12.6**: Compilación exitosa verificada del APK en CI.
- [ ] **REQ-12.7**: Guía paso a paso de validación en el teléfono realme GT Neo 2 (Doze, ahorro de batería, permisos de notificación).
- [ ] **REQ-12.8**: Mediciones reales de uso de RAM y CPU en la VM e2-micro para certificar que el backend cabe holgadamente en 1 GB de RAM.

## 13. Despliegue y Retirada del Bot
- [ ] **REQ-13.1**: Copia de seguridad consistente y recuperable de la base de datos SQLite y del código del bot original.
- [ ] **REQ-13.2**: Deshabilitación de automatizaciones previas de despliegue que pudieran pisar el nuevo backend.
- [ ] **REQ-13.3**: Plan de transición por fases con rollback inmediato disponible en caso de fallo.
- [ ] **REQ-13.4**: Validación de notificaciones reales en el dispositivo antes de proceder a la retirada definitiva del bot.
- [ ] **REQ-13.5**: Guía documentada de mantenimiento, actualización y resolución de incidencias.

---

## Verificación Formal de Coherencia con la Especificación Original
- [x] **CONF-01**: **Presupuesto 0,00 €**: Cumple estrictamente. Sin recursos de pago, sin facturación obligatoria (Firebase Spark), límite $0.00 en GitHub Actions y VM e2-micro permanente en Free Tier.
- [x] **CONF-02**: **Recursos de la VM (1 GB RAM)**: Cumple estrictamente. Se elimina Firefox/Selenium (-450 MB). FastAPI + SQLite opera en <50 MB con límite Docker de 256 MB, dejando >650 MB libres en el sistema.
- [x] **CONF-03**: **Dispositivo realme GT Neo 2 (Android 13 / Realme UI 4)**: Cumple estrictamente. Payload FCM data-only de alta prioridad para recepción inmediata y mitigación documentada de optimizaciones agresivas de batería.
- [x] **CONF-04**: **Sin IA en la aplicación final**: Cumple estrictamente. Arquitectura 100% determinista sin dependencias de LLMs.
- [x] **CONF-05**: **Sin compra ni reserva de billetes**: Cumple estrictamente. Solo monitorización de plazas y notificación informativa.
- [x] **CONF-06**: **Aislamiento del bot original**: Cumple estrictamente. Repositorio `Pablovb019/renfe-notifier-bot` inalterado.
- [x] **CONF-07**: **Frecuencia y concurrencia**: Cumple estrictamente. Agrupación por trayecto/fecha/plaza_h para 1-5 seguimientos respetando el ciclo de ~30s sin sobrecargar Renfe ni agotar el ancho de banda saliente de 1 GB/mes.
- [x] **CONF-08**: **Transporte seguro y cerrado a Internet**: Cumple estrictamente con la elección de Tailscale (WireGuard P2P), sin puertos expuestos públicamente.
- [x] **CONF-09**: **Despliegues y CI/CD**: Cumple estrictamente. Sin despliegues continuos automáticos; despliegue manual por `workflow_dispatch` con túnel IAP, verificación de commit y rollback automático en 30s.

