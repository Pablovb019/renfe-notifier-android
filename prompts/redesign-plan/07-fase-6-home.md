Ejecuta SOLO la fase 6. Lee AGENTS.md, DESIGN.md, PROGRESS.md.

OBJETIVO
Home ferroviaria minimalista. Eliminar botÃ³n "Recargar" no-op.

ARCHIVOS A MODIFICAR
- ${ROOT}/android/app/src/main/java/com/pablovb019/renfenotifier/feature/home/HomeScreen.kt
- ${ROOT}/android/app/src/main/res/values/strings.xml
- ${ROOT}/DESIGN.md, ${ROOT}/PROGRESS.md
ARCHIVOS A CREAR
- ${ROOT}/android/app/src/main/java/com/pablovb019/renfenotifier/feature/home/HomeComponents.kt
- ${ROOT}/android/app/src/debug/java/com/pablovb019/renfenotifier/feature/home/HomePreviews.kt
- ${ROOT}/android/app/src/androidTest/java/com/pablovb019/renfenotifier/feature/home/HomeContentTest.kt

CONTRATO
HomeUiState: versionName, now, loading, isPaired.
Callbacks: onNavigateToPairing, onNavigateToSearch, onNavigateToFollowUps, onNavigateToDiagnostics.
onRefresh = {} â†’ retirar botÃ³n. Sin red nueva.

PASOS

1. Columna scrollable:
   a) TopAppBar "Renfe Notifier" + "Ajustes" â†’ onNavigateToDiagnostics.
   b) Encabezado 2 lÃ­neas mÃ¡x.
   c) No emparejada: CTA "Vincular dispositivo". Emparejada: "Dispositivo vinculado".
   d) CTA principal "Buscar trenes". Secundaria "Mis seguimientos".
   e) Permisos denegados: aviso + botÃ³n ajustes.
   f) Hora y versiÃ³n pequeÃ±as.
   g) Eliminar "Recargar". Documentar.
2. RenfeScreenScaffold. MÃ¡rgenes RenfeSpacing.screenMargin.
3. Textos con maxLines/overflow. TÃ­tulos 24-28 sp. CTA heightIn(min=56.dp).
4. Preview 360Ã—800 claro/oscuro, fontScale 1.0/2.0.
5. Test: cada acciÃ³n llama una vez su callback. Sin botÃ³n "Recargar".
6. Comandos:
   - .\gradlew.bat :app:assembleDebug --no-daemon
   - .\gradlew.bat :app:testDebugUnitTest :app:lintDebug :app:assembleDebugAndroidTest --no-daemon

ACEPTACIÃ“N
- BotÃ³n no-op eliminado.
- Callbacks operativos.

CIERRE
Actualiza DESIGN.md y PROGRESS.md. DETENTE.
