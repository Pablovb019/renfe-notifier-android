# DESIGN.md - Rediseno UI Material 3 (renfe-notifier-android)

> Documento base del rediseno visual. Fase 0 (auditoria) ejecutada el 2026-09-21.
> ROOT = `C:\Users\pablo\Downloads\renfe-notifier-android`.
> Ningun archivo funcional fue modificado en la fase 0 (solo se crea este documento y se
> anade la entrada en PROGRESS.md).

## BASELINE_SHA

- HEAD auditado: `f5943c33bdd63207761e7266bed58e293e208687`
- Rama de trabajo: `redesign/ui-m3` (prohibido push a `main` y force-push durante el rediseno).
- Estado git al auditar: arbol con modificaciones PREEExistentes sin commitear en
  `prompts/redesign-plan/*` (reparacion de mojibake de una sesion previa) y ficheros sin
  seguir (`fix-mojibake.ps1`, `quitar-bom.ps1`, `prompts/redesign-plan-backup-20260921-225023/`).
  Se respeta AGENTS.md 4 (`prompts/` intacta): NO se incluyen en el commit de la fase 0.

## STACK (verificado, sin arquitectura inventada)

| Componente | Version | Fuente |
|---|---|---|
| JDK | 17.0.20.1 LTS (Microsoft OpenJDK) | `java -version` |
| Gradle (wrapper) | 8.9 | `android/gradle/wrapper/gradle-wrapper.properties:3` |
| AGP | 8.5.2 | `android/gradle/libs.versions.toml:2` |
| Kotlin | 2.0.21 (plugin compose) | `android/gradle/libs.versions.toml:4` |
| Compose BOM | 2024.12.01 | `android/gradle/libs.versions.toml:8` |
| material3 | sin version explicita (la resuelve el BOM) | `android/app/build.gradle.kts:104` |
| navigation-compose | 2.8.5 | `android/gradle/libs.versions.toml:9` |
| androidx.datastore-preferences | 1.1.1 | `android/gradle/libs.versions.toml:14` |
| firebase-bom / messaging | 33.7.0 / BOM | `android/gradle/libs.versions.toml:15` |
| work-runtime-ktx | 2.9.1 | `android/gradle/libs.versions.toml:17` |
| compileSdk / minSdk / targetSdk | 34 / 26 / 34 | `android/app/build.gradle.kts:17,22,24` |
| versionCode / versionName | 11 / 0.1.10 | `android/app/build.gradle.kts:25-26` |
| BACKEND_URL por defecto | `http://34.26.252.164:8000/` | `android/app/build.gradle.kts:35` |

Rutas relativas a `ROOT/`.

## DEVICE_PROFILE (medido por ADB el 2026-09-21, no inventado)

- Dispositivo: realme GT Neo 2, `RMX3370`, Android 13 (`ro.build.version.release` = 13).
- Seriales ADB detectados: USB `1ecdc196` y Wi-Fi `192.168.1.200:5555` (mismo modelo; lecturas sobre `1ecdc196`).
- `adb shell wm size` -> `Physical size: 1080x2400` (px).
- `adb shell wm density` -> `Physical density: 480` (dpi).
- Calculo dp: `ancho_dp = 1080 / (480/160) = 360 dp`; `alto_dp = 2400 / 3 = 800 dp`.
  **Confirma el DEVICE_PROFILE 360x800 dp.**
- `settings get system font_scale` -> `1.0` (100 %).
- `cmd uimode night` -> `Night mode: yes` (modo oscuro activo por defecto en el dispositivo).
- Insets (display principal 1080x2400, `dumpsys window displays`):
  - `ITYPE_STATUS_BAR` frame=[0,0][1080,110] visible=true -> 110 px ~= 36.7 dp.
  - `ITYPE_NAVIGATION_BAR` frame=[0,2400][1080,2400] visible=false (navegacion por gestos).
  - `ITYPE_IME` invisible en el momento de la lectura; visibleFrame=[0,2268][1080,2400] y
    hint inferior `bottom=897` px cuando se muestra.

## BASELINE COMPILACION / TESTS / LINT (ejecutado con salida real)

- `.\gradlew.bat :app:assembleDebug --no-daemon` -> **BUILD SUCCESSFUL in 15s**, 38 tasks
  up-to-date, exit 0.
- `.\gradlew.bat :app:testDebugUnitTest --no-daemon` -> **BUILD SUCCESSFUL**, exit 0.
  Resumen del reporte XML (`app/build/test-results/testDebugUnitTest/*.xml`):
  **10 suites, 85 tests, 0 failures, 0 errors**.
  (Nota: coincide con el incremento a 85 tras los 3 tests de SearchViewModel del lote 37.)
- `.\gradlew.bat :app:lintDebug --no-daemon` -> **BUILD SUCCESSFUL**, exit 0.
  Resumen del reporte (`app/build/reports/lint-results-debug.txt`): **0 errores, 53 warnings**
  = 42 `GradleDependency` + 4 `UnusedResources` + 3 `AndroidGradlePluginVersion`
  + 1 `MissingApplicationIcon` + 1 `PluralsCandidate` + 1 `HardwareIds`.

## AUDITORIA THEME (paso 5)

- `ui/theme/Theme.kt:15-31` `LightColors`: paleta fija M3 de respaldo; `primary = Color(0xFF0057A6)` (azul Renfe).
- `ui/theme/Theme.kt:33-49` `DarkColors`: paleta fija oscura (primary `0xFFA8C8FF`).
- `ui/theme/Theme.kt:55-75` `RenfeNotifierTheme(darkTheme, dynamicColor = true)`: usa color
  dinamico `dynamicLight/DarkColorScheme` si `SDK >= 31`; si no, la paleta fija.
