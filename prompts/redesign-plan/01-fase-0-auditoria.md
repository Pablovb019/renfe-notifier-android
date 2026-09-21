Ejecuta SOLO la fase 0. Lee AGENTS.md. No toques cÃ³digo.

OBJETIVO
Auditar el repositorio completo y el dispositivo Realme. Crear DESIGN.md con toda la informaciÃ³n base.

LECTURAS
- ${ROOT}/AGENTS.md
- ${ROOT}/PROGRESS.md (si existe)
- ${ROOT}/DESIGN.md (si existe de un intento previo)
- ${ROOT}/android/build.gradle.kts
- ${ROOT}/android/app/build.gradle.kts
- ${ROOT}/android/gradle/libs.versions.toml
- ${ROOT}/android/settings.gradle.kts
- ${ROOT}/android/app/src/main/AndroidManifest.xml
- ${ROOT}/android/app/src/main/java/com/pablovb019/renfenotifier/MainActivity.kt
- ${ROOT}/android/app/src/main/java/com/pablovb019/renfenotifier/navigation/AppNavHost.kt
- ${ROOT}/android/app/src/main/java/com/pablovb019/renfenotifier/ui/theme/Theme.kt
- ${ROOT}/android/app/src/main/java/com/pablovb019/renfenotifier/ui/theme/Type.kt
- ${ROOT}/android/app/src/main/java/com/pablovb019/renfenotifier/core/security/PreferencesRepository.kt
- ${ROOT}/android/app/src/main/java/com/pablovb019/renfenotifier/feature/home/HomeScreen.kt
- ${ROOT}/android/app/src/main/java/com/pablovb019/renfenotifier/feature/home/HomeUiState.kt
- ${ROOT}/android/app/src/main/java/com/pablovb019/renfenotifier/feature/search/SearchScreen.kt
- ${ROOT}/android/app/src/main/java/com/pablovb019/renfenotifier/feature/followups/FollowUpsScreen.kt
- ${ROOT}/android/app/src/main/java/com/pablovb019/renfenotifier/feature/followups/FollowUpDetailScreen.kt
- ${ROOT}/android/app/src/main/java/com/pablovb019/renfenotifier/feature/diagnostics/DiagnosticsScreen.kt
- ${ROOT}/android/app/src/main/java/com/pablovb019/renfenotifier/feature/pairing/PairingScreen.kt
- ${ROOT}/android/app/src/main/res/values/strings.xml
- ${ROOT}/.github/workflows/android-ci.yml
ROOT="$(git rev-parse --show-toplevel)". Windows: .\gradlew.bat.

PASOS

1. Entorno: git status, git branch, git rev-parse HEAD. Si no estÃ¡s en el repo, DETENTE.
2. Versiones: JDK, Gradle, AGP, Kotlin, BOM, M3, Navigation, DataStore, minSdk, targetSdk.
3. Baseline compilaciÃ³n:
   .\gradlew.bat :app:assembleDebug --no-daemon (Ãºltimas 15 lÃ­neas)
4. Baseline tests/lint:
   .\gradlew.bat :app:testDebugUnitTest --no-daemon (resumen: total/pasados/fallados)
   .\gradlew.bat :app:lintDebug --no-daemon (resumen: errores/warnings)
5. AuditorÃ­a theme: Theme.kt (paleta actual, dynamicColor), Type.kt (escala), MainActivity (observaciÃ³n de tema).
6. AuditorÃ­a persistencia: PreferencesRepository (clave theme, valores, archivo DataStore).
7. AuditorÃ­a pantallas (tabla compacta): por cada una, ruta, estado UI, callbacks, efectos, enabled clave.
8. Riesgos con file:lÃ­nea: colores hardcodeados, Row con >3 elementos, Text sin maxLines, botones con callback vacÃ­o, scrolls anidados.
9. ADB:
   adb devices -l (si hay varios, pide serial)
   adb shell wm size
   adb shell wm density
   adb shell getprop ro.product.model
   adb shell getprop ro.build.version.release
   adb shell settings get system font_scale
   adb shell cmd uimode night
10. Calcula: ancho_dp = ancho_px / (densidad/160), alto_dp = alto_px / (densidad/160).
11. Insets: adb shell dumpsys window displays (solo lÃ­neas ITYPE_STATUS_BAR, ITYPE_NAVIGATION_BAR, ITYPE_IME; mÃ¡x 15 lÃ­neas).
12. Crea/actualiza DESIGN.md con: BASELINE_SHA, stack, DEVICE_PROFILE, baseline tests/lint, contratos UI, perÃ­metro protegido, riesgos, bloqueos.
13. AÃ±ade entrada a PROGRESS.md.

CRITERIOS DE ACEPTACIÃ“N
- Versiones verificadas, sin arquitectura inventada.
- Densidad medida registrada o PENDIENTE.
- Baseline numÃ©rico registrado.
- Riesgos de 360 dp documentados.
- NingÃºn archivo funcional modificado.

CONTINGENCIA
Sin ADB: marca PENDIENTE, pide salida literal al usuario. No fuerces wm density en el telÃ©fono.

ENTREGA
Resumen de 20 lÃ­neas mÃ¡x. DETENTE.
