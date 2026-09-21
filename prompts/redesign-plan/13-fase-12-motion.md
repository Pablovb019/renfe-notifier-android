Ejecuta SOLO la fase 12. Lee AGENTS.md, DESIGN.md, PROGRESS.md.

OBJETIVO
Movimiento sutil. Sin Lottie ni shared transitions.

ARCHIVOS A CREAR
- ${ROOT}/android/app/src/main/java/com/pablovb019/renfenotifier/ui/theme/Motion.kt
- ${ROOT}/android/app/src/androidTest/java/com/pablovb019/renfenotifier/ui/MotionBehaviorTest.kt
ARCHIVOS A MODIFICAR
- ${ROOT}/android/app/src/main/java/com/pablovb019/renfenotifier/navigation/AppNavHost.kt (solo enter/exit/popEnter/popExit)
- ${ROOT}/DESIGN.md, ${ROOT}/PROGRESS.md

PASOS

1. Motion.kt: object RenfeMotion { Short=150, Normal=200, Medium=250 }. renfeSpring() = spring(dampingRatio=0.9f, stiffness=Spring.StiffnessMedium).
2. AppNavHost: fade + slide corto. Conserva rutas, argumentos, navController, popUpTo, launchSingleTop, pendingFollowupId.
3. Badges/chips: animateColorAsState si mantiene 4.5:1 en intermedios. Si no, cambio de golpe.
4. Secciones expandibles: animateContentSize con rememberSaveable.
5. Cargaâ†’contenido: AnimatedContent o Crossfade 150-200 ms. NO reordena listas.
6. Respeta animator duration scale.
7. Comandos:
   - .\gradlew.bat :app:assembleDebug --no-daemon
   - .\gradlew.bat :app:testDebugUnitTest :app:lintDebug :app:assembleDebugAndroidTest --no-daemon

ACEPTACIÃ“N
- Movimiento corto. Sin doble callback.

CONTINGENCIA
Si rompe foco o jank, elimina.

CIERRE
Actualiza DESIGN.md y PROGRESS.md. DETENTE.
