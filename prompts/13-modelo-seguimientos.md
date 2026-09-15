# Implementar reglas de seguimiento

Implementa lógica de dominio sin depender todavía del planificador.

Separa ciclo de vida, disponibilidad y aviso por episodio.
Define:
- Nueva aparición válida de plazas.
- Errores y datos obsoletos sin episodios nuevos.
- Confirmación sin borrar el seguimiento.
- Pausa, eliminación y renovación.
- Caducidad inicial de 30 días limitada por la salida.
- Comportamiento de primero, último y todos.
- Nuevos trenes dentro de "todos".
- Final del día y llegadas al día siguiente.
- ID real y fallback documentado.

Usa UTC para instantes y Europe/Madrid para fechas de viaje.
Añade tests de transiciones, medianoche y cambios de hora.