- `MainActivity.kt:50-63`: observa `PreferencesRepository.theme` (DataStore) y resuelve
  `darkTheme` (system -> `isSystemInDarkTheme()`). El tema NO es dinamico por switch en
  runtime: el valor del DataStore fluye y recompona el composable en `setContent`.
- `ui/theme/Type.kt:9`: `val Typography = Typography()` -> escala Material 3 por defecto,
  sin personalizar (README del propio archivo lo indica).

## AUDITORIA PERSISTENCIA (paso 6)

- `core/security/PreferencesRepository.kt:14`: DataStore `name = "app_preferences"`
  (fichero `app_preferences.preferences_pb`).
- Clave de tema: `KEY_THEME = "theme"` string con default `THEME_SYSTEM`; valores válidos
  `"system"` / `"light"` / `"dark"` (`PreferencesRepository.kt:45,72-75`).
- `KEY_ALERTS_ENABLED = "alerts_enabled"` bool default `true` (`PreferencesRepository.kt:46,82`).
- Otras claves: `is_paired`, `device_id`, `device_name`, `backend_url` (`PreferencesRepository.kt:77-81`).
- Se persiste/lee tambien desde `DiagnosticsViewModel.setTheme` (paso 25).
- Lo secreto (token) vive cifrado en Keystore (`TokenVault`), no en este DataStore.

## CONTRATOS UI (paso 7) - tabla compacta

| Pantalla | Ruta (AppNavHost) | Estado UI | Callbacks | Efectos | `enabled` clave |
|---|---|---|---|---|---|
| HOME | `home` | `HomeUiState(versionName, now, loading, isPaired)` | onNavigateToPairing / Search / FollowUps / Diagnostics; `onRefresh = {}` (vacio) | `LifecycleResumeEffect` refresca permiso notifs (HomeScreen.kt:51-54) | CTA emparejar solo si `!isPaired`; botones de navegacion siempre |
| PAIRING | `pairing` | `PairingUiState(code, isLoading, error, paired)` | onCodeChange, onClaim | `LaunchedEffect(paired)` -> onPaired (PairingScreen.kt:65-67) | Claim: `!isLoading && code.isNotBlank` (PairingScreen.kt:118) |
| SEARCH | `search` | `SearchUiState` (consultas, sugerencias, fecha, plazaH, trenes, mode, creando, errores, createdFollowUpId, availableTrainNotice) | onOrigin/Destination... , onDateSelected, onPlazaHChange, onSearch, onModeSelected, onTrainSelected, onCreateFollowUp, onCreatedAccepted | `DatePickerDialog`; `AlertDialog` creado / ya-tiene-plazas; debounce en VM | Buscar: `!isSearching` (SearchScreen.kt:204); Crear: `!isCreatingFollowUp` (SearchScreen.kt:455) |
| FOLLOWUPS | `followups` | `FollowUpsUiState(filter, items, loading, error)` | onFilterSelected, onRefresh(=load), onOpenDetail | `LaunchedEffect(Unit){load()}` (FollowUpsScreen.kt:58) | - |
| FOLLOWUP_DETAIL | `followup/{followupId}` | `FollowUpDetailUiState(detail, actionInProgress, deleted, ...)` | onPause / Resume / Renew / Acknowledge / Delete | load en arranque; `LaunchedEffect(deleted)` -> onDeleted (68-71) | Acciones: `!actionInProgress` (FollowUpDetailScreen.kt:312) |
| DIAGNOSTICS | `diagnostics` | `DiagnosticsUiState(theme, alertsEnabled, stages, diagnostics, testNotificationState...)` | onSetTheme, onSetAlertsEnabled, onSendTest | `LaunchedEffect` refreshEnvironment + load (63-66) | Enviar test: no `Sending` (DiagnosticsScreen.kt:163) |

Navegacion: 6 destinos declarados en `navigation/AppNavHost.kt:19-29`; grafo en `AppNavHost.kt:57-130`.

## PERIMETRO PROTEGIDO

- `prompts/` y `prompts/redesign-plan/`: NO modificables (AGENTS.md 4).
- `backend/`: fuera del alcance de este rediseno (solo UI Android).
- Produccion: VM, FCM, releases, repositorio original `Pablovb019/renfe-notifier-bot`:
  intocadas en el rediseno.
- Git: push SOLO a `redesign/ui-m3`; nunca `main` ni force-push. Sin merges durante el rediseno.
- Sin creacion real de seguimientos ni consultas de prueba (fixtures/mocks).

## RIESGOS PARA 360dp (paso 8) - con file:linea

1. **Colores hardcodeados fuera del theme** (rompen tema dinamico/oscuro): verde
   `0xFF1B7F3A` en `SearchScreen.kt:501`, `FollowUpsScreen.kt:254,263`,
   `FollowUpDetailScreen.kt:418,428` y `DiagnosticsScreen.kt:266,281,297,427`; naranja
   `0xFFB87333` y rojo `0xFFB00020` en `DiagnosticsScreen.kt:428-429`; `Color.White` en
   `DiagnosticsScreen.kt:212`. Candidatos a migracion al color scheme (fases 1/5).
2. **Row con >3 elementos**: `FollowUpsScreen.kt:95-108` -> los 5 `FilterChip`
   (Todos/Activos/Pausados/Vencidos/Eliminados) en una unica Row sin scroll horizontal:
   riesgo real de desborde en 360dp. `SearchScreen.kt:429-445` (3 chips de modo) y
   `DiagnosticsScreen.kt:382-390` (3 chips de tema): ajustados, a vigilar con otro font_scale.
