Ejecuta SOLO la fase 11. Lee AGENTS.md, DESIGN.md, PROGRESS.md.

OBJETIVO
Formulario RF-XXXXXX. Sin mÃ©todos nuevos de auth.

ARCHIVOS A MODIFICAR
- ${ROOT}/android/app/src/main/java/com/pablovb019/renfenotifier/feature/pairing/PairingScreen.kt
- ${ROOT}/android/app/src/main/res/values/strings.xml
- ${ROOT}/DESIGN.md, ${ROOT}/PROGRESS.md
ARCHIVOS A CREAR
- ${ROOT}/android/app/src/debug/java/com/pablovb019/renfenotifier/feature/pairing/PairingPreviews.kt
- ${ROOT}/android/app/src/androidTest/java/com/pablovb019/renfenotifier/feature/pairing/PairingContentTest.kt

PASOS

1. Columna scrollable, mÃ¡rgenes 16 dp, imePadding.
2. Campo RF-XXXXXX. Teclado de texto (NO numÃ©rico). Error asociado.
3. BotÃ³n enabled = !isLoading && code.isNotBlank(). Spinner. heightIn(min=56.dp).
4. onPaired solo tras uiState.paired=true.
5. No registrar cÃ³digo. No alterar Keystore, Android ID, vault.
6. Preview 360Ã—800, fontScale 1.0/2.0.
7. Comandos:
   - .\gradlew.bat :app:assembleDebug --no-daemon
   - .\gradlew.bat :app:testDebugUnitTest :app:lintDebug :app:assembleDebugAndroidTest --no-daemon

ACEPTACIÃ“N
- Sin teclado numÃ©rico. Sin capturas del cÃ³digo.

CIERRE
Actualiza DESIGN.md y PROGRESS.md. DETENTE.
