# Compilación local del APK en Windows

Guía verificada el 2026-09-13 en Windows (PowerShell): compila el APK debug
al completo, sin Android Studio y a 0 €.

## 1. Requisitos instalados en este equipo

| Herramienta     | Versión        | Instalación                                           |
|-----------------|----------------|-------------------------------------------------------|
| JDK             | 17 (Temurin)   | `%LOCALAPPDATA%\jdk-17` (zip oficial de Adoptium)     |
| Gradle          | 8.9            | `%LOCALAPPDATA%\gradle-8.9` (distribución bin oficial)|
| Android SDK     | cmdline-tools 12.0 | `%LOCALAPPDATA%\Android\Sdk` (zip oficial de dl.google.com) |
| Platform        | android-34     | `sdkmanager`                                           |
| Build tools     | 34.0.0         | `sdkmanager`                                           |

El repositorio **sí versiona** el wrapper de Gradle (`gradlew.bat`,
`gradlew`, `gradle/wrapper/`) para que cualquiera con JDK 17 y el SDK
pueda compilar sin instalar Gradle globalmente. `local.properties`
(ruta del SDK) es por máquina y **no** se sube a Git.

## 2. Provisionar el toolchain desde cero (solo la primera vez)

Lectura rápida y abstenerse de 0 €: todo se descarga de fuentes oficiales.

```powershell
# 1. JDK 17 (Temurin, perfil de usuario, sin admin)
$jdk = "$env:LOCALAPPDATA\jdk-17"
New-Item -ItemType Directory -Force $jdk | Out-Null
Invoke-WebRequest "https://api.adoptium.net/v3/binary/latest/17/ga/windows/x64/jdk/hotspot/normal/eclipse" -OutFile "$env:TEMP\jdk17.zip"
Expand-Archive "$env:TEMP\jdk17.zip" $jdk -Force

# 2. Gradle 8.9 (solo para generar el wrapper; el proyecto usa gradlew)
Invoke-WebRequest "https://services.gradle.org/distributions/gradle-8.9-bin.zip" -OutFile "$env:TEMP\gradle.zip"
Expand-Archive "$env:TEMP\gradle.zip" "$env:LOCALAPPDATA\gradle-8.9" -Force

# 3. Android cmdline-tools (debe ubicarse en <sdk>\cmdline-tools\latest)
$sdk = "$env:LOCALAPPDATA\Android\Sdk"
New-Item -ItemType Directory -Force "$sdk\cmdline-tools" | Out-Null
Invoke-WebRequest "https://dl.google.com/android/repository/commandlinetools-win-11076708_latest.zip" -OutFile "$env:TEMP\cmdline.zip"
Expand-Archive "$env:TEMP\cmdline.zip" "$sdk\cmdline-tools" -Force
Move-Item "$sdk\cmdline-tools\cmdline-tools" "$sdk\cmdline-tools\latest"

# 4. Licencias + plataforma y build-tools
$env:JAVA_HOME = "$jdk\jdk-17.0.20.1+1"; $env:ANDROID_HOME = $sdk
(1..40 | % { "y" }) | & "$sdk\cmdline-tools\latest\bin\sdkmanager.bat" --sdk_root=$sdk --licenses
& "$sdk\cmdline-tools\latest\bin\sdkmanager.bat" --sdk_root=$sdk "platform-tools" "platforms;android-34" "build-tools;34.0.0"

# 5. Punto de mira del proyecto al SDK local (gitignored)
Set-Content -Path android\local.properties -Value "sdk.dir=$($sdk.Replace('\','\\').Replace(':','\:'))"
```

> La primera ejecución de `gradlew` descarga AGP 8.5.2, Kotlin 2.0.21 y el
> resto de dependencias; tarda unos minutos.

## 3. Compilar el APK debug

```powershell
$env:JAVA_HOME = "$env:LOCALAPPDATA\jdk-17\jdk-17.0.20.1+1"
$env:ANDROID_HOME = "$env:LOCALAPPDATA\Android\Sdk"
cd android
.\gradlew.bat :app:assembleDebug
```

Resultado esperado:

```
BUILD SUCCESSFUL
android\app\build\outputs\apk\debug\app-debug.apk
```

El `output-metadata.json` confirma el artefacto: `applicationId
com.pablovb019.renfenotifier`, `versionCode 1`, `versionName 0.1.0`,
`minSdk 26` (dex), variante `debug`.

## 4. Notas

- **JDK 17** es obligatorio para AGP 8.5.x; el wrapper usa la ruta de
  `JAVA_HOME` configurada (no se instala nada del sistema).
- **Versiones** (compatibles entre sí y con la CI prevista en
  `docs/ci-cd.md`): Gradle 8.9, AGP 8.5.2, Kotlin 2.0.21, Compose BOM
  2024.12.01, compileSdk 34, targetSdk 34, minSdk 26.
- **Windows**: `gradlew.bat` (en macOS/Linux usar `./gradlew`). El daemon se
  desactiva con `--no-daemon` cuando se compila en un solo comando CI-like.
- La depuración del paso a paso del toolchain quedó en
  `prompts/PROGRESS.md` (paso 20).