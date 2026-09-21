Ejecuta SOLO la fase 2. Lee AGENTS.md, DESIGN.md, PROGRESS.md.

OBJETIVO
Refactorizar Theme.kt (dynamic color off) e introducir ThemeMode + ThemeViewModel sobre DataStore existente.

ARCHIVOS A MODIFICAR
- ${ROOT}/android/app/src/main/java/com/pablovb019/renfenotifier/ui/theme/Theme.kt
- ${ROOT}/android/app/src/main/java/com/pablovb019/renfenotifier/MainActivity.kt
ARCHIVOS A CREAR
- ${ROOT}/android/app/src/main/java/com/pablovb019/renfenotifier/ui/theme/ThemeMode.kt
- ${ROOT}/android/app/src/main/java/com/pablovb019/renfenotifier/ui/theme/ThemeViewModel.kt
- ${ROOT}/android/app/src/test/java/com/pablovb019/renfenotifier/ui/theme/ThemeModeTest.kt
- ${ROOT}/android/app/src/test/java/com/pablovb019/renfenotifier/ui/theme/ThemeViewModelTest.kt
ARCHIVOS A MODIFICAR
- ${ROOT}/DESIGN.md, ${ROOT}/PROGRESS.md

LECTURAS
- ${ROOT}/android/app/src/main/java/com/pablovb019/renfenotifier/core/security/PreferencesRepository.kt

PASOS

1. Theme.kt:
   - lightColorScheme(...) y darkColorScheme(...) con los 36 roles.
   - Firma: @Composable fun RenfeNotifierTheme(darkTheme: Boolean = isSystemInDarkTheme(), content: @Composable () -> Unit).
   - Elimina dynamicColor y dynamic*ColorScheme.
   - MaterialTheme(colorScheme, typography=RenfeTypography, shapes=RenfeShapes, content).
2. ThemeMode enum: fromStorage(value: String?) â†’ system/light/dark/null/desconocido â†’ SYSTEM. toStorage() â†’ strings existentes exactos. No enum.name.
3. ThemeViewModel:
   - Depende de SettingsSource.
   - mode: StateFlow<ThemeMode>.
   - isLoading, saveError: StateFlow.
   - setMode(mode) con viewModelScope. Serializa. Captura IOError. NO captura CancellationException.
   - Factory explÃ­cita con Application context.
4. MainActivity: sustituye observaciÃ³n manual por collectAsStateWithLifecycle del VM. MantÃ©n ApiModule.init, FcmTokenGateway, permisos, pendingFollowupId, handleIntent, onNewIntent.
5. darkTheme = when(mode) { SYSTEM â†’ isSystemInDarkTheme(); LIGHT â†’ false; DARK â†’ true }.
6. Tests JVM con fake SettingsSource.
7. Comandos:
   - .\gradlew.bat :app:testDebugUnitTest --tests "*ThemeModeTest" --tests "*ThemeViewModelTest" --no-daemon
   - .\gradlew.bat :app:assembleDebug --no-daemon
   - .\gradlew.bat :app:testDebugUnitTest :app:lintDebug --no-daemon
8. rg "dynamicColor|dynamicLightColorScheme|dynamicDarkColorScheme" â†’ 0.

ACEPTACIÃ“N
- Dynamic color eliminado.
- Sin claves nuevas en DataStore.
- Cambio de tema sin reiniciar Activity.

CIERRE
Actualiza DESIGN.md y PROGRESS.md. DETENTE.