3. **Text sin maxLines**: `SearchScreen.kt:175` fecha larga `"EEEE d 'de' MMMM 'de' yyyy"`
   dentro de Row `SpaceBetween` (166-182); puede envolver y empujar el boton en 360dp.
   `DiagnosticsScreen.kt:220-223` `stage.detail` sin maxLines.
4. **Scrolls anidados**: `SearchScreen.kt:325-351` -> sugerencias de estacion con
   `verticalScroll()` dentro de un `item` de la `LazyColumn` (139-259): scroll vertical
   anidado con riesgo de conflicto de gestos.
5. **Botones con callback vacio**: `HomeScreen.kt:57` `onRefresh = {}` -> el boton
   "Recargar" del `TopAppBar` (HomeScreen.kt:87-95) no hace nada. (`MainActivity.kt:38-40`
   callback vacio del launcher de permisos es intencional: la UI se actualiza al resume).
6. **Tipografia**: `Type.kt:9` escala M3 por defecto (a personalizar en fases 1/5).
7. **Tema dinamico vs identidad**: con `dynamicColor=true` en Android 13 el color real es
   del tema del sistema, no el azul Renfe; la UI debe validarse tanto con paleta dinamica
   como con la fija (y en claro y oscuro).

## BLOQUEOS

- Ninguno para la fase 0. Dispositivo presente y medido; baseline verde.
- PENDIENTE (no superado por no ejecutarse: no procede en fase 0): nada de UI implementado
  aun (las fases 1+ lo haran paso a paso).

## FASE 1 - SISTEMA VISUAL: color / tipo / forma / espaciado (2026-09-21)

- **Objetivo cumplido**: se crea el sistema de tokens visuales; NO se cablea aun en
  `Theme.kt` (eso es la fase 2).
- **Archivos creados**:
  - `android/app/src/main/java/com/pablovb019/renfenotifier/ui/theme/Color.kt`: 36 roles M3
    (48 constantes `val` publicas Light/Dark) con la paleta de la especificacion: primario
    magenta `#830065` (cLaro) / `#D98BC7` (oscuro), terciario `#885018`/`#FFB877`, fondo
    `#EFF3F6`/`#1A1519`, superficies contenedoras propias, error/outline, scrim negro.
  - `android/app/src/main/java/com/pablovb019/renfenotifier/ui/theme/Type.kt`: `RenfeTypography`
    sobre `FontFamily.SansSerif`; escala `tamano/lineHeight`: headlineLarge 28/36, headlineMedium
    24/32, headlineSmall 24/28, titleLarge 20/28, titleMedium 16/24 SemiBold, titleSmall 14/20
    Medium, bodyLarge 16/24, bodyMedium 14/20, bodySmall 12/16, labelLarge 14/20 Medium,
    labelMedium 12/16 Medium, labelSmall 12/16. **labelSmall = 12 sp (>= 12 sp, cumple la
    aceptacion).** Se conserva `val Typography = Typography()` como puente para `Theme.kt:72`
    (el cableado de `RenfeTypography` llega en la fase 2).
  - `android/app/src/main/java/com/pablovb019/renfenotifier/ui/theme/Shape.kt`: `RenfeShapes`
    (material3.Shapes) con extraSmall/small/medium/large/extraLarge = 4/8/12/20/28 dp.
  - `android/app/src/main/java/com/pablovb019/renfenotifier/ui/theme/Spacing.kt`: `object
    RenfeSpacing` con xs/sm/md/lg/xl/xxl/xxxl = 4/8/12/16/20/24/32 dp, `screenMargin = 16.dp`
    y `screenMarginWide = 20.dp`.
- **Verificacion (salida real, exit 0)**:
  - `.\gradlew.bat :app:assembleDebug --no-daemon` -> **BUILD SUCCESSFUL in 29s** (6 tasks ejecutados, 32 up-to-date).
  - `.\gradlew.bat :app:testDebugUnitTest --no-daemon` -> **BUILD SUCCESSFUL in 22s**;
    reporte XML: **10 suites, 85 tests, 0 failures, 0 errors** (sin regresiones).
- **Estado de los riesgos de la fase 0**: los tokens existen pero los colores hardcodeados
  de las pantallas siguen sin migrar (fase 5). El tema sigue usando la paleta fija
  `LightColors`/`DarkColors` de `Theme.kt` y el color dinamico; la escala tipografica activa
  sigue siendo `Typography()` (por defecto) hasta el cableado de la fase 2.
- **Comprobado**: los 4 archivos existen (aceptacion) y `labelSmall` >= 12 sp (aceptacion).
  Sin cambio funcional: la UI no varía visualmente hasta la fase 2.

## FASE 2 - THEME: dynamic color OFF + ThemeMode + ThemeViewModel (2026-09-21)

- **Objetivo cumplido**: `Theme.kt` refactorizado (solo paleta fija, sin dynamic color),
  modo de tema gestionado por `ThemeMode` + `ThemeViewModel` sobre el DataStore existente,
  y `MainActivity` observa el estado con `collectAsStateWithLifecycle`.
- **Archivos creados**:
  - `ui/theme/ThemeMode.kt`: enum `SYSTEM/LIGHT/DARK`; `fromStorage(String?)` mapea
    `"system"/"light"/"dark"` y cualquier otro valor/null a `SYSTEM`; `toStorage()` devuelve
    los strings exactos de `PreferencesRepository.THEME_SYSTEM/LIGHT/DARK`
    (`android/app/src/main/java/com/pablovb019/renfenotifier/core/security/PreferencesRepository.kt:73-75`),
    nunca `enum.name`.
  - `ui/theme/ThemeViewModel.kt`: depende de `SettingsSource` (`PreferencesRepository.kt:24-29`);
    `mode`/`isLoading`/`saveError` como `StateFlow`; `setMode()` optimista con escrituras
    serializadas (se cancela el guardado anterior, gana la ultima); captura SOLO `IOException`
    (lectura inicial y escritura). `ThemeViewModelFactory(Application)` explicita
    (`ThemeViewModel.kt:49-59`).
  - Tests: `src/test/.../ui/theme/ThemeModeTest.kt` (4) y `ThemeViewModelTest.kt` (7) con
    `FakeSettingsSource` y `StandardTestDispatcher` como Main.
