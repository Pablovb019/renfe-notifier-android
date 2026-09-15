"""Cola persistente de avisos y recordatorios.

Los avisos se asocian a episodios concretos de disponibilidad y se entregan
con reintentos limitados e idempotencia. La persistencia vive en SQLite, por
lo que el estado sobrevive a reinicios y no requiere Redis ni Celery.
"""
