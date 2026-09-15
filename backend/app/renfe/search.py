"""Motor de búsqueda de trenes con validación estricta de estaciones.

Resuelve códigos de estación contra el catálogo local (anticorrupción):
nunca se aceptan URLs arbitrarias del cliente ni códigos desconocidos, lo que
cumple la regla anti-SSRF documentada en ``docs/security.md``.
"""

from datetime import date

from app.renfe.client import RenfeDwrClient, SearchResult
from app.renfe.stations import StationCatalog


class StationNotFoundError(ValueError):
    """Un código de estación no existe en el catálogo local."""

    def __init__(self, role: str) -> None:
        self.role = role
        super().__init__(f"Estación de {role} desconocida")


class TrainSearchEngine:
    """Resuelve estaciones del catálogo y delega en el cliente DWR."""

    def __init__(self, client: RenfeDwrClient, catalog: StationCatalog) -> None:
        self._client = client
        self._catalog = catalog

    async def search(
        self,
        *,
        origin_code: str,
        destination_code: str,
        travel_date: date,
        plaza_h: bool,
    ) -> SearchResult:
        origin = self._catalog.by_code(origin_code)
        if origin is None:
            raise StationNotFoundError("origen")
        destination = self._catalog.by_code(destination_code)
        if destination is None:
            raise StationNotFoundError("destino")
        return await self._client.search(
            origin=origin,
            destination=destination,
            travel_date=travel_date,
            plaza_h=plaza_h,
        )
