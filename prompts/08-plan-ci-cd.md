# Diseñar CI/CD

Diseña y documenta, sin habilitar workflows:

- CI Linux para backend y Android.
- Filtros por cambios sin checks obligatorios eternamente pendientes.
- Sin secretos, Renfe ni producción en pruebas.
- Acciones fijadas por SHA y permisos mínimos.
- Cachés, retención y cancelación de CI obsoleta.
- CD manual, serializado y solo para commits autorizados de main con CI superada.
- Verificación de checks del SHA exacto.
- Acceso seguro a la VM; OIDC no implica conectividad SSH.
- Backups SQLite, migraciones, health check y rollback.
- APK debug y distribución firmada privada.
- Alternativa manual desde Windows si se agota la cuota.

No dependas de aprobaciones de Environments sin verificar su disponibilidad.
Guarda docs/ci-cd.md.