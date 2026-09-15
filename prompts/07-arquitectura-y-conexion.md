# Proponer arquitectura mínima

Usa la auditoría y el análisis de coste.

Propón:
- Android Kotlin/Compose, un módulo organizado por funcionalidades.
- Backend Python modular y ligero.
- SQLite persistente con migraciones.
- Planificador con un único propietario.
- FCM y emparejamiento de un único usuario.
- Conexión cifrada Android/backend.
- Acceso administrativo separado.

Justifica FastAPI o la alternativa.
Define exposición de red, TLS, autenticación y recuperación de acceso.
No asumas IP gratuita, dominio existente ni túnel permanente.

Describe límites de recursos y riesgos para 1 GB de RAM.
Guarda docs/architecture.md y docs/security.md.
Pregunta únicamente decisiones bloqueantes, agrupadas.
No implementes.