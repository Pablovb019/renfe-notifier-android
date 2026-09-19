from pathlib import Path

import pytest

from app.renfe.parser import Availability, DwrParseError, ParseStatus, parse_train_list


def test_parser_reads_synthetic_fixture_and_keeps_plaza_h_context() -> None:
    fixture = Path(__file__).parent / "fixtures" / "dwr_train_list_synthetic.txt"

    result = parse_train_list(fixture.read_text(encoding="utf-8"), plaza_h_requested=True)

    assert result.status is ParseStatus.OK
    assert result.plaza_h_requested is True
    assert result.trains[0].identifier == "SYN-100|08:15:00|10:45:00"
    assert str(result.trains[0].price) == "35.20"
    assert result.trains[1].price is None
    assert result.trains[1].availability is Availability.NO_AVAILABILITY


def test_parser_distinguishes_no_availability_from_invalid_response() -> None:
    no_availability = 'r.handleCallback("0", "0", {listadoTrenes: [{disponible: false}]});'

    result = parse_train_list(no_availability, plaza_h_requested=False)

    assert result.status is ParseStatus.NO_AVAILABILITY
    with pytest.raises(DwrParseError, match="callback DWR"):
        parse_train_list("respuesta no válida", plaza_h_requested=False)


def test_parser_flattens_grouped_payload_from_the_real_dwr_shape() -> None:
    payload = (
        'r.handleCallback("0", "0", {listadoTrenes: ['
        "{listviajeViewEnlaceBean: ["
        '{horaSalida: "07:30", horaLlegada: "10:10", tarifaMinima: "45,90", '
        'tipoTrenUno: "AVE", completo: false, razonNoDisponible: ""}, '
        '{horaSalida: "08:00", horaLlegada: "10:40", tarifaMinima: "NaN", '
        'tipoTrenUno: "AVE", completo: true, razonNoDisponible: "1"}'
        "]}]});"
    )

    result = parse_train_list(payload, plaza_h_requested=False)

    assert result.status is ParseStatus.OK
    assert result.trains[0].identifier == "AVE|07:30:00|10:10:00"
    assert str(result.trains[0].price) == "45.90"
    assert result.trains[0].availability is Availability.AVAILABLE
    assert result.trains[1].price is None
    assert result.trains[1].availability is Availability.NO_AVAILABILITY


def test_parser_ignores_solo_plaza_h_when_plaza_h_not_requested() -> None:
    payload = (
        'r.handleCallback("0", "0", {listadoTrenes: ['
        '{horaSalida: "11:00", horaLlegada: "12:00", tarifaMinima: "21,30", '
        'tipoTrenUno: "MD", completo: false, razonNoDisponible: "", soloPlazaH: true}'
        "]});"
    )

    result = parse_train_list(payload, plaza_h_requested=False)

    assert result.status is ParseStatus.NO_AVAILABILITY
    assert result.trains[0].availability is Availability.NO_AVAILABILITY


def test_parser_counts_solo_plaza_h_when_plaza_h_requested() -> None:
    payload = (
        'r.handleCallback("0", "0", {listadoTrenes: ['
        "{listviajeViewEnlaceBean: ["
        '{horaSalida: "11:00", horaLlegada: "12:00", tarifaMinima: "21,30", '
        'tipoTrenUno: "MD", completo: false, razonNoDisponible: "", soloPlazaH: true}, '
        '{horaSalida: "13:00", horaLlegada: "14:00", tarifaMinima: "15,00", '
        'tipoTrenUno: "REG", completo: false, razonNoDisponible: "", soloPlazaH: false}'
        "]}]});"
    )

    result = parse_train_list(payload, plaza_h_requested=True)

    assert result.status is ParseStatus.OK
    assert result.trains[0].availability is Availability.AVAILABLE
    assert result.trains[1].availability is Availability.NO_AVAILABILITY

    result_without_h = parse_train_list(payload, plaza_h_requested=False)
    assert result_without_h.trains[0].availability is Availability.NO_AVAILABILITY
    assert result_without_h.trains[1].availability is Availability.AVAILABLE