- **Archivos modificados**:
  - `ui/theme/Theme.kt`: `lightColorScheme`/`darkColorScheme` con los 36 roles de `Color.kt`
    (lineas 9-49); firma `RenfeNotifierTheme(darkTheme: Boolean = isSystemInDarkTheme(), content)`
    (lineas 55-65); `MaterialTheme(colorScheme, typography = RenfeTypography, shapes = RenfeShapes,
    content)`. Eliminados imports de `dynamic*ColorScheme`, `Build` y `LocalContext`
    (antes lineas 3-12).
  - `MainActivity.kt`: sustituida la observacion manual de `prefs.theme` en `setContent`
    (antes lineas 51-60) por `viewModel(factory = ThemeViewModelFactory(application))` +
    `collectAsStateWithLifecycle` (lineas 50-63); `darkTheme = when(mode) { SYSTEM ->
    isSystemInDarkTheme(); LIGHT -> false; DARK -> true }`. Se conservan `ApiModule.init`,
    `FcmTokenGateway`, permisos, `pendingFollowupId`, `handleIntent` y `onNewIntent`.
  - `ui/theme/Type.kt`: el puente `val Typography = Typography()` ya no es necesario
    (Theme.kt usa ahora `RenfeTypography`) y se mantiene sin uso pendiente de migracion.
- **Sin claves nuevas en DataStore**: `PreferencesRepository.kt` intacto; `ThemeMode` reutiliza
  la clave `"theme"` y los valores `"system"/"light"/"dark"` existentes (aceptacion).
- **Verificacion (salida real, exit 0)**:
  - `.\gradlew.bat :app:testDebugUnitTest --tests "*ThemeModeTest" --tests "*ThemeViewModelTest" --no-daemon` ->
    BUILD SUCCESSFUL in 33s; XML: `ThemeModeTest` tests=4 failures=0, `ThemeViewModelTest` tests=7 failures=0.
  - `.\gradlew.bat :app:assembleDebug --no-daemon` -> BUILD SUCCESSFUL in 20s.
  - `.\gradlew.bat :app:testDebugUnitTest :app:lintDebug --no-daemon` -> BUILD SUCCESSFUL in 59s;
    suite completa **12 suites / 96 tests / 0 failures / 0 errors** (baseline era 10/85);
    lint: **0 errors / 53 warnings** (igual que baseline).
  - `rg "dynamicColor|dynamicLightColorScheme|dynamicDarkColorScheme"` -> 0 coincidencias (paso 8).
- **Aceptacion**: dynamic color eliminado (0 matches); sin claves nuevas en DataStore;
  cambio de tema sin reiniciar Activity (el VM + StateFlow propaga el guardado y la
  recomposicion; el modo se aplica en vivo con `collectAsStateWithLifecycle`).
- **Pendiente**: selector de modo de tema visible en la UI (fase 3) y migracion de
  colores hardcodeados de las pantallas (fase 5). Detenido a la espera de instrucciones.

## FASE 3 - SELECTOR DE TEMA + CONTRASTE AA + PREVIEWS (2026-09-21)

- **Objetivo cumplido**: selector de modo de tema en Diagnósticos usando el mismo
  `ThemeViewModel` (una sola instancia), test de contraste AA sobre la paleta real y
  previews del tema para el perfil del dispositivo.
- **Archivos creados**:
  - `ui/components/ThemeModeSelector.kt`: lista vertical M3 con `selectableGroup`; filas
    `selectable` con `Role.RadioButton` y `RadioButton(onClick = null)`; `heightIn(min = 56.dp)`;
    `testTag` por opción; muestra `isLoading` (indicador + "Cargando el tema…") y `saveError`.
  - `src/test/.../ui/theme/ColorContrastTest.kt`: luminancia relativa sRGB lineal (WCAG 1.4.3)
    y `contrastRatio`; 9 pares de texto >= 4.5:1 y 4 pares no textuales >= 3:1, en claro y oscuro
    (4 tests), sobre los tokens reales de `Color.kt`.
  - `src/debug/.../ui/theme/RenfeThemePreviews.kt`: 4 `@Preview` (claro/oscuro y fontScale=2f)
    con `widthDp = 360, heightDp = 800, device = "spec:width=1080px,height=2400px,dpi=480"`,
    `showSystemUi = true` en las dos primeras; contenido con Button, Card, OutlinedTextField,
    RadioButton y Switch.
  - `src/androidTest/.../ui/theme/ThemeModeSelectorTest.kt`: 5 tests instrumentales Compose con
    `createAndroidComposeRule<ComponentActivity>()` (opciones, clic light/dark, loading, error).
