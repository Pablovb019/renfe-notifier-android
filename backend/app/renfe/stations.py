"""Catálogo local de estaciones y búsqueda tolerante a tildes."""

import json
import re
import unicodedata
from dataclasses import dataclass
from functools import cached_property
from pathlib import Path
from typing import Any


def normalize_station_key(value: str) -> str:
    """Normaliza texto como el algoritmo NFKD auditado del bot original."""
    normalized = unicodedata.normalize("NFKD", value)
    without_marks = "".join(char for char in normalized if not unicodedata.combining(char))
    return re.sub(r"[^A-Z0-9]", "", without_marks.upper())


@dataclass(frozen=True, slots=True)
class Station:
    name: str
    code: str
    priority: int
    administration_code: str | None
    uic_code: str | None

    @property
    def is_group(self) -> bool:
        return self.name.endswith("(TODAS)")


class StationCatalog:
    """Carga el catálogo versionado localmente; no realiza acceso de red."""

    # Alias de entrada comunes, resueltos siempre contra nombres existentes.
    aliases = {
        "ATOCHA": "MADRID PTA. ATOCHA - ALMUDENA GRANDES",
        "CHAMARTIN": "MADRID-CHAMARTIN-CLARA CAMPOAMOR",
        "SANTS": "BARCELONA-SANTS",
    }

    def __init__(self, path: Path | None = None) -> None:
        self._path = path or Path(__file__).parent / "data" / "stations.json"

    @cached_property
    def stations(self) -> tuple[Station, ...]:
        raw = json.loads(self._path.read_text(encoding="utf-8"))
        if not isinstance(raw, dict):
            raise ValueError("El catálogo de estaciones debe ser un objeto JSON")

        stations = []
        for name, metadata in raw.items():
            if not isinstance(name, str) or not isinstance(metadata, dict):
                continue
            code = metadata.get("cdgoEstacion")
            if not isinstance(code, str) or not code:
                continue
            priority = metadata.get("nmroPrioridad", 0)
            stations.append(
                Station(
                    name=name,
                    code=code,
                    priority=priority if isinstance(priority, int) else 0,
                    administration_code=self._optional_string(metadata, "cdgoAdmon"),
                    uic_code=self._optional_string(metadata, "cdgoUic"),
                )
            )
        return tuple(stations)

    @staticmethod
    def _optional_string(metadata: dict[str, Any], key: str) -> str | None:
        value = metadata.get(key)
        return value if isinstance(value, str) else None

    @cached_property
    def _by_key(self) -> dict[str, Station]:
        return {normalize_station_key(station.name): station for station in self.stations}

    @cached_property
    def _by_code(self) -> dict[str, Station]:
        return {station.code: station for station in self.stations}

    def by_code(self, code: str) -> Station | None:
        """Resuelve un código de estación exacto (ej. '00001')."""
        return self._by_code.get(code)

    def resolve(self, query: str) -> Station | None:
        """Resuelve un nombre oficial o alias sin distinguir tildes ni signos."""
        key = normalize_station_key(query)
        alias = self.aliases.get(key)
        return self._by_key.get(normalize_station_key(alias)) if alias else self._by_key.get(key)

    def group_candidates(self, group: Station) -> tuple[Station, ...]:
        """Devuelve estaciones concretas de una agrupación ``CIUDAD (TODAS)``."""
        if not group.is_group:
            return ()
        city = group.name.removesuffix("(TODAS)").strip()
        city_key = normalize_station_key(city)
        return tuple(
            sorted(
                (
                    station
                    for station in self.stations
                    if not station.is_group and city_key in normalize_station_key(station.name)
                ),
                key=lambda station: (station.priority, station.name),
            )
        )
