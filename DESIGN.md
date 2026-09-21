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