"""Contadores operativos en memoria para el panel de diagnóstico.

Acumulan las cifras del proceso lanzador (el backend corre una sola
instancia): "consultas lógicas" (grupos de seguimientos comprobados por el
planificador) y "peticiones HTTP" a Renfe (suma de peticiones de las búsquedas
exitosas). Se reinician con el proceso, así que siempre significan "desde el
último arranque".
"""

import threading
from dataclasses import dataclass


@dataclass(frozen=True, slots=True)
class ProcessSearchStats:
    logical_queries: int
    http_requests: int
    bytes_received: int


class Monitoring:
    """Acumuladores seguros para hilos y corrutinas del mismo proceso."""

    def __init__(self) -> None:
        self._lock = threading.Lock()
        self._logical_queries = 0
        self._http_requests = 0
        self._bytes_received = 0

    def record_logical_query(self) -> None:
        with self._lock:
            self._logical_queries += 1

    def record_search(self, *, http_requests: int, bytes_received: int) -> None:
        with self._lock:
            self._http_requests += http_requests
            self._bytes_received += bytes_received

    def snapshot(self) -> ProcessSearchStats:
        with self._lock:
            return ProcessSearchStats(
                logical_queries=self._logical_queries,
                http_requests=self._http_requests,
                bytes_received=self._bytes_received,
            )


monitoring = Monitoring()
