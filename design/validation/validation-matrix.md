# Matriz de validacion del rediseno UI (fase 13)

> Se aplica el DEVICE_PROFILE real: 1080x2400 px @ 480 dpi = 360x800 dp (DESIGN.md).
> Regla de honestidad (recordatorio + AGENTS.md): una fila solo se marca `VALIDADO`
> con evidencia real; sin dispositivos se marca `PENDIENTE`. Nunca se inventa.

Estado general: **PENDIENTE de validacion en dispositivo** (no habia dispositivo disponible;
`adb devices` mostro `192.168.1.200:5555` offline y sin serial USB el 2026-09-22).

## 1. Matriz de modos (app x sistema)

Modo de tema de la app (`ThemeMode`): SYSTEM, LIGHT, DARK. Modo del sistema: claro u oscuro.
Resultado esperado del esquema (definido en `Theme.kt`):

| App | Sistema | Esquema renderizado | Estado |
|---|---|---|---|
| SYSTEM | oscuro | oscuro (DarkColors) | PENDIENTE (device) |
| SYSTEM | claro | claro (LightColors) | PENDIENTE (device) |
| LIGHT | oscuro | claro (LightColors) | PENDIENTE (device) |
| LIGHT | claro | claro (LightColors) | PENDIENTE (device) |
| DARK | oscuro | oscuro (DarkColors) | PENDIENTE (device) |
| DARK | claro | oscuro (DarkColors) | PENDIENTE (device) |

Evidencia local ya cubierta (sin dispositivo):
- Logica del modo: `ThemeModeTest` (JVM) + `ThemeViewModelTest` (JVM) verdes (4 + 7 tests).
- Selector de tema: `ThemeModeSelectorTest` (instrumental, compilado, no ejecutado).
- Custom XML: los 36 roles se definen en claro y oscuro (`ui/theme/Color.kt`), y la resolucion
  `system`/`light`/`dark` esta en `ThemeViewModel` + `PreferencesRepository` (clave "theme").

## 2. Persistencia

| Verificacion | Estado |
|---|---|
| Clave "theme" (DataStore `app_preferences`) persiste y recarga | PENDIENTE (device) |
| `PreferencesRepository.kt:45,72-75` valores system/light/dark | PROBADO LOCAL (ThemeModeTest) |
| Sin claves nuevas introducidas por el rediseno | COMPROBADO (auditoria fase 13: solo UI) |

## 3. Fuentes (font scale)

| Verificacion | Estado |
|---|---|
| Previews: spec 1080x2400/480, 360x800 dp, claro/oscuro | COMPROBADO (los 9 ficheros de preview usan `spec:width=1080px,height=2400px,dpi=480`) |
| Previews fontScale 1.0 y 2.0 | COMPROBADO (1.0 implicito + `fontScale = 2f` en 2 previews por fichero) |
| Previews fontScale 1.3 | PENDIENTE (no existe en los previews actuales) |
| font_scale 1.0 y 2.0 en dispositivo (Realme) | PENDIENTE (sin dispositivo) |

## 4. Alcance del rediseno (git audit)

`git diff BASELINE_SHA..HEAD` (BASELINE_SHA = `f5943c33bdd63207761e7266bed58e293e208687`):
- Backend (`backend/`), `core/*`, `RenfeNotifierApp.kt`, `AndroidManifest.xml`, `.github/workflows/`
  y los 6 ViewModels de feature: **0 cambios** (comando de auditoria, salida vacia).
- Solo cambiaron: UI (`feature/*`, `navigation`, `ui/theme`, `ui/components`), `MainActivity.kt`,
  `strings.xml`, tests/previews y 2 ficheros de infra de test (`build.gradle.kts`,
  `libs.versions.toml` para dependencias de Compose UI test).

## 5. Auditoria estatica (fase 13)

| Regla | Resultado |
|---|---|
| `rg -P "import androidx\.compose\.material\.(?!icons)"` | 0 matches (solo material3 e icons) |
| `rg "Color\(0x"` fuera de `ui/theme` | 0 matches |
| `rg "dynamicColor"` | 0 matches |

## 6. Flujos con fakes (sin OTP ni Renfe real)

`RedesignRegressionTest` (androidTest): 6 tests que componen cada `Content` publico con estados
falsos y callbacks vacios (Home, Search, FollowUps, Detalle, Diagnostics, Pairing). Compilado;
ejecucion en dispositivo PENDIENTE.

## 7. Comandos finales

| Comando | Resultado |
|---|---|
| `:app:assembleDebug` | BUILD SUCCESSFUL (exit 0) |
| `:app:testDebugUnitTest` | BUILD SUCCESSFUL (13 suites / 100 tests / 0 failures / 0 errors) |
| `:app:lintDebug` | BUILD SUCCESSFUL (0 errors / 53 warnings) |
| `:app:assembleDebugAndroidTest` | BUILD SUCCESSFUL (exit 0; compila `RedesignRegressionTest`) |
| `:app:connectedDebugAndroidTest` | PENDIENTE (sin dispositivo) |