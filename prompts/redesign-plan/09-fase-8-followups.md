Ejecuta SOLO la fase 8. Lee AGENTS.md, DESIGN.md, PROGRESS.md.

OBJETIVO
Lista de seguimientos y 5 filtros accesibles en 360 dp.

ARCHIVOS A MODIFICAR
- ${ROOT}/android/app/src/main/java/com/pablovb019/renfenotifier/feature/followups/FollowUpsScreen.kt
- ${ROOT}/android/app/src/main/res/values/strings.xml
- ${ROOT}/DESIGN.md, ${ROOT}/PROGRESS.md
ARCHIVOS A CREAR
- ${ROOT}/android/app/src/main/java/com/pablovb019/renfenotifier/feature/followups/FollowUpComponents.kt
- ${ROOT}/android/app/src/debug/java/com/pablovb019/renfenotifier/feature/followups/FollowUpsPreviews.kt
- ${ROOT}/android/app/src/androidTest/java/com/pablovb019/renfenotifier/feature/followups/FollowUpsContentTest.kt

PASOS

1. Filtros: LazyRow horizontal. NO Row estÃ¡tica. Ãšltimo filtro accesible. Fuente 2Ã— verificada.
2. Tarjeta: origenâ†’destino jerarquÃ­a. Fallback cÃ³digo. Fecha MadridFormat. Ciclo de vida badge (ACTIVEâ†’SUCCESS, PAUSEDâ†’WARNING, EXPIREDâ†’ERROR, DELETEDâ†’NEUTRAL). Un click â†’ onOpenDetail(id). key estable.
3. Estados: Loading/Empty/Error con onRefresh.
4. NO modifiques LaunchedEffect de recarga.
5. MÃ¡rgenes 16 dp. Ruta larga 2 lÃ­neas Ellipsis.
6. Preview 360Ã—800, fontScale 1.0/2.0.
7. Comandos:
   - .\gradlew.bat :app:assembleDebug --no-daemon
   - .\gradlew.bat :app:testDebugUnitTest :app:lintDebug :app:assembleDebugAndroidTest --no-daemon

ACEPTACIÃ“N
- 5 filtros accesibles a 2Ã—.

CIERRE
Actualiza DESIGN.md y PROGRESS.md. DETENTE.
