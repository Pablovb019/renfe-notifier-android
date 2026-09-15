# Auditar consultas, tareas y persistencia

Continúa sobre el mismo commit de la auditoría anterior.

Revisa renfechecker.py, renfebot.py y dbmanager.py.
Comprueba:
- Inicio de Firefox/Xvfb y fallback HTTP/DWR/Selenium.
- Función de los cinco POST, incluidos los dos generateId.
- Aislamiento y cierre de sesiones HTTP.
- Procesamiento secuencial, notifying y recordatorios.
- Identificación de trenes.
- Fechas, zonas horarias y cierre de SQLite.
- Gestión de excepciones.

Revisa dependencias, Docker y workflows originales.
Añade a docs/audit.md qué conservar, adaptar o eliminar y por qué.
No atribuyas problemas al código sin verificarlos.
No hagas consultas reales a Renfe.