# Estudio de Viabilidad y Restricción de Coste Cero (0,00 €)

- **Fecha de consulta y verificación**: 12 de septiembre de 2026
- **Presupuesto asignado**: **0,00 €** (Requisito excluyente)
- **Principio rector**: No contratar ni activar ningún recurso de pago, suscripción, dominio comercial ni prueba temporal que genere cobros automáticos posteriores.

---

## 1. Google Cloud Compute Engine (VM `e2-micro`)

### 1.1. Condiciones Oficiales Publicadas del Free Tier Permanente
- **Fuente oficial**: [Google Cloud Free Program Features](https://cloud.google.com/free/docs/free-cloud-features) / [Compute Engine Pricing](https://cloud.google.com/compute/pricing)
- **Cuota mensual gratuita incluida**:
  1. **Instancia de cómputo**: 1 instancia VM no interrumpible `e2-micro` (2 vCPUs compartidas, 1 GB de memoria RAM) por mes.
  2. **Regiones elegibles**: La instancia **debe residir obligatoriamente** en una de las siguientes zonas de Estados Unidos:
     - `us-west1` (Oregon)
     - `us-central1` (Iowa)
     - `us-east1` (Carolina del Sur)
     *Nota crítica*: Si la VM se ubica en cualquier otra región (por ejemplo, Europa `europe-west1` o `europe-west4`), la instancia **no es gratuita** y facturará a la tarifa estándar (~$7,50–$9,00/mes).
  3. **Horas agregadas**: Se incluyen hasta **744 horas de instancia al mes** agregadas en las regiones elegibles.
     - 1 sola instancia encendida 24/7 consume como máximo 744 horas (31 días × 24 h).
     - Si se arranca una segunda instancia simultánea, las horas se suman y se facturará el exceso a partir de la hora 745.
  4. **Disco persistente**: Hasta **30 GB-mes de disco estándar (`pd-standard`)**.
     - El disco debe ser de tipo estándar (`pd-standard`). Si se aprovisiona como `pd-ssd` (SSD) o `pd-balanced`, generará costes (~$0,10–$0,17/GB-mes).
  5. **Snapshots de disco**: Hasta 5 GB-mes de almacenamiento de instantáneas en regiones seleccionadas.
  6. **Tráfico saliente de red (Egress)**: Hasta **1 GB al mes** de salida de red desde Norteamérica hacia destinos mundiales (excluyendo China y Australia).
     - Una vez superado 1 GB/mes, el tráfico de salida a Internet hacia Europa tiene un coste de ~$0,12 por GB.
  7. **Direcciones IP externas**:
     - Desde 2024, Google Cloud introdujo facturación para todas las direcciones IPv4 externas (~$0,005/h). Sin embargo, Google Cloud mantiene dentro del Free Tier la exención de 1 dirección IPv4 efímera en uso asociada a la instancia `e2-micro` en las regiones participantes durante 744 horas al mes.
     - Si se reserva una IP estática sin asociar, o se asocian múltiples IPs, se aplicará el cargo de IP (~$3,60/mes).
  8. **Cloud Logging / Cloud Monitoring**:
     - Ingestión gratuita de hasta **50 GiB al mes** por proyecto con retención estándar de 30 días. Superados los 50 GiB, el coste es de $0,50/GiB.

### 1.2. Estimación de Tráfico y Consumo de Red en el Proyecto
- Cada ciclo de comprobación HTTP/DWR de Renfe (5 peticiones POST y sus respuestas JSON) transfiere aproximadamente entre 15 KB y 25 KB combinados.
- **Cálculo de tráfico en sondeo**:
  - Sondeo continuo cada 30 segundos = 2 comprobaciones/minuto = 120/hora = 2.880 ciclos/día.
  - 2.880 ciclos × 20 KB = ~57,6 MB/día = **~1,72 GB/mes** si se consultase ininterrumpidamente las 24 horas durante 30 días.
  - *Riesgo identificado*: Si el bot consulta de forma ininterrumpida sin descansos ni agrupaciones durante un mes entero, el tráfico saliente podría superar el límite gratuito de 1 GB en ~720 MB, generando un coste potencial de ~$0,09 a $0,15 USD.
- **Medidas de mitigación en software para garantizar 0 €**:
  1. **Sin consultas en reposo**: Si no hay seguimientos activos configurados, el planificador no realiza ninguna llamada de red a Renfe.
  2. **Agrupación inteligente**: Una única consulta compartida sirve para todos los seguimientos que coincidan en origen, destino y fecha.
  3. **Pausas / Límite de horario**: Configurar los seguimientos para no consultar trenes en franjas nocturnas irrelevantes o trenes ya pasados.

### 1.3. Cloud Logging: Prevención de Exceso de Cuota
- Si el backend registra un evento de log completo por cada comprobación de 30 segundos (2.880 líneas/día), se generan unos 2–5 MB de logs al mes, muy lejos de los 50 GiB gratuitos.
- No obstante, para evitar saturación de disco local o llamadas de Cloud Logging API:
  - Los logs deben emitirse por `stdout`/`stderr` dentro del contenedor con rotación de archivos locales (`max-size=10m`, `max-file=3`).
  - Nivel de log por defecto: `INFO` (con `WARNING` para reintentos normales), evitando el nivel `DEBUG` en producción.

---

## 2. Diferencia entre Free Tier Permanente y Créditos Promocionales

| Concepto | Google Cloud Free Tier Permanente | Créditos de Prueba Gratuita ($300 Free Trial) |
|---|---|---|
| **Duración** | Ilimitada / Permanente (mientras se respeten los límites mensuales). | 90 días desde la activación de la cuenta. |
| **Recursos** | 1 VM `e2-micro`, 30 GB disco estándar, 1 GB egress, 50 GiB logs. | Créditos en dólares consumibles en cualquier servicio. |
| **Vencimiento** | No vence; se renueva cada mes natural. | Caduca a los 90 días o cuando se consumen los $300. |
| **Validez para este proyecto** | **La única base aceptable** para sostener el proyecto a coste cero. | **No computable**: no se puede basar la viabilidad en saldo temporal. |

---

## 3. Firebase Cloud Messaging (FCM) y Plan Spark

- **Fuente oficial**: [Firebase Pricing - Spark Plan](https://firebase.google.com/pricing)
- **Condiciones oficiales**:
  - **Plan Spark**: Gratuito de por vida. **No requiere vincular tarjeta de crédito ni cuenta de facturación** si se crea como proyecto puro de Firebase o si se mantiene dentro del tier Spark.
  - **Firebase Cloud Messaging (FCM)**: Es un producto **100% gratuito sin límite de mensajes** ni de dispositivos en el plan Spark.
  - **Uso desde servidor**: El backend en la VM interactúa con la API de FCM mediante HTTP v1 o `firebase-admin` (Python). No se requiere Cloud Functions, Firestore de pago ni ningún otro servicio adicional de Firebase.
- **Garantía de coste cero**: Al no requerir facturación vinculada, no existe posibilidad de generación de deuda o cargos por el uso de notificaciones push.

---

## 4. GitHub Actions en Repositorio Privado

- **Fuente oficial**: [GitHub Actions Billing and Limits](https://docs.github.com/en/billing/managing-billing-for-your-products/managing-billing-for-github-actions/about-billing-for-github-actions)
- **Cuotas del plan GitHub Free para repositorios privados**:
  1. **Minutos de computación**: **2.000 minutos gratuitos al mes** para runners estándar alojados en GitHub (runners Linux consumen multiplicador 1x).
  2. **Almacenamiento de artefactos y paquetes**: **500 MB gratuitos**.
  3. **Caché de GitHub Actions**: Hasta **10 GB por repositorio** (las cachés no utilizadas durante más de 7 días son eliminadas automáticamente; si se supera el límite, se evicta la más antigua bajo política LRU sin coste alguno).
- **Consumo proyectado del proyecto**:
  - Pipeline de CI de backend (linting + tests unitarios con fixtures): ~1–2 minutos por ejecución.
  - Pipeline de CI de Android (linting + compilación de APK de depuración): ~4–6 minutos por ejecución.
  - Con un promedio de 10 a 20 commits/PRs al mes, el consumo mensual estimado es de 100 a 160 minutos (menos del 8% de la cuota mensual de 2.000 minutos).
  - Retención de artefactos configurada en **3 días** (máximo 50–100 MB de APK temporal), muy por debajo de los 500 MB.
- **Mecanismo de seguridad absoluta contra consumo facturado**:
  - En los ajustes de la cuenta de GitHub:
    `Settings -> Billing and plans -> Spending limits -> Actions -> Set to $0.00 USD`.
  - Con el límite de gasto fijado en **0,00 $**, si por cualquier eventualidad se superasen los 2.000 minutos o los 500 MB, **GitHub corta automáticamente las ejecuciones adicionales** devolviendo error de cuota sin cobrar jamás un céntimo.

---

## 5. Acceso Cifrado Permanente sin Dominio de Pago

Uno de los mayores riesgos de coste en la nube es la contratación de dominios comerciales, direcciones IP elásticas reservadas o balanceadores de carga administrados (Cloud Load Balancing cuesta ~$18/mes base). Para mantener el coste en **0,00 €**, se evalúan las siguientes alternativas técnicas:

| Solución | Coste | Cifrado TLS / Seguridad | Resistencia a IP Cambiante | Viabilidad Técnica |
|---|---|---|---|---|
| **Opción 1: Tailscale (WireGuard P2P + MagicDNS HTTPS)** | **0,00 €** (Plan Personal gratuito hasta 3 usuarios y 100 nodos) | Cifrado punto a punto WireGuard (ChaCha20-Poly1305) + HTTPS con certificados Let's Encrypt automáticos (`.ts.net`). | Excelente: Conexión P2P directa independientemente de IP pública o CGNAT. No requiere abrir puertos en la VM. | **Opción Recomendada**: Elimina superficie de ataque externa, no requiere dominio de pago y funciona nativamente en Android y Linux. |
| **Opción 2: DuckDNS + Let's Encrypt (Certbot)** | **0,00 €** (Servicio gratuito comunitario) | HTTPS estándar con certificado público válido emitido por Let's Encrypt. | Media: Requiere script en la VM para actualizar la IP pública efímera cada vez que cambie. Requiere abrir puerto 443 al mundo. | **Opción Secundaria Viable**: Expuesta a escaneos de bots en Internet si no se filtra por firewall. |
| **Opción 3: IP directa efímera + SSL Pinning** | **0,00 €** | Cifrado TLS estricto con certificado autofirmado validado mediante `network_security_config.xml` en Android. | Mala: Cada vez que la VM se reinicia o cambia la IP efímera, el APK debe recompilarse o actualizarse. | Descartada por mala mantenibilidad. |
| **Opción 4: Cloudflare Tunnels (`cloudflared`)** | **0,00 €** (Zero Trust free) | HTTPS gestionado por Cloudflare. | Alta: Túnel saliente sin abrir puertos. | Requiere un dominio propio registrado en Cloudflare (los dominios de prueba rápidos son efímeros). |

**Recomendación de arquitectura**: Utilizar **Tailscale** o **DuckDNS + Let's Encrypt**. Ambas opciones garantizan 0,00 € de gasto de forma permanente y cumplen rigurosamente la prohibición de relajar o desactivar la verificación TLS.

---

## 6. Distinción entre Alertas Presupuestarias y Corte Automático

> [!WARNING] **AVISO CRÍTICO SOBRE GOOGLE CLOUD BUDGETS**:
> Las alertas de presupuesto de Google Cloud (`Billing -> Budgets & alerts`) **son notificaciones reactivas por correo electrónico o Pub/Sub**.
> **NO DETIENEN LAS MÁQUINAS NI CORTAN EL SERVICIO AUTOMÁTICAMENTE**.
> Si se configura una alerta de presupuesto a 0,01 €, Google enviará un email cuando se detecte el cobro, pero los recursos continuarán funcionando y generando costes hasta que sean detenidos manualmente.

Por este motivo, la garantía de coste cero **no descansa en las alertas de Google Cloud**, sino en:
1. Respetar de forma milimétrica los límites arquitectónicos del Free Tier.
2. Fijar el "Spending Limit" de GitHub Actions en 0,00 $.
3. Usar el plan Spark sin facturación vinculada en Firebase.
4. Descartar servicios de pago o balanceadores en la nube.

---

## 7. Tabla Comparativa: Condiciones Publicadas vs. Configuración Real Pendiente de Verificación

| Parámetro | Condición Oficial Publicada | Estado Real en la Cuenta del Usuario | Punto de Verificación Requerido |
|---|---|---|---|
| **Región VM** | `us-central1`, `us-east1` o `us-west1` | Pendiente de comprobación | Verificar en consola GCP la zona de la instancia existente. |
| **Tipo de máquina** | `e2-micro` (2 vCPU, 1 GB RAM) | Pendiente de comprobación | Verificar que la VM es exactamente `e2-micro` y no `e2-small` ni `n1`. |
| **Tipo de disco** | `pd-standard` ≤ 30 GB | Pendiente de comprobación | Verificar que el disco asignado no sea `pd-ssd` ni `pd-balanced`. |
| **Instancias activas** | Máximo 1 instancia en ejecución | Pendiente de comprobación | Verificar que no haya otras VMs encendidas consumiendo la cuota de 744 h. |
| **Límite GitHub Actions** | $0.00 Spending limit | Pendiente de comprobación | Verificar en ajustes de facturación de GitHub. |
| **Firebase Billing** | Plan Spark (sin tarjeta vinculada) | Pendiente de comprobación | Crear proyecto Firebase independiente sin asociar cuenta de facturación. |

---

## 8. Conclusión de Viabilidad y Bloqueos de Despliegue

- **Conclusión técnica**: El proyecto **ES 100% VIABLE A COSTE CERO (0,00 €)** siempre que:
  1. La VM existente se encuentre ubicada en una de las 3 regiones gratuitas de EE.UU. con disco estándar de hasta 30 GB.
  2. No se agregue una segunda VM en la cuenta.
  3. No se utilice Selenium ni navegadores pesados que desborden la memoria de 1 GB.
  4. El acceso móvil se resuelva mediante Tailscale o DuckDNS sin balanceadores de pago.
  5. Se aplique el límite de gasto 0 $ en GitHub Actions y el plan Spark en Firebase.
- **Bloqueo preventivo de despliegue**:
  - Antes de realizar cualquier despliegue en la VM, se deberá solicitar al usuario la confirmación de la región de la VM y el tipo de disco. No se autoriza ningún despliegue si la VM está fuera de las regiones gratuitas.
