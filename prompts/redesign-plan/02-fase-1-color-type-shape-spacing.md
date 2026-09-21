Ejecuta SOLO la fase 1. Lee AGENTS.md, DESIGN.md.

OBJETIVO
Crear el sistema visual completo: colores, tipografÃ­a, formas, espaciado.

ARCHIVOS A CREAR (o editar si existen)
- ${ROOT}/android/app/src/main/java/com/pablovb019/renfenotifier/ui/theme/Color.kt
- ${ROOT}/android/app/src/main/java/com/pablovb019/renfenotifier/ui/theme/Type.kt
- ${ROOT}/android/app/src/main/java/com/pablovb019/renfenotifier/ui/theme/Shape.kt
- ${ROOT}/android/app/src/main/java/com/pablovb019/renfenotifier/ui/theme/Spacing.kt
ARCHIVOS A MODIFICAR
- ${ROOT}/DESIGN.md, ${ROOT}/PROGRESS.md

PASOS

1. Color.kt: 36 roles M3 en claro y oscuro. Constantes `val` pÃºblicas.
   Tabla (LIGHT / DARK):
   primary #830065 / #D98BC7
   onPrimary #FFFFFF / #3D0030
   primaryContainer #FFD8EF / #62004C
   onPrimaryContainer #37002A / #FFD8EF
   inversePrimary #EFB1DA / #830065
   secondary #5D5E63 / #C5C5CB
   onSecondary #FFFFFF / #2F3035
   secondaryContainer #E2E2E8 / #45464B
   onSecondaryContainer #1A1B20 / #E2E2E8
   tertiary #885018 / #FFB877
   onTertiary #FFFFFF / #4B2700
   tertiaryContainer #FFDCBE / #6A3900
   onTertiaryContainer #2D1600 / #FFDCBE
   background #EFF3F6 / #1A1519
   onBackground #211A1F / #EEDFE8
   surface #FFF8FA / #1E191D
   onSurface #211A1F / #EEDFE8
   surfaceVariant #ECDFE7 / #50454D
   onSurfaceVariant #50454D / #D4C2CE
   surfaceTint #830065 / #D98BC7
   inverseSurface #362E33 / #EEDFE8
   inverseOnSurface #F9EEF4 / #362E33
   error #BA1A1A / #FFB4AB
   onError #FFFFFF / #690005
   errorContainer #FFDAD6 / #93000A
   onErrorContainer #410002 / #FFDAD6
   outline #82747D / #9D8D97
   outlineVariant #CFC3CC / #50454D
   scrim #000000 / #000000
   surfaceBright #FFF8FA / #453B42
   surfaceDim #E3D7DE / #1A1519
   surfaceContainer #F6ECF2 / #251F24
   surfaceContainerHigh #F0E6EC / #30292F
   surfaceContainerHighest #EADFE5 / #3B343A
   surfaceContainerLow #FDF2F8 / #211B20
   surfaceContainerLowest #FFFFFF / #151014

2. Type.kt: RenfeTypography con FontFamily.SansSerif.
   headlineLarge 28/36, headlineMedium 24/32, headlineSmall 24/28.
   titleLarge 20/28, titleMedium 16/24 SemiBold, titleSmall 14/20 Medium.
   bodyLarge 16/24, bodyMedium 14/20, bodySmall 12/16.
   labelLarge 14/20 Medium, labelMedium 12/16 Medium, labelSmall 12/16.
3. Shape.kt: 4/8/12/20/28 dp.
4. Spacing.kt: object RenfeSpacing con 4, 8, 12, 16, 20, 24, 32 dp. screenMargin=16.dp, screenMarginWide=20.dp.
5. .\gradlew.bat :app:assembleDebug --no-daemon (Ãºltimas 10 lÃ­neas).
6. .\gradlew.bat :app:testDebugUnitTest --no-daemon (resumen).

ACEPTACIÃ“N
- Cuatro archivos creados.
- labelSmall >= 12 sp.

CIERRE
Actualiza DESIGN.md y PROGRESS.md. DETENTE.
