"""Lógica pura de agrupación de seguimientos y mapeo de observaciones.

Sin dependencias de la red, el reloj de sistema ni la persistencia: solo
transforma el estado del dominio. Esto permite reutilizar ``build_plan`` y
``train_list_to_observation`` tanto desde el planificador como desde una
búsqueda manual equivalente.
"""

from collections import defaultdict
from dataclasses import dataclass
from datetime import date, datetime

from app.followups.domain import FollowUp, Observation, ObservationKind, TrainSnapshot
from app.renfe.parser import TrainList


@dataclass(frozen=True, slots=True)
class GroupKey:
    """Parámetros que afectan la respuesta de Renfe para una consulta."""

    origin_code: str
    destination_code: str
    travel_date: date
    plaza_h: bool


def build_plan(active_followups: list[FollowUp]) -> list[tuple[GroupKey, list[FollowUp]]]:
    """Agrupa seguimientos activos por la clave de consulta equivalente.

    Devuelve exactamente una entrada por ``GroupKey`` válido, garantizando que
    ningún grupo se comprueba dos veces en el mismo ciclo y que una sola
    búsqueda sirve a todos los seguimientos compatibles del grupo.
    """
    groups: dict[GroupKey, list[FollowUp]] = defaultdict(list)
    for followup in active_followups:
        key = GroupKey(
            origin_code=followup.origin_code,
            destination_code=followup.destination_code,
            travel_date=followup.travel_date,
            plaza_h=followup.plaza_h,
        )
        groups[key].append(followup)
    return list(groups.items())


def train_list_to_observation(trains: TrainList, observed_at: datetime) -> Observation:
    """Convierte un listado DWR parseado en una observación válida del dominio.

    ``ObservationKind.STALE`` queda reservado para cuando exista una señal fiable
    de datos desactualizados; hoy no existe y todo listado válido es ``VALID``.
    """
    snapshots = tuple(
        TrainSnapshot(
            departure=train.departure,
            arrival=train.arrival,
            availability=train.availability,
            real_id=train.identifier,
        )
        for train in trains.trains
    )
    return Observation(ObservationKind.VALID, observed_at, snapshots)


def error_observation(observed_at: datetime) -> Observation:
    """Observación de error cuando el cliente falla de forma irrecuperable."""
    return Observation(ObservationKind.ERROR, observed_at)
