Actúa como ingeniero senior de Android y Python especializado en aplicaciones
personales, notificaciones fiables, servidores con recursos limitados y CI/CD.

Tu tarea es construir una app Android y un backend que sustituyan este bot:
https://github.com/Pablovb019/renfe-notifier-bot

Trabajaré con un agente de programación conectado mediante:
https://github.com/alishahryar1/free-claude-code

No presupongas que el modelo subyacente es Claude ni que tiene contexto,
herramientas o cuota ilimitados.

====================================================================
1. PRIMER PASO OBLIGATORIO: CREAR UN NUEVO REPOSITORIO
====================================================================

Antes de auditar o implementar, crea un repositorio NUEVO y PRIVADO en mi
cuenta de GitHub para la app Android y su backend.

Nombre propuesto: renfe-notifier-android.

- Comprueba la cuenta autenticada y si el nombre ya existe.
- Si existe, pregunta antes de reutilizarlo; nunca sobrescribas nada.
- Si no tienes acceso a GitHub, solicita autenticación segura, sin pedir
  que pegue tokens ni contraseñas en el chat.
- Si no puedes crearlo directamente, indícame los pasos mínimos para hacerlo.
  No afirmes haberlo creado si no lo has comprobado.
- No modifiques el repositorio original Pablovb019/renfe-notifier-bot.
- El nuevo repositorio alojará Android, backend y GitHub Actions.
- Inicialízalo sin secretos, datos personales, bases de datos ni claves.
- Conserva las licencias y atribuciones del código reutilizado; comprueba
  primero las condiciones de su licencia.
- Crear el repositorio no autoriza a desplegar ni a detener el bot.
- No cambies su visibilidad a pública sin mi autorización.

Después de crearlo, realiza la auditoría y presenta la arquitectura y el
plan de CI/CD para aprobación. No escribas todavía la aplicación.

====================================================================
2. OBJETIVO Y CONTEXTO CONFIRMADO
====================================================================

El bot actual funciona de forma fiable. Reutiliza su conocimiento de Renfe
y sus comportamientos útiles; no lo reescribas todo sin justificación.

Quiero sustituir Telegram por una aplicación Android nativa. Cuando el
reemplazo esté validado, dejaré de utilizar el bot.

Datos confirmados:
- Único usuario: yo.
- Instalación mediante APK, sin publicar en Google Play.
- Teléfono: realme GT Neo 2, 8 GB RAM, 128 GB de almacenamiento.
- Sistema: Realme UI 4, Android 13.
- Desarrollo y compilación local en PC Windows.
- Backend actual: Google Cloud Compute Engine e2-micro,
  2 vCPU compartidas y 1 GB RAM, utilizado dentro del Free Tier.
- Entre 1 y 5 seguimientos simultáneos.
- Objetivo de comprobación: aproximadamente cada 30 segundos.
- Avisos configurables: uno por aparición o recordatorios hasta confirmar.
- Empezar con seguimientos nuevos; no migrar los actuales.
- Quiero CI y despliegue mediante GitHub Actions.
- PRESUPUESTO TOTAL: 0 €. Es un requisito excluyente.

No vuelvas a preguntar estos datos. Pregunta solo por decisiones bloqueantes
que no puedas comprobar, agrupándolas.

====================================================================
3. REGLAS DE COSTE: CERO EUROS
====================================================================

- No contratar ni activar recursos con cobro automático, suscripciones,
  dominios de pago o pruebas temporales como solución permanente.
- No cambiar planes, vincular facturación ni ampliar cuotas por tu cuenta.
- La VM actual ya existe: no crear otra como sustitución automática.
- Comprobar elegibilidad y costes de región, horas agregadas, discos,
  direcciones IP, tráfico saliente, registros y recursos auxiliares.
- Distinguir Free Tier permanente de créditos promocionales.
- No asumir que utilizar una e2-micro garantiza una factura de cero euros.
- Los presupuestos de Google Cloud avisan; no garantizan un corte del gasto.
- Medir tráfico real y establecer límites conservadores de actividad.
  No presentar esos límites como garantía absoluta contra cobros.
- Si una solución no satisface 0 €, detener su despliegue y explicar
  el conflicto. Nunca resolverlo silenciosamente con un servicio de pago.
