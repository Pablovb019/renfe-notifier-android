"""Planificador de comprobaciones de seguimientos.

Un único propietario (``SchedulerService``) agrupa seguimientos activos por
los parámetros que afectan la respuesta de Renfe y ejecuta una búsqueda por
grupo. La lógica lógica de agrupación y mapeo vive en ``plan.py``, y el ciclo
asíncrono con persistencia en ``service.py``.
"""
