# Generación y Backup Seguro de Clave de Firma (Keystore)

> **Regla de oro**: La clave de firma **NUNCA** se sube a Git, ni a CI, ni a artefactos. Solo existe en tu máquina local y (opcional) en GitHub Secrets codificada en Base64 para releases automáticas.

## 1. Generar keystore local (ejecutar UNA vez)

```powershell
# En PowerShell (Windows)
keytool -genkey -v -keystore keystore.jks -keyalg RSA -keysize 2048 -validity 10000 -alias renfe-notifier
```

Te pedirá:
- **Contraseña del keystore** (storepass) → anótala: `KEYSTORE_PASSWORD`
- **Contraseña de la clave** (keypass) → puede ser igual o distinta: `KEY_PASSWORD`
- **Alias**: `renfe-notifier` (anótalo: `KEY_ALIAS`)
- Datos del certificado (CN, OU, O, L, ST, C) → rellena o deja por defecto

Resultado: archivo `keystore.jks` en el directorio actual.

## 2. Verificar keystore

```powershell
keytool -list -v -keystore keystore.jks -alias renfe-notifier
```

Debe mostrar:
- Alias: `renfe-notifier`
- Tipo: `PrivateKeyEntry`
- Algoritmo: `RSA` 2048 bits
- Validez: ~27 años (10000 días)
- Huella SHA-256 (anótala para auditoría)

## 3. Codificar en Base64 para GitHub Secrets

```powershell
# PowerShell
[Convert]::ToBase64String([IO.File]::ReadAllBytes("keystore.jks")) | Set-Clipboard
# O guardar en archivo (¡borrar después!):
[Convert]::ToBase64String([IO.File]::ReadAllBytes("keystore.jks")) > keystore.b64.txt
```

## 4. Configurar Secrets en GitHub

En `Settings → Secrets and variables → Actions → New repository secret`:

| Secret | Valor |
|--------|-------|
| `ANDROID_KEYSTORE_BASE64` | Contenido Base64 del paso 3 |
| `KEYSTORE_PASSWORD` | Contraseña del keystore (storepass) |
| `KEY_ALIAS` | `renfe-notifier` |
| `KEY_PASSWORD` | Contraseña de la clave (keypass) |

## 5. Backup seguro de la clave (fuera de Git y CI)

**Obligatorio**: Guardar `keystore.jks` + las 3 contraseñas en **mínimo 2 ubicaciones físicas separadas**:

- USB encriptado (BitLocker / VeraCrypt) guardado en lugar seguro
- Gestor de contraseñas (Bitwarden, 1Password, KeePass) con adjunto
- Papel en caja fuerte (contraseñas) + USB en sitio distinto

**NUNCA**:
- Subir `keystore.jks` a Git, Drive, email, chat
- Incluirlo en backups de CI/artefactos
- Compartir contraseñas por canales inseguros

## 6. Actualizaciones futuras (MISMA clave = actualizaciones incrementales)

Mientras uses el **mismo keystore + alias**, cada nuevo APK firmado se instala sobre el anterior **sin pérdida de datos** del usuario (SharedPreferences, DataStore, BD SQLite local).

Si pierdes el keystore → **no podrás actualizar la app** (Play Store / instalación manual rechazará firma distinta). Usuario tendría que desinstalar y reinstalar → pierde datos.

## 7. Flujo de Release (manual, tú decides cuándo)

```bash
# Opción A: workflow_dispatch (manual desde Actions UI)
# 1. GitHub → Actions → "android-release" → Run workflow
# 2. version_name: "1.0.0", version_code: "2", confirm: "RELEASE"
# 3. Se crea Release privada + APK + SHA256

# Opción B: Tag versionado (push tag → release automática)
git tag -a v1.0.0 -m "Release 1.0.0"
git push origin v1.0.0
# → Dispara workflow, valida CI, crea Release privada
```

## 8. Verificación manual del APK firmado

```powershell
# Descargar APK de la Release privada → verificar localmente
apksigner verify --print-certs app-1.0.0.apk
sha256sum app-1.0.0.apk
# Comparar con app-1.0.0.apk.sha256 de la Release
```

## 9. Rotación de clave (solo si compromiso)

Si la clave se compromete:
1. Generar nuevo keystore (paso 1)
2. Subir nueva app a Play Store como **nueva aplicación** (package name distinto) O
3. Contactar soporte Google Play para reset de clave de subida (proceso largo, requiere prueba de propiedad)

**Prevención**: Backup múltiple (paso 5) evita este escenario.