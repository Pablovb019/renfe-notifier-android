Ejecuta SOLO la fase 5. Lee AGENTS.md, DESIGN.md, PROGRESS.md.

OBJETIVO
Eliminar todos los colores hardcodeados de las pantallas.

ARCHIVOS A MODIFICAR
- ${ROOT}/android/app/src/main/java/com/pablovb019/renfenotifier/feature/search/SearchScreen.kt (~501)
- ${ROOT}/android/app/src/main/java/com/pablovb019/renfenotifier/feature/followups/FollowUpsScreen.kt (~254, ~263)
- ${ROOT}/android/app/src/main/java/com/pablovb019/renfenotifier/feature/followups/FollowUpDetailScreen.kt (~418, ~427)
- ${ROOT}/android/app/src/main/java/com/pablovb019/renfenotifier/feature/diagnostics/DiagnosticsScreen.kt (~212, ~266, ~281, ~296, ~427-430)
- ${ROOT}/DESIGN.md, ${ROOT}/PROGRESS.md

PASOS

1. Sustituye en cada pantalla:
   - Color(0xFF1B7F3A) â†’ RenfeStatusBadge(BadgeType.SUCCESS) o primaryContainer.
   - Color.White sobre fondo â†’ rol on* correspondiente.
   - Color(0xFFB87333) â†’ BadgeType.WARNING.
   - Color(0xFFB00020) â†’ BadgeType.ERROR.
   - Color(0xFF75777F) â†’ BadgeType.NEUTRAL.
2. Sin cambios funcionales.
3. Grep global: rg "Color\(0x" en android/app/src/main/java debe dar 0 fuera de ui/theme.
4. Comandos:
   - .\gradlew.bat :app:assembleDebug --no-daemon
   - .\gradlew.bat :app:testDebugUnitTest :app:lintDebug --no-daemon

ACEPTACIÃ“N
- 0 literales de color en pantallas.
- 85/85 verdes.

CIERRE
Actualiza DESIGN.md y PROGRESS.md. DETENTE.
