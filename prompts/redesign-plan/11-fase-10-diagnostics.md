Ejecuta SOLO la fase 10. Lee AGENTS.md, DESIGN.md, PROGRESS.md.

OBJETIVO
TÃ­tulo "Ajustes". Apariencia â†’ Avisos â†’ DiagnÃ³stico tÃ©cnico. Sin nueva ruta.

ARCHIVOS A MODIFICAR
- ${ROOT}/android/app/src/main/java/com/pablovb019/renfenotifier/feature/diagnostics/DiagnosticsScreen.kt
- ${ROOT}/android/app/src/main/res/values/strings.xml
- ${ROOT}/DESIGN.md, ${ROOT}/PROGRESS.md
ARCHIVOS A CREAR
- ${ROOT}/android/app/src/main/java/com/pablovb019/renfenotifier/feature/diagnostics/DiagnosticsComponents.kt
- ${ROOT}/android/app/src/debug/java/com/pablovb019/renfenotifier/feature/diagnostics/DiagnosticsPreviews.kt
- ${ROOT}/android/app/src/androidTest/java/com/pablovb019/renfenotifier/feature/diagnostics/DiagnosticsContentTest.kt

PASOS

1. TÃ­tulo "Ajustes". Ruta sigue diagnostics.
2. Secciones: Apariencia (ThemeModeSelector de Fase 3), Avisos (switch), DiagnÃ³stico tÃ©cnico (5 etapas, plegable con rememberSaveable).
3. BotÃ³n prueba: tÃ­tulo corto. Estado sending deshabilita. Resultado en texto adjunto. NO tokens/OTP/credenciales.
4. MÃ¡rgenes 16 dp.
5. Preview 360Ã—800, fontScale 1.0/2.0.
6. NO tocar load, refreshEnvironment, sendTestNotification, computeStages del VM.
7. Comandos:
   - .\gradlew.bat :app:assembleDebug --no-daemon
   - .\gradlew.bat :app:testDebugUnitTest :app:lintDebug :app:assembleDebugAndroidTest --no-daemon

ACEPTACIÃ“N
- TÃ­tulo "Ajustes" visible.
- Info previa accesible.

CIERRE
Actualiza DESIGN.md y PROGRESS.md. DETENTE.
