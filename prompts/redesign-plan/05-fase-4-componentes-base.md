Ejecuta SOLO la fase 4. Lee AGENTS.md, DESIGN.md, PROGRESS.md.

OBJETIVO
Crear todos los componentes UI reutilizables.

ARCHIVOS A CREAR
- ${ROOT}/android/app/src/main/java/com/pablovb019/renfenotifier/ui/components/RenfeScreenScaffold.kt
- ${ROOT}/android/app/src/main/java/com/pablovb019/renfenotifier/ui/components/RenfeLoadingState.kt
- ${ROOT}/android/app/src/main/java/com/pablovb019/renfenotifier/ui/components/RenfeEmptyState.kt
- ${ROOT}/android/app/src/main/java/com/pablovb019/renfenotifier/ui/components/RenfeErrorState.kt
- ${ROOT}/android/app/src/main/java/com/pablovb019/renfenotifier/ui/components/RenfeStatusBadge.kt
- ${ROOT}/android/app/src/debug/java/com/pablovb019/renfenotifier/ui/components/RenfeComponentsPreviews.kt
- ${ROOT}/android/app/src/androidTest/java/com/pablovb019/renfenotifier/ui/components/RenfeComponentsTest.kt
ARCHIVOS A MODIFICAR
- ${ROOT}/android/app/src/main/res/values/strings.xml, ${ROOT}/DESIGN.md, ${ROOT}/PROGRESS.md

PASOS

1. RenfeScreenScaffold: Scaffold M3 + TopAppBar. Params title, onBack, actions, content(PaddingValues). Consume innerPadding UNA vez. TÃ­tulo 24 sp, maxLines=1 Ellipsis.
2. RenfeLoadingState: CircularProgressIndicator centrado. NO fillMaxWidth sobre el cÃ­rculo.
3. RenfeEmptyState: icono + tÃ­tulo + texto + acciÃ³n opcional.
4. RenfeErrorState: icono + tÃ­tulo + texto + "Reintentar" solo si hay callback.
5. RenfeStatusBadge: label, icon, type (SUCCESS/WARNING/ERROR/NEUTRAL). SUCCESS â†’ primaryContainer, WARNING â†’ tertiaryContainer, ERROR â†’ errorContainer, NEUTRAL â†’ surfaceVariant.
6. Migra loading/empty/error en pantallas SOLO si es mecÃ¡nico.
7. Preview 360Ã—800 claro/oscuro, fontScale 1.0/2.0. Spec 1080Ã—2400/480 con showSystemUi.
8. Comandos:
   - .\gradlew.bat :app:assembleDebug --no-daemon
   - .\gradlew.bat :app:testDebugUnitTest :app:lintDebug :app:assembleDebugAndroidTest --no-daemon

ACEPTACIÃ“N
- 5 componentes pequeÃ±os, no megacomponente.

CIERRE
Actualiza DESIGN.md y PROGRESS.md. DETENTE.