- No modificar facturación, red o recursos actuales sin autorización.
- No introducir APIs de IA en la aplicación final: FCC solo se utiliza
  como herramienta de desarrollo.
- Verificar las condiciones gratuitas actuales en documentación oficial,
  indicando la fecha de consulta y lo que no se haya podido comprobar.

====================================================================
4. AUDITORÍA INICIAL DEL REPOSITORIO ORIGINAL
====================================================================

Inspecciona el repositorio y registra el commit revisado.

Lee especialmente:
- python/renfechecker.py
- python/renfebot.py
- python/conversations.py
- python/dbmanager.py
- python/texts.py
- data/stations.json
- Pruebas existentes.
- requirements.txt
- Dockerfile y docker-compose.yml.
- Workflows de GitHub Actions.
- README y licencia.

Contrasta documentación y código. Si alguno de esos archivos ha cambiado
o no existe, localiza su equivalente.

No imprimas ni envíes al modelo secretos, bases privadas o credenciales.

Comprueba estos hallazgos de una revisión previa; podrían haber cambiado:
- RenfeChecker inicia Firefox/Xvfb antes de saber si los necesita.
- La vía HTTP/DWR precede al fallback Selenium.
- Cada búsqueda crea requests.Session y hace cinco POST explícitos;
  dos corresponden a generateId. No elimines uno sin verificar su función.
- check_followups procesa seguimientos secuencialmente con asyncio.to_thread.
- get_active_followups excluye el estado notifying, mientras otra tarea
  repite avisos cada 10 segundos sin comprobar de nuevo las plazas.
- El matching de un tren utiliza solamente salida y llegada.
- SQLite combina fechas locales implícitas con referencias explícitas
  a Europe/Madrid.
- Revisar el cierre de sesiones HTTP, conexiones SQLite y navegador,
  también cuando se producen excepciones.

Entrega una auditoría breve con:
1. Problema y evidencia por archivo/función.
2. Impacto en fiabilidad, consumo o mantenimiento.
3. Qué conservar, adaptar y eliminar.
4. Costes, incertidumbres y validaciones pendientes.
5. Arquitectura recomendada.
6. Plan de CI/CD.
7. Preguntas realmente bloqueantes.

Distingue hallazgos comprobados de hipótesis y recomendaciones.
No atribuyas al código problemas que no hayas verificado.

====================================================================
5. ARQUITECTURA BASE PROPUESTA
====================================================================

Punto de partida:
Android Kotlin + Jetpack Compose + backend Python ligero en la VM +
SQLite persistente + Firebase Cloud Messaging.

Valida esta propuesta antes de implementarla. Si no satisface los requisitos,
explica por qué y plantea la alternativa mínima.

No desarrollar una web/PWA ni envolver Telegram en una WebView.
No usar Kubernetes, Redis, Celery, PostgreSQL, múltiples servidores o
servicios serverless sin una necesidad demostrable y coste cero verificado.

BACKEND
- Monolito modular: adaptador Renfe, lógica de seguimiento, persistencia,
  planificador, transporte de notificaciones y API.
- FastAPI es una opción razonable; justificar cualquier alternativa.
- Un único propietario del planificador.
- Evitar duplicar comprobaciones al iniciar múltiples workers.
- SQLite persistente con migraciones versionadas, índices adecuados,
  transacciones, busy_timeout y WAL cuando corresponda.
- SQL parametrizado y conexiones cerradas correctamente.
- No mantener transacciones abiertas mientras se consulta la red.
- Ajustar imagen y procesos a 1 GB de RAM.
- Medir RAM y CPU antes de fijar límites.
- No compilar Android ni ejecutar modelos de IA en la VM.

ANDROID
- Kotlin, Compose, ViewModel, coroutines/Flow y repositorios sencillos.
- Un módulo inicial organizado por funcionalidades; evitar capas vacías.
- Backend como fuente de verdad de los seguimientos.
- Room solo si aporta caché o historial offline.
- DataStore para preferencias.
- No mostrar una operación remota fallida como guardada.
- Versiones estables y compatibles de JDK, Gradle, AGP, Kotlin y SDK.
- Verificar funcionamiento en Android 13.
- No elegir un targetSdk antiguo para esquivar restricciones del sistema.

====================================================================
6. ACCESO A RENFE Y EFICIENCIA
====================================================================

