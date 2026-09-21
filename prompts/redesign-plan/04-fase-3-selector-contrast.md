Ejecuta SOLO la fase 3. Lee AGENTS.md, DESIGN.md, PROGRESS.md.

OBJETIVO
Selector de tema en Diagnostics + test de contraste AA + previews del tema.

ARCHIVOS A CREAR
- ${ROOT}/android/app/src/main/java/com/pablovb019/renfenotifier/ui/components/ThemeModeSelector.kt
- ${ROOT}/android/app/src/test/java/com/pablovb019/renfenotifier/ui/theme/ColorContrastTest.kt
- ${ROOT}/android/app/src/debug/java/com/pablovb019/renfenotifier/ui/theme/RenfeThemePreviews.kt
- ${ROOT}/android/app/src/androidTest/java/com/pablovb019/renfenotifier/ui/theme/ThemeModeSelectorTest.kt
ARCHIVOS A MODIFICAR
- ${ROOT}/android/app/src/main/java/com/pablovb019/renfenotifier/feature/diagnostics/DiagnosticsScreen.kt
- ${ROOT}/android/app/src/main/java/com/pablovb019/renfenotifier/navigation/AppNavHost.kt
- ${ROOT}/android/app/src/main/res/values/strings.xml
- ${ROOT}/DESIGN.md, ${ROOT}/PROGRESS.md

PASOS

1. ThemeModeSelector: lista vertical M3:
   - "SegÃºn el sistema" + descripciÃ³n.
   - "Claro".
   - "Oscuro".
   - selectableGroup, filas selectable con Role.RadioButton, RadioButton(onClick=null).
   - heightIn(min=56.dp). Ãrea â‰¥48Ã—48.
   - isLoading y saveError visibles.
2. Diagnostics: selector con VM compartido. NO segunda instancia. Conserva switch avisos, refreshEnvironment, sendTest, computeStages.
3. ColorContrastTest: relativeLuminance sRGB lineal, contrastRatio.
   Pares >=4.5: primary/onPrimary, primary/primaryContainer texto, secondary/onSecondary, tertiary/onTertiary, error/onError, background/onBackground, surface/onSurface, surfaceVariant/onSurfaceVariant, inverseSurface/inverseOnSurface.
   Pares >=3: outline/background, outline/surface, primary/background, error/background.
4. RenfeThemePreviews: @Preview con Button, Card, TextField, RadioButton, Switch. widthDp=360, heightDp=800, device="spec:width=1080px,height=2400px,dpi=480", uiMode noche y dÃ­a. Preview con showSystemUi=true. Preview fontScale=2f.
5. Comandos:
   - .\gradlew.bat :app:testDebugUnitTest --tests "*ColorContrastTest" --no-daemon
   - .\gradlew.bat :app:assembleDebug --no-daemon
   - .\gradlew.bat :app:testDebugUnitTest :app:lintDebug :app:assembleDebugAndroidTest --no-daemon

ACEPTACIÃ“N
- Un solo ThemeViewModel.
- Contrastes AA pasan.
- Preview 360Ã—800 y spec 1080Ã—2400/480.

CIERRE
Actualiza DESIGN.md y PROGRESS.md. DETENTE.
