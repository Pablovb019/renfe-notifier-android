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
