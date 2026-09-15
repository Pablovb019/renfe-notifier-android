"""Limitación de peticiones en memoria, sin servicios externos de pago."""

import threading
import time


class InMemoryRateLimiter:
    """Ventana deslizante por clave (IP) con estado en memoria.

    No es distribuido ni persistente: sirve para una VM de un único proceso y
    un único usuario, donde un reinicio solo resetea los contadores.
    """

    def __init__(self, *, limit: int, window_s: float) -> None:
        self._limit = limit
        self._window_s = window_s
        self._hits: dict[str, list[float]] = {}
        self._lock = threading.Lock()

    def allowed(self, key: str, now: float | None = None) -> bool:
        """Consume una petición para ``key``; False si excede el límite."""
        stamp = now if now is not None else time.monotonic()
        cutoff = stamp - self._window_s
        with self._lock:
            recent = [t for t in self._hits.get(key, []) if t > cutoff]
            if len(recent) >= self._limit:
                self._hits[key] = recent
                return False
            recent.append(stamp)
            self._hits[key] = recent
            return True