- **Archivos modificados**:
  - `feature/diagnostics/DiagnosticsScreen.kt`: sustituidos los `FilterChip` de tema por
    `ThemeCard` + `ThemeModeSelector` (lineas 139-148 y nuevo `ThemeCard`); `DiagnosticsContent`
    recibe `themeMode/themeLoading/themeSaveError/onSelectTheme`; se conservan switch de avisos,
    refreshEnvironment, sendTest y computeStages. `DiagnosticsViewModel` intacto.
  - `navigation/AppNavHost.kt`: nuevo parametro `themeViewModel: ThemeViewModel` (lineas 44-47)
    reenviado a `DiagnosticsScreen` (linea 117).
  - `MainActivity.kt`: pasa `themeViewModel` al `AppNavHost` (lineas 62-65). El `ThemeViewModel`
    se crea una sola vez, con ámbito de Activity, y se comparte por parametro (aceptacion
    "un solo ThemeViewModel").
  - `main/res/values/strings.xml`: `diag_theme_system` pasa a valer "Según el sistema" (linea 151),
    nuevas `diag_theme_system_desc` y `diag_theme_loading`.
  - Infra (necesaria para compilar el test instrumental): `gradle/libs.versions.toml` anade
    `ui-test-junit4` y `ui-test-manifest` (versiones gestionadas por el Compose BOM, sin numeros
    nuevos); `app/build.gradle.kts` anade `androidTestImplementation(platform(BOM))`,
    `androidTestImplementation(ui-test-junit4)`, `androidTestImplementation(test-ext)` y
    `debugImplementation(ui-test-manifest)` (build.gradle.kts lineas 119-125).
- **Verificacion (salida real, exit 0)**:
  - `:app:testDebugUnitTest --tests "*ColorContrastTest"` -> BUILD SUCCESSFUL; tests=4 failures=0.
  - `:app:assembleDebug` -> BUILD SUCCESSFUL in 53s.
  - `:app:testDebugUnitTest :app:lintDebug :app:assembleDebugAndroidTest` -> BUILD SUCCESSFUL in 1m12s;
    suite completa **13 suites / 100 tests / 0 failures / 0 errors** (fase 2: 12/96);
    lint **0 errors / 53 warnings**; se genero `app-debug-androidTest.apk`.
- **Aceptacion**: un solo `ThemeViewModel` (creado en MainActivity y pasado explicitamente);
  contrastes AA pasan (13 pares/4 tests en ambos modos); previews 360x800 con spec 1080x2400/480.
- **Nota honesta**: los tests de `ThemeModeSelectorTest` estan compilados (APK instrumental OK) pero
  NO ejecutados en emulador/dispositivo en esta fase (no hay `connectedDebugAndroidTest` en el plan);
  se marcara "validado" cuando se lancen sobre el Realme en la fase de validacion.
- **Pendiente**: componentes base reutilizables (fase 4) y migracion de colores (fase 5).
  Detenido a la espera de instrucciones.

## FASE 4 - COMPONENTES BASE REUTILIZABLES (2026-09-22)

- **Objetivo cumplido**: 5 componentes UI pequeños e independientes en `ui/components/`,
  previews del set y tests instrumentales; migracion de loading/error SOLO donde fue
  mecanica (FollowUps y FollowUpDetail). Sin megacomponentes (aceptacion).
- **Archivos creados**:
  - `ui/components/RenfeScreenScaffold.kt`: Scaffold M3 + `TopAppBar`. Parametros
    `title`, `onBack: (() -> Unit)?` (null -> sin boton atras), `actions` (lambda, vacia por
    defecto) y `content: @Composable (PaddingValues) -> Unit`; consume `innerPadding` UNA vez
    (linea 44) y delega al content. Titulo con `titleLarge` (24 sp), `maxLines = 1`,
    `TextOverflow.Ellipsis`; `contentDescription` del boton atras = `R.string.common_back`.
  - `ui/components/RenfeLoadingState.kt`: `Box(fillMaxSize) + contentAlignment = Center`
    con `CircularProgressIndicator()` **sin** `fillMaxWidth` sobre el circulo; `testTag`.
  - `ui/components/RenfeEmptyState.kt`: icono (contentDescription null) + titulo `titleMedium`
    + texto `bodyMedium` onSurfaceVariant + `actionLabel`/`onAction` opcionales
    (`TextButton` solo si ambos presentes); `testTag`.
  - `ui/components/RenfeErrorState.kt`: icono (tint error) + titulo + texto; boton
    "Reintentar" (`R.string.common_retry`) SOLO si `onRetry != null`; `testTag`.
  - `ui/components/RenfeStatusBadge.kt`: `enum RenfeStatusType (SUCCESS/WARNING/ERROR/NEUTRAL)`;
    fondo `RoundedCornerShape(4.dp)` con `primaryContainer`/`tertiaryContainer`/`errorContainer`/
    `surfaceVariant`, contenido `on*Container`/`onSurfaceVariant`, icono nullable 14 dp,
    `labelSmall`. `modifier` = primer parametro opcional (lint ModifierParameter).
  - `src/debug/.../ui/components/RenfeComponentsPreviews.kt`: 4 `@Preview` 360x800,
    claro/oscuro, fontScale 1x (showSystemUi=true) y 2x, device `"spec:width=1080px,
    height=2400px,dpi=480"`; contenido combinado en `RenfeScreenScaffold`: badges, empty y error.
  - `src/androidTest/.../ui/components/RenfeComponentsTest.kt`: 7 tests Compose con
    `createAndroidComposeRule<ComponentActivity>()`: titulo+atras del scaffold (conteo de
    clics), loading tag, empty con accion, empty sin boton, error sin "Reintentar", error
    con "Reintentar" clickable, y badges (labels visibles). Compilados, NO ejecutados en
    dispositivo/emulador (igual que fase 3).
