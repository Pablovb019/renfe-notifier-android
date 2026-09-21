Ejecuta SOLO la fase 13. Cierre con evidencia.

ARCHIVOS A MODIFICAR
- ${ROOT}/DESIGN.md
- ${ROOT}/PROGRESS.md
- ${ROOT}/android/app/src/test/java/com/pablovb019/renfenotifier/ui/theme/ColorContrastTest.kt (ampliar si falta)
ARCHIVOS A CREAR
- ${ROOT}/android/app/src/androidTest/java/com/pablovb019/renfenotifier/ui/RedesignRegressionTest.kt
- ${ROOT}/design/validation/validation-matrix.md
- ${ROOT}/design/validation/contrast-report.md

PASOS

1. AuditorÃ­a alcance: git diff vs BASELINE_SHA. Backend, core/*, RenfeNotifierApp, ViewModels, manifiesto, CI intactos.
2. AuditorÃ­a estÃ¡tica:
   - rg "import androidx\.compose\.material\.(?!icons)" â†’ 0.
   - rg "Color\(0x" en pantallas â†’ solo ui/theme.
   - rg "dynamicColor" â†’ 0.
3. Contraste sobre esquemas finales. AÃ±ade pares si faltan.
4. Accesibilidad: TalkBack, â‰¥48 dp, sin info solo por color.
5. Previews: todas 360Ã—800 claro/oscuro, spec 1080Ã—2400/480, fontScale 1.0/1.3/2.0.
6. Prueba en Realme (o emulador 1080Ã—2400, 480 dpi):
   - wm size â†’ 1080x2400.
   - wm density â†’ 480.
   - Matriz 6 modos (app Ã— sistema).
   - Persistencia.
   - Fuentes 1.0 y 2.0.
7. Flujos con fakes. Sin OTP ni Renfe real.
8. Comandos finales:
   - .\gradlew.bat :app:assembleDebug --no-daemon
   - .\gradlew.bat :app:testDebugUnitTest --no-daemon
   - .\gradlew.bat :app:lintDebug --no-daemon
   - .\gradlew.bat :app:assembleDebugAndroidTest --no-daemon
   - Si dispositivo: .\gradlew.bat :app:connectedDebugAndroidTest --no-daemon
9. DESIGN.md final con inventario, tabla 36 roles, tipografÃ­a, ThemeMode, DEVICE_PROFILE, contraste, estados (implementado/probado local/emulador/Realme/pendiente), nota propuesta no oficial.
10. Sin push, merge, release.

ACEPTACIÃ“N FINAL
- Todos los comandos pasan.
- M3 exclusivo.
- Realme solo validado con evidencia.

CONTINGENCIA
Sin emulador: entrega y marca PENDIENTE. NUNCA "validado" sin evidencia.

CIERRE
Actualiza DESIGN.md y PROGRESS.md. DETENTE.
