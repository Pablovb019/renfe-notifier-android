# Persistir el dominio

Implementa SQLite según la arquitectura aprobada:

- Migraciones versionadas.
- Repositorios y SQL parametrizado.
- Índices, busy_timeout y WAL cuando corresponda.
- Transacciones cortas.
- Cierre correcto de conexiones.
- Datos fuera de directorios reemplazables.
- Sin transacciones abiertas durante consultas de red.

Persiste seguimientos, episodios y configuración necesaria.
No importes seguimientos del bot.

Prueba creación, migración, reinicio y recuperación con bases temporales.
Incluye un mecanismo de backup consistente, no una copia ciega del .db.