- **Archivos modificados (migracion mecanica loading/error)**:
  - `main/res/values/strings.xml`: nuevas `common_back` "Volver" (linea 163) y `common_retry`
    "Reintentar" (linea 164), comunes a los componentes; eliminada `followups_retry` (ya sin
    uso tras la migracion -> fuera de los 4 UnusedResources).
  - `feature/followups/FollowUpsScreen.kt`: carga -> `RenfeLoadingState` (117) y bloque de
    error de 3 `Text` + retry -> `RenfeErrorState` con `Icons.Filled.Warning`,
    `followups_load_error` y `onRetry = onRefresh` (120-126). Eliminados imports en desuso
    (`Box`, `Alignment`, `CircularProgressIndicator`).
  - `feature/followups/FollowUpDetailScreen.kt`: indicador de carga `fillMaxWidth` ->
    `RenfeLoadingState` (126); branch de error (sin detail) -> `RenfeErrorState` con
    `Icons.Filled.Warning`, `followups_load_error` y `text = uiState.actionError ?:
    followups_no_checks` (198-203); reintento del error -> `onRetry = onRefresh`. Se conserva
    el `CircularProgressIndicator` del boton de accion (311, spinner inline, fuera de alcance).
  - NO migrados (no mecanicos): empties (solo `Text` sin icono/descripcion) y spinners
    inline de `SearchScreen.kt:223,461`, `HomeScreen.kt:140`, `DiagnosticsScreen.kt:140`,
    `PairingScreen.kt:122` (estados parciales, no estados de pantalla).
- **Verificacion (salida real, exit 0)**:
  - `.\gradlew.bat :app:assembleDebug --no-daemon` -> BUILD SUCCESSFUL in 44s (12 ejecutados,
    26 up-to-date).
  - `.\gradlew.bat :app:testDebugUnitTest :app:lintDebug :app:assembleDebugAndroidTest --no-daemon`
    -> BUILD SUCCESSFUL in 1m; **13 suites / 100 tests / 0 failures / 0 errors** (igual que
    fase 3, sin regresiones); lint: 0 errors / **54 warnings** (aparece `ModifierParameter`
    en `RenfeStatusBadge` -> reordenado `modifier` primero, sin cambios en llamadas que ya
    usaban `type =`); `app-debug.apk` y `app-debug-androidTest.apk` generados.
  - Re-verificacion tras el fix de lint: `:app:assembleDebug :app:lintDebug` -> BUILD
    SUCCESSFUL in 1m6s; lint **0 errors / 53 warnings** (baseline).
- **Aceptacion**: 5 componentes pequenos (ningun megacomponente); loading sin fillMaxWidth
  sobre el indicador; previews 360x800 claro/oscuro fontScale 1/2 spec 1080x2400/480;
  builds, 100 tests de JVM y lint al baseline.
- **Nota honesta**: `RenfeComponentsTest` (7 tests) y `ThemeModeSelectorTest` (5 tests)
  compilados con `assembleDebugAndroidTest` pero NO ejecutados; la ejecucion sobre el Realme
  se marcara como "validado" en la fase de validacion.
- **Pendiente**: migracion de colores hardcodeados de las pantallas (fase 5). Detenido a la
  espera de instrucciones.

## FASE 5 - MIGRACION DE COLORES HARDCODEADOS (2026-09-22)

- **Objetivo cumplido**: 0 literales de color (`Color(0x...)`, `Color.White/Black/Red/Gray`)
  fuera de `ui/theme`. Sin cambios funcionales.
- **Decision**: en la fase 5 del plan el enum de la insignia se referencia como `BadgeType`
  (`RenfeStatusBadge(BadgeType.SUCCESS)`), por lo que se renombra el enum `RenfeStatusType`
  -> `BadgeType` (mismo archivo `ui/components/RenfeStatusBadge.kt:19`), junto con sus usos en
  `src/debug` (previews) y `src/androidTest` (RenfeComponentsTest).
- **Sustituciones** (mapeo segun prompt, sobre roles de `MaterialTheme.colorScheme`):
  - `Color(0xFF1B7F3A)` (exito/activo/disponible) -> `onPrimaryContainer` en **texto** sobre
    superficie: el rol contenedor `primaryContainer` como color de texto romperia el contraste
    AA (LightPrimaryContainer `#FFD8EF` vs LightSurface `#FFF8FA` ~= 1.1:1), asi que para
    texto se usa el rol de contenido de `BadgeType.SUCCESS` definido en fase 4
    (`onPrimaryContainer`), que cumple AA con amplio margen (claro 16.8:1, oscuro 13.5:1).
  - `Color.White` sobre fondo de estado -> eliminado: ese fondo pasivo ahora lo pinta el propio
    `RenfeStatusBadge` (contenidos `onPrimaryContainer/onTertiaryContainer/onErrorContainer/
    onSurfaceVariant`).
  - `Color(0xFFB87333)` / `Color(0xFFB00020)` / `Color(0xFF75777F)` ya no existen: el mapeo de
    sitio en sitio usa `tertiary`/`error`/`onSurfaceVariant` (fase 4) o el badge con
    `BadgeType.WARNING/ERROR/NEUTRAL`.
