Lee AGENTS.md completo. Respeta: coste cero, una fase por ejecución, no tocar prompts/, sin producción.

Lee DESIGN.md si existe (DEVICE_PROFILE: 1080x2400 px, 480 dpi, 360x800 dp).
Lee las últimas 30 líneas de PROGRESS.md si existe.

REGLAS DE EJECUCIÓN:
- Comandos de verificación UNO a UNO, mostrando salida real y código de salida.
- Entorno Windows: .\gradlew.bat desde android/.
- Si un comando falla, corrige solo ese punto.
- Si no puedes ejecutar algo: PENDIENTE o BLOQUEADO. Nunca superado.
- No inventes rutas, versiones, colores ni APIs.
- Al explicar cambios, cita rutas y líneas concretas; no reimprimas archivos completos.

AL TERMINAR LA FASE:
- Verifica que .\gradlew.bat :app:assembleDebug y :app:testDebugUnitTest pasan.
- Si algún comando falla, NO hagas commit. Corrige o marca BLOQUEADO en DESIGN.md.
- Actualiza DESIGN.md y PROGRESS.md con lo realmente ejecutado.
- Distingue: implementado / probado local / validado emulador / validado Realme / pendiente.
- Crea un commit en la rama de trabajo `redesign/ui-m3` con mensaje: "fase N: <descripción breve>".
- Haz push de esa rama: `git push origin redesign/ui-m3`.
- NUNCA hagas push a `main` ni force-push.
- DETENTE. No ejecutes la siguiente fase sin autorización.

---