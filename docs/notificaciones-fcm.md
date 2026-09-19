# Transporte FCM (paso 19)

Diseño explícito del envío de notificaciones desde el servidor. **En este paso
nada se envía contra FCM real**: solo transporte implementado y probado con un
emisor simulado (`httpx.MockTransport` + token estático). La configuración real
(hacer clic en "Firebase" en la VM, crear el proyecto, etc.) queda documentada y
pendiente en el paso 33.

## 1. Transporte y autenticación

- **HTTP v1** (`https://fcm.googleapis.com/v1/projects/{project_id}/messages:send`),
  sin Admin SDK ni Cloud Functions.
- Token OAuth2 efímero obtenido en tiempo de ejecución mediante
  **Application Default Credentials** (ADC):
  - En la VM: **identidad de la instancia** (permisos mínimos, sin archivos de
    credenciales en el repositorio). Resuelta por el servidor de metadatos
    (`GceMetadataTokenProvider`) o por google-auth cuando está instalado.
  - En local: cuenta de servicio vía `GOOGLE_APPLICATION_CREDENTIALS`
    (requiere `pip install google-auth`; es opcional y seguro).
- Las credenciales **nunca se leen en el contexto del modelo ni viajan por la
  API**: el backend solo intercambia el access token contra FCM.
- `AutoTokenProvider`: intenta first ADO con google-auth y cae al metadato de
  GCE; si ambos fallan lanza `FcmNotConfiguredError` (la API responde 503).

## 2. Contrato de payload (data-only, REQ-08.5)

Para distinguir primer/segundo plano y evitar avisos duplicados, se envían
**mensajes `data` completos sin bloque `notification`**. La app los muestra como
notificación nativa con **toda la información necesaria, sin ninguna consulta
adicional de red** (el backend ya la verificó antes de enviar).

| Clave              | Formato / ejemplo                          | Uso por la app                      |
|-------------------|--------------------------------------------|-------------------------------------|
| `type`            | `alert` \| `test`                          | Ruta de renderizado / diagnóstico   |
| `event_id`        | cadena estable del aviso                   | Deduplicación cliente + servidor    |
| `followup_id`     | id del seguimiento                         | Abrir detalle, acciones             |
| `episode_id`      | entero del episodio                        | Sincronización de estado            |
| `title`/`body`    | texto visible                              | Notificación `disponibilidad_plazas_v2`|
| `origin_code`     | `60000` (Atocha)                           | Abrir detalle sin consultar          |
| `origin`          | `MADRID (TODAS)`                           | Texto visible                       |
| `destination_code`| `71801` (Barcelona-Sants)                  | Idem                                |
| `destination`     | `BARCELONA-SANTS`                          | Texto visible                       |
| `travel_date`     | ISO `2026-09-20`                           | Contexto                            |
| `departure`       | `08:00:00` (solo si hay tren)              | Texto visible                       |
| `arrival`         | `09:30:00`                                 | Texto visible                       |
| `price`           | `42.50`                                    | Texto visible                       |
| `observed_at`     | ISO 8601 con offset                        | Ordenar/descartar avisos viejos     |
| `expires_at`      | ISO 8601 con offset                        | Caducidad breve (servidor y app)    |
| `channel_id`      | `disponibilidad_plazas_v2`                 | Canal Android                       |
| `priority`        | `high`                                     | Decoración (el high se controla en ``android``) |

Todas las claves son válidas para `data` de FCM (≤150 caracteres, `[A-Za-z0-9_-]+`,
valores ≤1024 y total ≤4096 bytes; `FcmMessage.validate()` lo garantiza antes de
enviar).

## 3. Prioridad y TTL

- `android.priority = "high"` **únicamente** para contenido urgente y visible:
  alertas de plazas y notificación de prueba (ésta es visible por definición,
  la pide el usuario). Los resúmenes/servicio usarán `normal` y canal
  `resumen_y_servicio`.
- TTL corto por defecto (`RENFE_NOTIFIER_FCM_DEFAULT_TTL_S`, 300 s): **declaración**
  ante la cola, no garantía — entregas fuera de plazo que lleguen serán
  descartadas por `expires_at`.
- `collapse_key` = `followup:{followup_id}` en alertas: sustituye mensajes
  antiguos del mismo seguimiento ya no-mostrados.

## 4. Reintentos y deduplicación

- Transporte **idempotente por mensaje y sin reintentos internos**: los intentos
  los acota la cola existente (`AlertQueue`, reintentos limitados) y la deduplicación
  la garantiza `event_id` en servidor y cliente.
- HTTP v1 no admite claves de idempotencia para mensajes individuales; se
  asume "al menos una vez" con intentos acotados.

## 5. Separación "aceptado por FCM" vs "realmente mostrado"

| Resultado                 | Interpretación                                   |
|---------------------------|--------------------------------------------------|
| 200 + `name`              | Aceptado. `FcmResult(accepted=True, message_id=...)` — **no** implica mostrado |
| 404 / `UNREGISTERED`/ "registration token" | Token inválido: `FcmResult(token_invalid=True)` → invalida `fcm_token` vía `PairingService.invalidate_fcm_token` y continúa con el siguiente dispositivo |
| 401/403 / 400 genérico    | `FcmRejectedError(retryable=False)` → error de configuración (502 en test) |
| 429 / 5xx                 | `FcmRejectedError(retryable=True)` → reintento posterior por la cola    |
| Sin credenciales          | `FcmNotConfiguredError` → 503                    |

El cliente, a su vez, devuelve confirmación de visualización (episodio
`acknowledged`) que la cola usa como prueba de entrega real; el servidor nunca
supone "visto" por el hecho de que FCM haya aceptado.

## 6. Estado y primitivas nuevas

- `FcmMessage`, `AlertMessage` (payload tipado), `build_test_alert`,
  `StaticTokenProvider` (pruebas), `GceMetadataTokenProvider`,
  `AutoTokenProvider` y `FcmNotificationSender` en
  `app/notifications/fcm.py`.
- Inyección: `FcmNotificationSender` se construye en `main.py` solo si
  `RENFE_NOTIFIER_FCM_PROJECT_ID` está definido y se expone como
  `app.state.fcm_sender` / `app.state.test_sender` (protocolo
  `NotificationSender`).
- Invalidación de tokens: `PairingRepository.clear_fcm_token`,
  `get_device_by_fcm_token` y `PairingService.clear_fcm_token` /
  `invalidate_fcm_token` (sin revocar el emparejamiento).

## 7. Configuración pendiente (no ejecutada; se documenta)

- Crear/aplicar **Firebase Spark sin facturación** y el proyecto id asociado.
- Dar a la **VM** la identidad con permisos mínimos `firebase.messaging`
  (o contar de servicio local para desarrollo).
- Instalar `google-auth` en local si se usa cuenta de servicio (opcional en la
  VM con metadatos).
- Renovar/cachear el token OAuth2 (vida media ~1 h) está cubierto por
  `credential.refresh()` de google-auth; el provider de metadatos emite tokens
  frescos en cada petición.