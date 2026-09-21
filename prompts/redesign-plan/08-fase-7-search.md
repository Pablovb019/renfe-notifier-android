Ejecuta SOLO la fase 7. Lee AGENTS.md, DESIGN.md, PROGRESS.md.

OBJETIVO
RediseÃ±ar Search completa: campos, resultados y selector de modos.

ARCHIVOS A MODIFICAR
- ${ROOT}/android/app/src/main/java/com/pablovb019/renfenotifier/feature/search/SearchScreen.kt
- ${ROOT}/android/app/src/main/res/values/strings.xml
- ${ROOT}/DESIGN.md, ${ROOT}/PROGRESS.md
ARCHIVOS A CREAR
- ${ROOT}/android/app/src/main/java/com/pablovb019/renfenotifier/feature/search/SearchComponents.kt
- ${ROOT}/android/app/src/debug/java/com/pablovb019/renfenotifier/feature/search/SearchPreviews.kt

PASOS

1. Campos: singleLine, 56 dp min. Sugerencias EN FLUJO. Contenedor heightIn(max=192.dp). NO anidar verticalScroll dentro de LazyColumn. Ãrea â‰¥48 dp por sugerencia.
2. Fecha: DatePicker existente. No cambiar validaciÃ³n.
3. Plaza H: switch o checkbox M3.
4. CTA Buscar: enabled = !isSearching, heightIn(min=56.dp), fullWidth.
5. Modos: FIRST/LAST/ALL/SPECIFIC en FlowRow o lista vertical. NO borres ningÃºn modo.
6. Tarjeta por tren: hora salida/llegada, tipo, disponibilidad badge. Precio null â†’ "Precio no disponible", NUNCA 0 â‚¬. train.identity como key. Un click â†’ onTrainSelected.
7. Carga: RenfeLoadingState. Error: RenfeErrorState con onSearch.
8. Crear seguimiento: enabled = !isCreatingFollowUp. DiÃ¡logos conservados.
9. MÃ¡rgenes 16 dp. Estaciones 2 lÃ­neas Ellipsis.
10. Preview 360Ã—800, fontScale 1.0/2.0.
11. Comandos:
    - .\gradlew.bat :app:assembleDebug --no-daemon
    - .\gradlew.bat :app:testDebugUnitTest :app:lintDebug --no-daemon

ACEPTACIÃ“N
- Sin popups.
- 4 modos accesibles en 360 dp a 2Ã—.
- null no es 0.

CIERRE
Actualiza DESIGN.md y PROGRESS.md. DETENTE.
