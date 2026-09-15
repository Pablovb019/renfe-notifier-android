# Implementar cliente HTTP/DWR

Implementa el flujo existente comprobado.
Conserva los cinco POST mientras no exista evidencia para cambiarlos.

Incluye:
- Timeouts por petición y presupuesto total.
- Cierre de sesiones ante éxito y error.
- Aislamiento del estado de búsqueda.
- Backoff con jitter.
- Respeto a Retry-After.
- Tratamiento de 403 y 429.
- Métricas de peticiones, duración y bytes observables.

No evadas bloqueos ni CAPTCHA.
No inicies Selenium.
Prueba con transporte simulado, sin red real.