- **Archivos modificados**:
  - `feature/diagnostics/DiagnosticsScreen.kt`:
    - `StageCard` (lineas 215-219): el `Box` con `background(statusColor)` + `Text(Color.White)`
      que simulaba una insignia se sustituye por `RenfeStatusBadge(label, icon = null,
      type = statusType(stage.status))`. Se elimina `statusColor(status): Color` (antes
      450-455) y se crea `statusType(status): BadgeType` (linea 439): OK->SUCCESS,
      ATTENTION->WARNING, BLOCKED->ERROR, UNKNOWN->NEUTRAL.
    - `ServerCard` (lineas 271, 286, 301): los 3 textos de estado OK/permiso/canal pasan de
      `Color(0xFF1B7F3A)` a `MaterialTheme.colorScheme.onPrimaryContainer`.
    - Imports sin uso eliminados: `background`, `Box`, `RoundedCornerShape`,
      `androidx.compose.ui.graphics.Color`; anadidos `BadgeType` y `RenfeStatusBadge`
      (linea 39-40).
  - `feature/followups/FollowUpsScreen.kt:239,248` (`lifecycleColor`/`availabilityColor`):
    rama "active"/"available" de verde -> `onPrimaryContainer` (el resto ya usaba
    tertiary/error/onSurfaceVariant).
  - `feature/followups/FollowUpDetailScreen.kt:415,424`: idem (`FollowUpLifecycle.ACTIVE`,
    "available" -> `onPrimaryContainer`).
  - `feature/search/SearchScreen.kt:501`: `Availability.AVAILABLE` -> `onPrimaryContainer`.
  - `ui/components/RenfeStatusBadge.kt`, `src/debug/.../RenfeComponentsPreviews.kt`,
    `src/androidTest/.../RenfeComponentsTest.kt`: renombrado `RenfeStatusType` -> `BadgeType`.
- **Grep de aceptacion (paso 3)**: `rg "Color\(0x|Color\.White|Color\.Black|Color\.Red|Color\.Gray"`
  en `feature/` -> 0 coincidencias (exit 1); `rg "0x"` en `feature/` -> 0 coincidencias.
- **Verificacion (salida real, exit 0)**:
  - `.\gradlew.bat :app:assembleDebug --no-daemon` -> BUILD SUCCESSFUL in 39s (4 ejecutados, 34 up-to-date).
  - `.\gradlew.bat :app:testDebugUnitTest :app:lintDebug --no-daemon` -> BUILD SUCCESSFUL in 57s;
    **13 suites / 100 tests / 0 failures / 0 errors** (el "85/85" del prompt esta desactualizado:
    la suite actual es 100 y verde) y lint **0 errors / 53 warnings** (baseline).
- **Aceptacion**: 0 literales de color en pantallas (comprobado con grep); suite verde
  (100/100 reales, no 85).
- **Pendiente**: pantallas de feature refactorizadas al sistema de componentes (home/search/
  followups/detail/diagnostics/pairing, fases 6-11). Detenido a la espera de instrucciones.

## FASE 6 - HOME FERROVIARIA MINIMALISTA (2026-09-22)

- **Objetivo cumplido**: Home rediseñada con `RenfeScreenScaffold`, CTAs de altura mínima
  56 dp, textos con `maxLines`/`overflow`; eliminado el botón "Recargar" no-op. Sin red nueva.
- **Archivos creados**:
  - `feature/home/HomeComponents.kt`: subcomponentes de la pantalla: `HomeHeader` (titulo
    `headlineMedium` 28 sp, `maxLines = 2`, Ellipsis, centrado), `HomePairedBadge`
    (`RenfeStatusBadge` tipo `BadgeType.SUCCESS` con `Icons.Filled.CheckCircle` y
    `home_paired`), `HomeMeta` (hora + version en `bodySmall`/`onSurfaceVariant`, máx. 1 línea),
    `HomeNotificationNotice` (aviso de permisos con `OutlinedButton` a ajustes). `HOUR_FORMAT`
    movido aquí desde `HomeScreen.kt`.
  - `src/debug/.../feature/home/HomePreviews.kt`: 4 `@Preview` 360x800, claro/oscuro y
    fontScale 1/2, spec 1080x2400/480, `showSystemUi = true` en las dos primeras; variantes
    `isPaired` (emparejado con badges; no emparejado con aviso de permisos).
  - `src/androidTest/.../feature/home/HomeContentTest.kt`: 6 tests Compose
    (`createAndroidComposeRule<ComponentActivity>()`): cada CTA (Vincular dispositivo, Buscar
    trenes, Mis seguimientos, Ajustes) invoca su callback una sola vez; no existe botón
    "Recargar"; emparejado muestra "Dispositivo vinculado".
- **Archivos modificados**:
  - `feature/home/HomeScreen.kt`: `HomeContent` usa `RenfeScreenScaffold` (78) con
    `onBack = null` y action "Ajustes" (`TextButton`, linea 82-84, -> `onNavigateToDiagnostics`).
    Columna `verticalScroll` (90) y `padding(horizontal = RenfeSpacing.screenMargin)`; al no
    emparejar: `HomeHeader` (home_not_paired_title) + `Button` "Vincular dispositivo"
    (`heightIn(min = 56.dp)`, 111); emparejado: `HomeHeader(home_subtitle)` + `HomePairedBadge` (104).
    `HomeMeta` (117) sustituye el bloque loading/clock (el spinner inline desaparece: `now == null`
    mientras carga y `HomeMeta` omite la hora; sin cambio funcional). CTA principal
    `Button` "Buscar trenes" (123) y secundaria `OutlinedButton` "Mis seguimientos" (132), ambos
    `heightIn(min = 56.dp)`. Eliminada la firma `onRefresh` y el `TopAppBar`/`Scaffold` propios,
    y con ellos `CircularProgressIndicator`, `semantics`, `@OptIn(ExperimentalMaterial3Api)`.
  - `res/values/strings.xml`: `home_not_paired_cta` -> "Vincular dispositivo", `home_search_button`
    -> "Buscar trenes", nueva `home_paired` "Dispositivo vinculado" y `home_settings` "Ajustes";
    eliminadas las ahora sin uso `home_refresh`, `cd_refresh`, `home_diagnostics_button`,
    `home_status_pending` (no aparecen como UnusedResources en lint).
- **Riesgo 0/5 resuelto**: el botón "Recargar" no-op (`HomeScreen.kt` onRefresh = {}) ya no
  existe; documentado aquí como cierre del punto 5 de RIESGOS PARA 360dp (fase 0).
