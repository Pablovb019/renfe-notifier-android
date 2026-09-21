Ejecuta SOLO la fase 9. Lee AGENTS.md, DESIGN.md, PROGRESS.md.

OBJETIVO
Detalle preservando 5 acciones separadas.

ARCHIVOS A MODIFICAR
- ${ROOT}/android/app/src/main/java/com/pablovb019/renfenotifier/feature/followups/FollowUpDetailScreen.kt
- ${ROOT}/android/app/src/main/java/com/pablovb019/renfenotifier/feature/followups/FollowUpComponents.kt
- ${ROOT}/android/app/src/main/res/values/strings.xml
- ${ROOT}/DESIGN.md, ${ROOT}/PROGRESS.md
ARCHIVOS A CREAR
- ${ROOT}/android/app/src/debug/java/com/pablovb019/renfenotifier/feature/followups/FollowUpDetailPreviews.kt
- ${ROOT}/android/app/src/androidTest/java/com/pablovb019/renfenotifier/feature/followups/FollowUpDetailContentTest.kt

PASOS

1. Cabecera: ruta, fecha, modo badge, ciclo de vida badge.
2. "Ãšltima comprobaciÃ³n": lastValidObservedAt (NO hora actual). Caducidad Europe/Madrid. Plaza H. NÂº episodios.
3. Acciones separadas: Pausar, Reanudar, Renovar, Confirmar aviso, Eliminar. Confirmar aviso NO elimina. Eliminar â†’ AlertDialog. Cancelar NO llama onDelete. NO swipe-to-delete.
4. actionInProgress: botones deshabilitados. Conserva LaunchedEffect(deleted).
5. MÃ¡rgenes 16 dp.
6. Preview 360Ã—800, fontScale 1.0/2.0.
7. Comandos:
   - .\gradlew.bat :app:assembleDebug --no-daemon
   - .\gradlew.bat :app:testDebugUnitTest :app:lintDebug :app:assembleDebugAndroidTest --no-daemon

ACEPTACIÃ“N
- 5 acciones separadas. Cancelar borrado no elimina.

CIERRE
Actualiza DESIGN.md y PROGRESS.md. DETENTE.
