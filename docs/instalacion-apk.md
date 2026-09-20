# Instalación y Actualización del APK en el realme GT Neo 2

> Alcance: cómo instalar, actualizar y verificar el APK firmado `app-release.apk` en el dispositivo realme sin Google Play. Convierte la instalación manual; no se publica la app en ninguna tienda.

## 1. Releases disponibles

Todas las releases `v0.1.0`ââ‚¬Â¦`v0.1.10` son **privadas** (`docs/security.md`) y llevan dos assets:

- `app-release.apk` ââ‚¬â€ APK firmado.
- `app-release.apk.sha256` ââ‚¬â€ checksum SHA-256 de ese APK.

ÃƒÅ¡ltima release verificada (2026-09-20):

| Campo | Valor |
|-------|-------|
| Versión | `v0.1.10` (versionCode 11) |
| Asset | `app-release.apk` (8.995.411 B) |
| SHA-256 | `37a59734efc5567f3362b671d87aebf037c2374c4a68fa4649bbcb718bdf4c76` |
| URL | `https://github.com/Pablovb019/renfe-notifier-android/releases/download/v0.1.10/app-release.apk` |
| Firma | keystore `renfe-notifier-android` (ver `docs/keystore.md`) |

> El checksum se comprueba siempre contra el asset real del release (no contra este documento) si hay duda.

## 2. Instalación inicial (primera vez)

1. Descarga `app-release.apk` y `app-release.apk.sha256` del release.
2. Verifica el checksum (PowerShell):
   ```powershell
   Get-FileHash .\app-release.apk -Algorithm SHA256
   # comparar con el valor del .sha256
   ```
3. Copia el APK al realme (USB o descarga directa desde el teléfono).
4. En el realme: **Ajustes ââ€ â€™ Seguridad/Privacidad ââ€ â€™ más ajustes ââ€ â€™ Permisos de instalación de apps desconocidas** ââ€ â€™ habilitar para el gestor de archivos o navegador usado.
5. Abre el APK con el gestor de archivos (`es.apk...`/`com.android.documentsui`) y confirma la instalación.
6. Comprueba la firma tras instalar (opcional): `apksigner verify --print-certs app-release.apk` (ver `docs/keystore.md`).

## 3. Actualización (mantenimiento)

- Los APK con mayor `versionCode` **reemplazan** a la versión instalada conservando datos (misma `applicationId`, misma clave de firma):
  ```powershell
  adb install -r app-release.apk          # reinstala conservando datos
  # o, desde el teléfono, tocar el nuevo APK y confirmar "Actualizar"
  ```
- `adb install -r` es el método recomendado para desarrollo; en producción del realme sirve tocar el APK desde el gestor de archivos.
- **Regla**: nunca instalar un APK con firma distinta sobre la versión actual (fallará) ni un APK sin firmar (ver `docs/keystore.md`, `docs/ci-cd.md`).

## 4. Rollback a una release anterior

1. Descarga el APK del release anterior (p. ej. `v0.1.9`).
2. Verifica su checksum.
3. Instala con `adb install -r <apk>` o `adb install -d -r` si el `versionCode` es menor (downgrade).
4. La BD de SQLite vive en el *backend* (VM), no en el teléfono: rollback de app **no** toca seguimientos (`docs/recovery.md`, `docs/architecture.md`).

## 5. Verificación post-instalación (guía realme)

Ver `docs/diagnostico-realme.md` para el flujo de diagnóstico (permisos de notificación, FCM de prueba, optimización de batería realme).
