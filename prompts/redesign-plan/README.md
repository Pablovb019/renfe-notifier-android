# Plan de rediseÃ±o â€“ Renfe Notifier

Pack de prompts para ejecutar el rediseÃ±o visual con un agente de codificaciÃ³n (Antigravity / MiniMax M3).

## Estructura

- `00-recordatorio-arranque.md` â€” texto que se pega ANTES de cada fase.
- `01-fase-0-auditoria.md` â€” auditorÃ­a inicial del repo y del dispositivo.
- `02-fase-1-color-type-shape-spacing.md`
- `03-fase-2-theme-mode-viewmodel.md`
- `04-fase-3-selector-contrast.md`
- `05-fase-4-componentes-base.md`
- `06-fase-5-migracion-colores.md`
- `07-fase-6-home.md`
- `08-fase-7-search.md`
- `09-fase-8-followups.md`
- `10-fase-9-detail.md`
- `11-fase-10-diagnostics.md`
- `12-fase-11-pairing.md`
- `13-fase-12-motion.md`
- `14-fase-13-validacion.md`
- `99-checklist-final.md`

## CÃ³mo usar

1. Un chat nuevo por fase.
2. Pega el contenido de `00-recordatorio-arranque.md`.
3. Pega debajo el contenido de la fase correspondiente.
4. Verifica criterios de aceptaciÃ³n antes de pasar a la siguiente.
5. Estado real vive en `DESIGN.md` y `PROGRESS.md` del repo.

## Reglas

- No modificar esta carpeta.
- DEVICE_PROFILE: 1080x2400 px, 480 dpi, 360x800 dp.
- Entorno Windows: usar `.\gradlew.bat`.
