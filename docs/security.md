# Modelo de Seguridad y Conexión: renfe-notifier-android

---

## 1. Principios de Seguridad y Modelo de Amenazas

1. **Uso Exclusivamente Personal (Single User)**:
   - La aplicación está diseñada para **un único usuario legítimo**.
   - No existe formulario de registro público, autoregistro ni endpoints expuestos para creación de cuentas.
   - Cualquier petición no autenticada es rechazada con `HTTP 401 Unauthorized`.
2. **Cero Secretos Compartidos en el Cliente**:
   - No se incrustan claves maestras, contraseñas de base de datos ni credenciales de servicio de Google Cloud o Firebase en el APK compilado.
   - El archivo `google-services.json` incluido en el APK solo contiene identificadores públicos del cliente de Firebase (App ID, Project ID), mientras que la clave privada de la cuenta de servicio (`service-account.json`) reside **estrictamente en el backend**.
3. **Cero Modificación o Ejecución Arbitraria (Anti-SSRF)**:
   - La API del backend no permite que el cliente pase URLs arbitrarias para consultar. Las búsquedas solo aceptan códigos de estaciones validados contra el catálogo local `stations.json`.
4. **Cifrado Obligatorio en Tránsito**:
   - Prohibido el uso de HTTP plano o desactivar la comprobación de certificados TLS en Android (`android:usesCleartextTraffic="false"`).

---

## 2. Flujo de Emparejamiento Criptográfico (Device Pairing)

Para autenticar de forma robusta la app sin requerir un sistema complejo de usuarios/contraseñas ni OAuth de terceros:

```
 Administrador (VM)                    App Android (Móvil)                     Backend (FastAPI)
         │                                      │                                      │
         │  1. Ejecuta CLI de setup             │                                      │
         │  python -m app.cli create-pairing    │                                      │
         ├────────────────────────────────────────────────────────────────────────────►│
         │                                      │                                      │ Genera código OTP
         │  2. Obtiene código temporal          │                                      │ (ej: "RF-839201", 10 min)
         │◄────────────────────────────────────────────────────────────────────────────┤
         │                                      │                                      │
         │  3. Introduce código en la App       │                                      │
         │─────────────────────────────────────►│                                      │
         │                                      │  4. POST /api/v1/pairing/claim       │
         │                                      │     {code, device_id, device_name}   │
         │                                      ├─────────────────────────────────────►│
         │                                      │                                      │ Valida OTP, genera
         │                                      │                                      │ token criptográfico
         │                                      │  5. 200 OK                           │ único (64 bytes hex)
         │                                      │     {device_token, expires_in}       │
         │                                      │◄─────────────────────────────────────┤
         │                                      │                                      │
         │                                      │  6. Guarda en EncryptedSharedPreferences
         │                                      │     (Android Keystore)               │
         │                                      │                                      │
         │                                      │  7. Peticiones API normales          │
         │                                      │     Authorization: Bearer <token>    │
         │                                      ├─────────────────────────────────────►│ Valida token
         │                                      │                                      │ contra DB
```

### 2.1. Detalles Técnicos del Token de Dispositivo
- El `device_token` se genera mediante `secrets.token_urlsafe(48)`.
- En la base de datos SQLite se almacena exclusivamente el hash del token (`hashlib.sha256(token.encode()).hexdigest()`), evitando que el acceso al archivo `.db` comprometa las credenciales en texto plano.
- Todas las rutas protegidas (`/api/v1/search/*`, `/api/v1/followups/*`, `/api/v1/fcm/token`) validan el token mediante el encabezado HTTP:
  ```http
  Authorization: Bearer <device_token>
  ```

---

## 3. Acceso Administrativo Separado

- **Separación de Responsabilidades**: La API web **no expone endpoints administrativos** (como autorizar nuevos usuarios, reiniciar servicios, consultar secretos o vaciar la base de datos).
- **Mecanismo de Administración**:
  - Toda tarea administrativa se efectúa localmente en la máquina virtual mediante una interfaz de línea de comandos (CLI):
    ```bash
    # Generar código de emparejamiento para un nuevo teléfono
    python -m app.cli pairing-code --generate

    # Listar dispositivos activos
    python -m app.cli devices --list

    # Revocar de inmediato el acceso a un dispositivo
    python -m app.cli devices --revoke <device_id>

    # Limpieza o mantenimiento de base de datos
    python -m app.cli db --vacuum
    ```
  - El acceso a la VM se realiza exclusivamente mediante SSH seguro a través de la consola de Google Cloud (`gcloud compute ssh`) o claves SSH protegidas.

---

## 4. Conexión de Red Cifrada Android ↔ Backend a 0 €

Se contemplan dos vías de acceso permanente sin coste de dominio ni IP estática:

### 4.1. Opción 1 (Recomendada): Tailscale (Mesh VPN WireGuard)
- **Aislamiento absoluto**: El backend FastAPI escucha únicamente en la interfaz `100.x.y.z:8000`. No se abren puertos en el firewall de Google Cloud.
- **Cifrado P2P**: Comunicación punto a punto con cifrado WireGuard de alta velocidad sin consumo adicional de CPU.
- **Certificado TLS**: Mediante Tailscale HTTPS / MagicDNS, se obtiene un nombre FQDN con certificado TLS reconocido automáticamente por el sistema Android (ej. `https://renfe-vm.tailnet-xyz.ts.net`).
- **Resistencia**: Inmune a cambios de IP externa efímera en la VM de Google Cloud.

### 4.2. Opción 2 (Alternativa sin VPN móvil): DuckDNS + Let's Encrypt
- **Subdominio gratuito**: Se utiliza `duckdns.org`, actualizando la IP efímera de la VM mediante un script periódico en `cron`.
- **Cifrado**: Certbot / Let's Encrypt emite certificados TLS reconocidos por Android.
- **Firewall**: En Google Cloud VPC sólo se autoriza tráfico entrante en el puerto TCP 443.
- **Rate Limiting**: El reverse proxy (Caddy o Nginx) limita el número de peticiones por segundo para prevenir ataques de denegación de servicio.

---

## 5. Recuperación de Acceso y Gestión de Pérdida de Dispositivo

- **Pérdida o sustitución de teléfono móvil**:
  1. El usuario accede a la VM vía SSH de Google Cloud.
  2. Ejecuta el comando de revocación:
     ```bash
     python -m app.cli devices --revoke-all
     ```
  3. Esto invalida inmediatamente el hash del token en la base de datos. Cualquier intento de conexión desde el dispositivo antiguo devolverá `401 Unauthorized`.
  4. El usuario instala el APK en su nuevo terminal realme.
  5. Genera un nuevo código de emparejamiento con `python -m app.cli pairing-code --generate` y lo introduce en la app para vincular el nuevo dispositivo.

---

## 6. Sanitización de Registros y Prevención de Fugas de Información

1. **Filtro de Logs en Backend**:
   - Los middleware de FastAPI interceptan y enmascaran cabeceras sensibles:
     - `Authorization: Bearer ********`
     - Códigos de emparejamiento OTP.
     - Tokens FCM de registro.
     - Cookies de sesión DWR (`DWRSESSIONID`).
2. **Logs en Android**:
   - Uso de un logger condicional (`Timber` o `Logcat` envuelto): los mensajes detallados de depuración de red solo se emiten en compilaciones `debug`, quedando completamente inhabilitados en compilaciones `release`.
