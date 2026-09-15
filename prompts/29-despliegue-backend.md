# Preparar CD sin ejecutarlo

Crea scripts y workflow según el diseño aprobado:

- workflow_dispatch.
- Solo SHA autorizado de main con checks necesarios superados.
- Rechazar entradas que permitan otras ramas o comandos inyectados.
- Autenticación y conectividad verificables por separado.
- Confianza OIDC restringida si se eligió ese mecanismo.
- Despliegues serializados sin cancelación durante migraciones.
- Backup, actualización, health check y rollback.
- Secretos fuera de paquetes e imágenes.
- Sin servicios o registros de pago adicionales.

Prueba controles y fallos con dobles o entorno local.
NO despliegues, configures la VM ni retires el bot.