- **Verificacion (salida real, exit 0)**:
  - `.\gradlew.bat :app:assembleDebug --no-daemon` -> 1er intento FAILED (faltaba
    `import androidx.compose.ui.unit.dp` en `HomeComponents.kt:62,89`); corregido -> BUILD
    SUCCESSFUL in 54s (6 ejecutados, 32 up-to-date).
  - `.\gradlew.bat :app:testDebugUnitTest :app:lintDebug :app:assembleDebugAndroidTest --no-daemon` ->
    1er intento FAILED en `compileDebugAndroidTestKotlin` (import `assertExists`
    inexistente en esta version); sustituido por `assertIsDisplayed` -> BUILD SUCCESSFUL in 38s.
  - Re-ejecucion forzada del test de unidad sobre el codigo final:
    `.\gradlew.bat :app:testDebugUnitTest --no-daemon --rerun-tasks` -> BUILD SUCCESSFUL in 1m1s;
    reporte XML **13 suites / 100 tests / 0 failures / 0 errors**; lint **0 errors / 53 warnings**
    (baseline; las strings retiradas no generan UnusedResources).
- **Aceptacion**: boton no-op eliminado y sin referencia residual (`rg` de las strings -> nada
  en codigo); los 4 callbacks operativos (6 tests instrumentales compilados, pendientes de
  ejecucion en dispositivo).
- **Nota honesta**: `HomeContentTest` compilado con `assembleDebugAndroidTest` pero NO ejecutado
  en emulador/dispositivo (misma politica que fases 3-4).
- **Pendiente**: pantalla de busqueda (fase 7). Detenido a la espera de instrucciones.

## FASE 7 - SEARCH REDISENADA (2026-09-22)

- **Objetivo cumplido**: campos (singleLine, 56 dp min), sugerencias EN FLUJO sin popup, CTA
  Buscar fullWidth a 56 dp deshabilitada mientras busca, 4 modos en FlowRow, tarjeta por tren
  con badge de disponibilidad y precio null → "Precio no disponible". Scaffold unificado.
- **Archivos creados**:
  - `feature/search/SearchComponents.kt`: `SearchStationField` (54) — sugerencias EN FLUJO bajo
    el campo en `Surface`, contenedor `heightIn(max = 192.dp)` (77) y filas `heightIn(min =
    48.dp)` (87) + `maxLines = 2`/Ellipsis, SIN `verticalScroll` anidado dentro del LazyColumn
    de la pantalla; `SearchTrainCard` (117) — hora salida/llegada, badge `RenfeStatusBadge` de
    disponibilidad, identificación del tren, precio; `SearchFollowUpModes` (185) — `FlowRow`
    (190) con los 4 chips FIRST/LAST/ALL/SPECIFIC; `SearchDatePickerDialog` (220) y
    `SearchFollowUpCreator` (254) — CTA `enabled = !isCreatingFollowUp` y diálogos conservados.
  - `src/debug/.../feature/search/SearchPreviews.kt`: 4 previews 360×800, claro/oscuro ×
    fontScale 1.0/2.0 (2 con formulario + showSystemUi, 2 con resultados y fuente 2×), spec
    1080x2400/480; un tren de ejemplo con precio y otro con `price = null`.
- **Archivos modificados**:
  - `feature/search/SearchScreen.kt`: `SearchScreen` usa `RenfeScreenScaffold` (63) en vez de
    Scaffold/TopAppBar propios; `SearchContent` con margen horizontal
    `RenfeSpacing.screenMargin` (16 dp) y separación 12 dp; campos `heightIn(min = 56.dp)`;
    resultados con `items(key = { it.identity })` (220) (antes `"identity#index"`); carga →
    `RenfeLoadingState` (189) y error → `RenfeErrorState` con `onRetry = onSearch` (198) en
    caja acotada (240 dp); vacío → texto `search_no_trains`.
  - `res/values/strings.xml`: `search_price_unknown` → "Precio no disponible"; nuevas
    `search_train_type` ("Tren %1$s"), `search_mode_specific` ("Un tren concreto"),
    `search_error_title`; eliminada `cd_search_back` (el `RenfeScreenScaffold` usa
    `common_back`; sin UnusedResources).
- **Decision**: el prompt pedía "tipo" en la tarjeta; `TrainOut` (ApiModels.kt:47-54) no expone
  tipo de tren — NO se inventa API. Se muestra el `identity` como identificación del tren
  ("Tren %1$s") y el "tipo" de disponibilidad queda cubierto por el badge.
- **Verificacion (salida real, exit 0)**:
  - `.\gradlew.bat :app:assembleDebug --no-daemon` -> 1er intento FAILED (faltaban imports de
    `Row`/`Column` en `SearchScreen.kt`) -> corregido solo ese punto -> BUILD SUCCESSFUL in 43s.
  - `.\gradlew.bat :app:testDebugUnitTest :app:lintDebug --no-daemon` -> BUILD SUCCESSFUL in 59s;
    reporte XML **13 suites / 100 tests / 0 failures / 0 errors**; lint **0 errors / 53 warnings**
    (baseline, sin warnings nuevos por strings retiradas/añadidas).
- **Aceptación**: sin popups (sugerencias en flujo); 4 modos accesibles en 360 dp a 2× (FlowRow
  envuelve, no recorta); `price = null` → "Precio no disponible" y grep de "0 €" sin resultados
  en UI (solo aparece en un comentario KDoc).
- **Pendiente**: validación visual en Realme/emulador (FlowRow y tarjetas); Search ViewModel
  intacto (validación/fecha sin cambios). Detenido a la espera de instrucciones.