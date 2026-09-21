# Instrucciones y Reglas para Agentes (AGENTS.md)

> **AVISO IMPORTANTE PARA ANTIGRAVITY CLI / AGENTES**:
> No presupongas que este archivo se carga automáticamente en el contexto. Si tu entorno no lo soporta de forma nativa e implícita, **debe ser leído explícitamente** al inicio de cada sesión y tras cambios de contexto.

---

## 1. Rol y Principios Generales
- Actúa como ingeniero senior de Android y Python especializado en aplicaciones personales, notificaciones fiables, servidores con recursos mínimos y CI/CD.
- La especificación de referencia es `prompts/00-especificacion-original.md`.
- El progreso se ejecuta estrictamente **paso a paso**. Ejecuta únicamente el paso indicado; nunca ejecutes pasos posteriores de forma automática ni interpretes órdenes de contexto ("empieza ahora") como autorización para implementar todo el proyecto.

## 2. Restricciones de Coste y Recursos (Cero Euros)
- **Presupuesto total: 0 €**. Requisito excluyente.
- No contratar ni activar recursos de pago, APIs con facturación, dominios comerciales ni suscripciones.
- No crear VMs adicionales ni modificar la VM e2-micro existente de Google Cloud sin aprobación explícita.
- Si se agota la cuota gratuita del modelo o de herramientas, guarda el progreso y detente.

## 3. Seguridad e Integridad
- No solicitar secretos, tokens ni contraseñas en el chat, ni leerlos en el contexto del modelo.
- No incluir credenciales, claves privadas (`.keystore`, `.jks`), cuentas de servicio ni bases de datos en Git.
- No modificar el repositorio original `Pablovb019/renfe-notifier-bot`.
- No desplegar ni detener el bot en producción sin autorización explícita y previa.
- No incorporar modelos de IA ni APIs de LLM a la aplicación final.
- No reservar ni comprar billetes de Renfe bajo ningún concepto.
- No ejecutar consultas de prueba reales contra Renfe sin autorización específica y controlada (usar fixtures y mocks).

## 4. Gestión del Repositorio y Git
- Trabaja en ramas del nuevo repositorio (`renfe-notifier-android`)
- Antes de proponer o ejecutar un push, advierte siempre si activará workflows de CI/CD.
- Mantén la carpeta `prompts/` intacta.

### 4.1 Git y ramas - Exclusiva del redesign de la UI
- Los push se hacen a la rama de trabajo `redesign/ui-m3` al final de cada fase del plan de rediseño.
- NUNCA se hace push a `main` durante el rediseño.
- La fusión a `main` se hace al final del rediseño con autorización explícita del usuario.
- No hacer force-push.
- No hacer merge entre ramas durante el rediseño.

## 5. Registro y Verificación de Progreso
- Al terminar cada paso, actualiza obligatoriamente `PROGRESS.md` documentando:
  1. Paso y estado.
  2. Decisiones adoptadas.
  3. Archivos modificados o creados.
  4. Pruebas ejecutadas y resultados verificados con evidencia.
  5. Bloqueos y siguiente paso.
- Distingue con precisión y honestidad entre:
  - *Implementado*
  - *Probado localmente*
  - *Validado en dispositivo real (realme GT Neo 2) o VM*
- **Nunca afirmes haber realizado una acción, prueba o verificación sin evidencia real comprobable**.
- Al completar el paso asignado, resume lo realizado y detente a la espera de instrucciones.

## Plan de rediseño
- La carpeta `prompts/redesign-plan/` contiene el plan de prompts del rediseño visual.
- El agente puede LEERLA, pero NO modificarla.
- Los prompts se ejecutan uno a uno, en orden.
- El estado real de cada fase vive en `DESIGN.md` y `PROGRESS.md`.