- Reutilizar el protocolo HTTP/DWR existente antes de cambiar librerías.
- No inventar una API oficial ni contratos no comprobados.
- Agrupar seguimientos por todos los parámetros que afectan a la respuesta:
  estaciones, fecha, Plaza H y otros filtros relevantes.
- Una búsqueda por grupo debe poder atender varios trenes de ese grupo.
- Compartir búsquedas manuales equivalentes cuando sea seguro.
- No compartir cookies o estado de búsqueda concurrente sin aislamiento.
- Evaluar reutilización de sesiones y conexiones con pruebas.
- Conservar el flujo funcional si la optimización no está demostrada.
- Concurrencia global pequeña: inicialmente 1–2, ajustada con mediciones.
- Un grupo no puede ejecutar dos comprobaciones simultáneas.
- Si una comprobación supera 30 segundos, no acumular tareas atrasadas.
- Timeouts por petición y presupuesto total por comprobación.
- Backoff con jitter, respeto a Retry-After y tratamiento de 429/403.
- No evadir CAPTCHA, bloqueos ni controles de acceso.
- No consultar sin seguimientos activos.
- Distinguir error, respuesta inválida, dato antiguo y falta de plazas.
- Revisar la semántica de Plaza H con fixtures y observaciones reales:
  no asumir sin pruebas que soloPlazaH expresa toda la disponibilidad accesible.
- Mantener el precio desconocido como desconocido, no convertirlo en cero.
- Medir peticiones y bytes reales: el intervalo por sí solo no permite
  estimar correctamente tráfico ni coste.

NAVEGADOR
- No iniciarlo al arrancar el backend.
- Si HTTP funciona, mantenerlo fuera del despliegue habitual.
- No eliminar definitivamente el fallback sin comprobar su necesidad.
- Si es imprescindible: opcional, bajo demanda, serializado,
  cierre por inactividad y límite de recursos.
- Medir su comportamiento en la e2-micro.
- Si no cabe de forma fiable en 1 GB, explicarlo; no ampliar la VM.

====================================================================
7. MODELO DE SEGUIMIENTOS Y CORRECCIÓN
====================================================================

Separar tres conceptos:
1. Ciclo de vida: activo, pausado, vencido o eliminado.
2. Disponibilidad: desconocida, no disponible o disponible.
3. Aviso: pendiente, enviado o confirmado para un episodio concreto.

Requisitos:
- Seguir comprobando aunque ya se haya enviado un aviso.
- Avisar ante una nueva aparición válida de disponibilidad.
- No crear un episodio nuevo por un error de red o una respuesta obsoleta.
- Confirmar un aviso no borra automáticamente el seguimiento.
- Pausar y eliminar son acciones separadas.
- Recordatorios configurables y acotados.
- Cancelarlos al confirmar, pausar, vencer o desaparecer la disponibilidad.
- No repetir indefinidamente una disponibilidad cuya comprobación haya caducado.
- Definir qué ocurre si aparecen otros trenes en un seguimiento de “todos”.
- Identificar trenes con ID real cuando exista.
- Documentar y probar el fallback cuando no exista.
- Guardar instantes en UTC.
- Interpretar fechas de viaje y mostrar información en Europe/Madrid.
- Probar cambios de hora, medianoche y llegadas al día siguiente.
- Mantener inicialmente la caducidad de 30 días con renovación, limitada
  por la salida, salvo simplificación propuesta y aprobada.
- En “todos los trenes”, tratar correctamente el final del día.

====================================================================
8. NOTIFICACIONES ANDROID Y FCM
====================================================================

- Utilizar FCM sin Cloud Functions ni productos innecesarios.
- Preferir Firebase Spark separado, sin facturación vinculada, si resulta
  compatible; verificar configuración y permisos entre proyectos.
- Enviar desde la VM mediante Admin SDK o HTTP v1.
- Credenciales de envío exclusivamente en servidor.
- Preferir identidad de la VM con permisos mínimos cuando sea viable.
- Nunca incluir credenciales de servidor en el APK.
- Comprobar que el teléfono tiene Google Play services operativos.
- Solicitar POST_NOTIFICATIONS en Android 13 y gestionar su denegación.
- Implementar canales, sonido/vibración y acceso a ajustes del sistema.
- Alta prioridad únicamente para contenido urgente y visible.
- Payload suficiente para mostrar el aviso sin esperar otra consulta.
- Diseñar explícitamente data/notification payload y sus diferencias
  en primer y segundo plano, evitando avisos duplicados.
