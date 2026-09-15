# Medir recursos y decidir sobre Selenium

Prepara y ejecuta mediciones locales reproducibles:
- CPU, RAM, disco y duración.
- 1 y 5 seguimientos.
- Con y sin agrupación.
- Peticiones y bytes observables.

Las simulaciones no acreditan consumo real en la VM.
No conviertas bytes de aplicación directamente en tráfico facturado.

Evalúa necesidad del fallback Selenium a partir de la auditoría.
Si sigue siendo necesario, prepara implementación opcional:
bajo demanda, serializada, cierre por inactividad y recursos acotados.
No lo incluyas en el despliegue habitual si HTTP basta.

Documenta qué mediciones reales en e2-micro faltan.
Si no cabe en 1 GB, explica el bloqueo; no amplíes la VM.