"""Parser sin red de respuestas DWR para listados de trenes."""

import re
from dataclasses import dataclass
from datetime import time
from decimal import Decimal, InvalidOperation
from enum import StrEnum
from typing import Any, cast

import json5


class DwrParseError(ValueError):
    """La respuesta no tiene la estructura DWR comprobada para un listado."""


class Availability(StrEnum):
    AVAILABLE = "available"
    NO_AVAILABILITY = "no_availability"
    UNKNOWN = "unknown"


class ParseStatus(StrEnum):
    OK = "ok"
    NO_TRAINS = "no_trains"
    NO_AVAILABILITY = "no_availability"


@dataclass(frozen=True, slots=True)
class Train:
    identifier: str
    departure: time | None
    arrival: time | None
    price: Decimal | None
    availability: Availability


@dataclass(frozen=True, slots=True)
class TrainList:
    status: ParseStatus
    plaza_h_requested: bool
    trains: tuple[Train, ...]


_CALLBACK = re.compile(
    r"(?:dwr\.engine\.remote\.|r\.)handleCallback\s*\(\s*['\"]\d+['\"]\s*,\s*['\"]\d+['\"]\s*,\s*"
)


def parse_train_list(response_text: str, *, plaza_h_requested: bool) -> TrainList:
    """Convierte un callback DWR con ``listadoTrenes`` en un resultado tipado.

    ``plaza_h_requested`` se conserva como contexto de la consulta. Este parser
    no interpreta Plaza H como una regla de disponibilidad: esa semántica no se
    ha verificado y corresponde al cliente HTTP posterior.
    """
    payload = _extract_callback_payload(response_text)
    decoded = json5.loads(payload)
    rows = _find_train_rows(decoded)
    trains = tuple(_parse_train(row) for row in rows)

    if not trains:
        status = ParseStatus.NO_TRAINS
    elif all(train.availability is Availability.NO_AVAILABILITY for train in trains):
        status = ParseStatus.NO_AVAILABILITY
    else:
        status = ParseStatus.OK
    return TrainList(status=status, plaza_h_requested=plaza_h_requested, trains=trains)


def _extract_callback_payload(response_text: str) -> str:
    match = _CALLBACK.search(response_text)
    if match is None:
        raise DwrParseError("No se encontró callback DWR")
    start = match.end()
    while start < len(response_text) and response_text[start].isspace():
        start += 1
    if start >= len(response_text) or response_text[start] not in "[{":
        raise DwrParseError("El callback DWR no contiene un objeto o lista")

    opening = response_text[start]
    closing = "}" if opening == "{" else "]"
    depth = 0
    quote: str | None = None
    escaped = False
    for index in range(start, len(response_text)):
        char = response_text[index]
        if quote:
            if escaped:
                escaped = False
            elif char == "\\":
                escaped = True
            elif char == quote:
                quote = None
            continue
        if char in "'\"":
            quote = char
        elif char == opening:
            depth += 1
        elif char == closing:
            depth -= 1
            if depth == 0:
                return response_text[start : index + 1]
    raise DwrParseError("El payload DWR está incompleto")


def _find_train_rows(decoded: Any) -> list[dict[str, Any]]:
    rows: Any
    if isinstance(decoded, list):
        rows = decoded
    elif isinstance(decoded, dict):
        rows = decoded.get("listadoTrenes")
    else:
        raise DwrParseError("El payload DWR no tiene formato de listado")
    if not isinstance(rows, list) or not all(isinstance(row, dict) for row in rows):
        raise DwrParseError("listadoTrenes no es una lista de trenes válida")
    return cast(list[dict[str, Any]], rows)


def _parse_train(row: dict[str, Any]) -> Train:
    departure = _parse_time(row.get("horaSalida"))
    arrival = _parse_time(row.get("horaLlegada"))
    service = _string_value(row.get("numeroTren")) or _string_value(row.get("tren")) or "unknown"
    departure_key = departure.isoformat() if departure else "unknown"
    arrival_key = arrival.isoformat() if arrival else "unknown"
    availability = _parse_availability(row)
    return Train(
        identifier=f"{service}|{departure_key}|{arrival_key}",
        departure=departure,
        arrival=arrival,
        price=_parse_price(row.get("precio")),
        availability=availability,
    )


def _parse_time(value: Any) -> time | None:
    if not isinstance(value, str) or not value.strip():
        return None
    for pattern in ("%H:%M", "%H:%M:%S"):
        try:
            return (
                time.fromisoformat(value)
                if pattern == "%H:%M:%S"
                else time.fromisoformat(value + ":00")
            )
        except ValueError:
            continue
    raise DwrParseError("Horario de tren inválido")


def _parse_price(value: Any) -> Decimal | None:
    if value is None or value == "":
        return None
    if isinstance(value, (int, float, Decimal)) and not isinstance(value, bool):
        return Decimal(str(value))
    if isinstance(value, str):
        normalized = re.sub(r"[^0-9,.-]", "", value)
        if "," in normalized and "." in normalized:
            normalized = normalized.replace(".", "").replace(",", ".")
        elif "," in normalized:
            normalized = normalized.replace(",", ".")
        try:
            return Decimal(normalized)
        except InvalidOperation as error:
            raise DwrParseError("Precio de tren inválido") from error
    raise DwrParseError("Precio de tren inválido")


def _parse_availability(row: dict[str, Any]) -> Availability:
    value = row.get("disponible", row.get("DISPONIBLE"))
    if value is True:
        return Availability.AVAILABLE
    if value is False:
        return Availability.NO_AVAILABILITY
    return Availability.UNKNOWN


def _string_value(value: Any) -> str | None:
    return value.strip() if isinstance(value, str) and value.strip() else None