- Incluir event_id, identificador de episodio, observed_at y caducidad breve.
- Gestionar idempotencia, renovación del token FCM y tokens inválidos.
- Persistir eventos pendientes en SQLite con reintentos acotados.
- No prometer entrega exactamente una vez: deduplicar en servidor y cliente.
- Acciones: abrir seguimiento, confirmar aviso y pausar.
- No usar FCM como base de datos ni como sustituto de autenticación.
- No prometer recordatorios exactos cada 10 segundos con pantalla apagada.
- Si esa cadencia resulta necesaria, explicar restricciones y proponer
  una alternativa aprobada sin abusar de mensajes de alta prioridad.
- WorkManager no sirve para vigilancia periódica cada 30 segundos.
- No mantener polling, WebSocket, wake lock o servicio permanente
  en el teléfono como estrategia principal.

Distinguir:
frecuencia de consulta → detección → envío → entrega → visualización.

Treinta segundos es un objetivo de consulta, no una garantía extremo a extremo.

Documentar Doze, ahorro de batería, conectividad, cierre desde recientes
y forzar detención como situaciones diferentes.
No prometer recepción tras forzar detención hasta que Android permita
reanudar la aplicación.

====================================================================
9. SEGURIDAD Y CONEXIÓN
====================================================================

- Conexión cifrada y autenticada entre Android y backend.
- No asumir que ya existe dominio, HTTPS o IP gratuita.
- Proponer acceso permanente compatible con 0 €, contrastado con
  documentación oficial.
- No comprar dominio ni crear balanceador o Cloud NAT.
- No presentar túneles temporales como despliegue estable.
- Nunca desactivar la validación TLS.
- Para un único usuario, preferir emparejamiento seguro y credencial
  revocable por dispositivo antes que registro público.
- No incrustar un secreto compartido en el código o APK.
- Proteger credenciales locales con Android Keystore cuando corresponda.
- Autenticar búsquedas, seguimientos, registro de tokens y confirmaciones.
- Validar fechas, estaciones, límites e identificadores.
- Impedir que el cliente indique URLs arbitrarias para consultar desde el servidor.
- Limitar peticiones y registros sin servicios externos de pago.
- Logs sin tokens, cookies ni credenciales.
- Diseñar la exposición de red de la API y el acceso administrativo
  como problemas separados.

====================================================================
10. FUNCIONALIDAD A CONSERVAR
====================================================================

- Estaciones, alias, búsqueda tolerante a tildes y grupos de estaciones.
- Origen, destino, fecha y filtro Plaza H.
- Tren concreto, primero, último y todos, respetando la semántica del bot.
- Crear, listar, pausar, reanudar y eliminar seguimientos.
- Reutilizar ruta y fecha para otra búsqueda.
- Mostrar disponibilidad y última comprobación válida.
- Diferenciar errores de ausencia de plazas.
- Estadísticas útiles, separando comprobaciones lógicas y peticiones HTTP.
- Interfaz en español, accesible y con tema claro/oscuro.
- Pantalla de diagnóstico.
- Envío de una notificación de prueba claramente identificada.
- No reservar ni comprar billetes automáticamente.

====================================================================
11. CI/CD OBLIGATORIO CON GITHUB ACTIONS
====================================================================

Toda la automatización vivirá en el nuevo repositorio.

PRESUPUESTO
- Comprobar cuotas vigentes de minutos, almacenamiento y artefactos
  de mi cuenta para repositorios privados.
- No activar consumo facturable ni permitir sobrecostes.
- Si no se puede garantizar que la configuración impida consumo pagado,
  señalarlo antes de habilitar workflows.
- Si se agota la cuota gratuita, detener la automatización y ofrecer
  compilación/despliegue manual desde Windows.
- No hacer público el repositorio para evitar costes sin mi permiso.

CI
- Ejecutar en pull requests y cambios en main.
- Backend: análisis estático y pruebas automatizadas.
- Android: análisis estático, pruebas unitarias y compilación del APK.
- Ejecutar solo los trabajos afectados por los cambios.
- Cambios compartidos deben activar las comprobaciones correspondientes.
- Diseñar filtros sin dejar checks obligatorios pendientes indefinidamente.
- Usar runners Linux alojados por GitHub.
- No alojar un runner ni compilar Android en la e2-micro.
- Cachés adecuadas, tiempos máximos y cancelación de ejecuciones obsoletas.
- Evitar matrices enormes y emuladores en cada commit.
- Retención corta y controlada de artefactos.
- No consultar Renfe ni acceder a producción desde las pruebas.
- Las pull requests no necesitan secretos de producción.
- Evitar ejecutar código no confiable mediante pull_request_target.
- Permisos mínimos del GITHUB_TOKEN y acciones fijadas por SHA.
- No publicar secretos en logs ni artefactos.

CD DEL BACKEND
- Desplegar únicamente versiones de main que hayan superado CI.
- Empezar con activación manual mediante workflow_dispatch.
- Verificar los checks del commit exacto que se desplegará.
- Impedir desplegar otra rama mediante manipulación de entradas.
- Primer despliegue y retirada del bot requieren mi aprobación.
- Usar mecanismo de acceso seguro compatible con coste cero.
- Preferir OIDC/Workload Identity Federation cuando encaje.
- Restringir confianza por repositorio y rama o entorno autorizado.
- No asumir que autenticarse con Google Cloud proporciona por sí solo
  conectividad o permisos de acceso a la VM.
- No depender de aprobaciones de GitHub Environments sin comprobar
  que están disponibles gratuitamente para mi repositorio privado.
- Serializar despliegues para evitar ejecuciones simultáneas.
- No cancelar un despliegue a mitad de una migración.
- Identificar versiones por commit.
- Respaldar SQLite de forma consistente antes de migrar.
- Conservar los datos fuera del directorio reemplazado durante el despliegue.
- Comprobar salud después de actualizar.
- Conservar una versión anterior y un procedimiento de rollback.
- El rollback debe contemplar compatibilidad de la base de datos:
  no revertir binarios a ciegas después de una migración incompatible.
- No crear registros de imágenes, balanceadores u otros recursos de pago.
- Elegir entrega de código o artefacto según consumo real de RAM,
  disco, tráfico y cuotas, justificando la decisión.
- Nunca desplegar secretos dentro de imágenes o artefactos.

ENTREGA ANDROID
- Generar APK de prueba como artefacto de CI.
- Generar APK de distribución firmado mediante ejecución manual
  o etiqueta de versión autorizada.
- Verificar que el commit de distribución pertenece a main y superó CI.
- No publicar en Google Play.
- Proteger clave de firma y contraseñas con secretos de GitHub.
- No exponerlas a workflows de pull requests.
- Crear archivos temporales de firma solo durante el trabajo necesario.
- Nunca incluirlos en cachés, logs o artefactos.
- Mantener la misma firma para futuras actualizaciones.
- Gestionar versionCode y versionName de forma reproducible.
- Entregar el APK y su checksum para descarga.
- Distribuir mediante artefactos o releases privadas tras verificar cuotas.
- La instalación y actualización en el teléfono serán manuales.
- No confundir publicar un APK con instalarlo automáticamente.

====================================================================
12. PRUEBAS Y VALIDACIÓN
====================================================================

Antes de sustituir el bot:

BACKEND
- Tests del parser DWR, estaciones, Plaza H y matching de trenes.
- Tests de transiciones, confirmaciones, recordatorios y caducidades.
- Tests de errores, reinicios y persistencia del planificador.
- Tests de deduplicación de búsquedas y avisos.
- Tests de autenticación, validación y control de acceso.
- Tests de migraciones y recuperación.

ANDROID
- Compilación real del APK.
- Pruebas unitarias y comprobaciones de UI disponibles.
- Validación de permisos y canales.
- Comprobación de acciones de las notificaciones.
- Guía de prueba en el realme: pantalla apagada, Doze, reinicio,
  Wi-Fi/datos, permisos denegados y restricciones de batería.
- No inventar nombres o rutas de menús de Realme UI.
- Si no puedes acceder al dispositivo, marcar esas pruebas como pendientes.

CI/CD
- Validar sintaxis y comportamiento de workflows.
- Comprobar que CI no recibe secretos innecesarios.
- Comprobar que solo se despliegan commits autorizados con CI superada.
- Probar fallos de despliegue y procedimiento de recuperación.
- No activar pruebas destructivas en producción.

RECURSOS
- Medir CPU, RAM, disco, tráfico y duración de comprobaciones.
- Comparar 1 y 5 seguimientos, con y sin agrupación.
- Verificar recursos del navegador si se mantiene.
- Evaluar consumo Android sin atribuirle mediciones no realizadas.

Usar fixtures en pruebas automatizadas; no bombardear Renfe.
Las pruebas reales serán pocas, explícitas y separadas de simulaciones.
No afirmar que algo funciona solo porque compila.
Distinguir implementado, probado localmente y validado en dispositivo/VM.

====================================================================
13. DESPLIEGUE Y RETIRADA DEL BOT
====================================================================

- Trabajar en ramas del nuevo repositorio.
- No modificar ni destruir el repositorio original.
- Crear base nueva para la aplicación.
- No importar los seguimientos actuales.
- Preservar copia recuperable del bot y de SQLite.
- Para SQLite en uso, utilizar un mecanismo de copia consistente;
  no copiar únicamente el archivo principal ignorando WAL.
- Revisar automatizaciones antiguas para evitar que vuelvan a desplegar
  el bot sobre el nuevo backend.
- No mantener durante días un sondeo duplicado de ambos sistemas.
- Proponer transición controlada:
  1. Preparar y validar fuera de producción.
  2. Solicitar aprobación.
  3. Detener el bot.
  4. Iniciar el backend nuevo.
  5. Comprobar consultas y notificaciones reales.
  6. Restaurar el bot si falla.
- No dar el reemplazo por terminado hasta validar avisos reales.
- Documentar qué automatización antigua debe deshabilitarse, sin hacerlo
  sobre el repositorio original sin autorización.
- Entregar instrucciones de instalación y actualización del APK.
- Respaldar la clave de firma de forma segura fuera de Git.
- Documentar configuración, costes, recuperación y mantenimiento.

====================================================================
14. FORMA DE TRABAJAR CON FREE-CLAUDE-CODE
====================================================================

- Primero crear el repositorio; después auditar y proponer.
- Esperar aprobación de arquitectura y CI/CD antes de implementar.
- Implementar por hitos pequeños, compilables y verificables.
- No generar todo el proyecto en una única respuesta.
- Mantener CLAUDE.md breve con instrucciones estables.
- Mantener un archivo de progreso con decisiones, archivos modificados,
  pruebas realizadas, bloqueos y siguiente paso.
- No guardar secretos en esos documentos.
- Leer solo contexto relevante.
- Evitar volcar archivos enormes o logs completos.
- No lanzar agentes paralelos masivos ni reintentos ilimitados.
- No suponer que FCC añade capacidades que el modelo no tiene.
- Usar únicamente proveedores y modelos explícitamente gratuitos
  y verificados para mi cuenta.
- Nunca configurar fallback a modelos de pago.
- Ante cuota agotada, guardar progreso y detenerse.
- No intentar eludir límites de proveedores.
- No afirmar haber ejecutado herramientas, compilado o desplegado
  si no se ha hecho realmente.
- Para cada hito, resumir cambios, pruebas, pendientes y siguiente paso.

====================================================================
15. ENTREGABLES
====================================================================

1. Nuevo repositorio privado en mi GitHub.
2. Auditoría con referencias concretas al código original.
3. Arquitectura y viabilidad de coste cero aprobadas.
4. App Android nativa funcional.
5. Backend ligero con SQLite.
6. Notificaciones FCM y configuración segura.
7. Pruebas automatizadas.
8. CI y CD mediante GitHub Actions.
9. APK firmado instalable y actualizable.
10. Documentación breve para Windows, VM y realme.
11. Procedimiento de transición y recuperación del bot.
12. Relación honesta de comprobaciones realizadas y pendientes.

EMPIEZA AHORA:
Crea primero el nuevo repositorio privado renfe-notifier-android.
Después audita el repositorio original y presenta la arquitectura,
la viabilidad de coste cero y el plan de GitHub Actions.
Pregunta solo por bloqueos reales.
No escribas todavía la app ni despliegues nada.