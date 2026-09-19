from app.renfe.stations import Station, StationCatalog, normalize_station_key, station_match_score


def test_normalization_removes_accents_and_punctuation() -> None:
    assert normalize_station_key("  Ávila, Estación ") == "AVILAESTACION"


def test_catalog_resolves_alias_and_preserves_full_catalog() -> None:
    catalog = StationCatalog()

    station = catalog.resolve("atocha")

    assert len(catalog.stations) == 1347
    assert station is not None
    assert station.name == "MADRID PTA. ATOCHA - ALMUDENA GRANDES"


def test_group_returns_concrete_city_stations() -> None:
    catalog = StationCatalog()
    group = catalog.resolve("MADRID (TODAS)")

    assert group is not None and group.is_group
    candidates = catalog.group_candidates(group)
    assert candidates
    assert all(not candidate.is_group for candidate in candidates)
    assert all("MADRID" in candidate.name for candidate in candidates)


def test_by_code_resolves_an_existing_station() -> None:
    catalog = StationCatalog()
    first = catalog.stations[0]

    resolved = catalog.by_code(first.code)

    assert resolved is not None
    assert resolved.code == first.code
    assert resolved.name == first.name


def test_by_code_returns_none_for_unknown_code() -> None:
    assert StationCatalog().by_code("ZZZZZ") is None


def _station(name: str, priority: int = 100) -> Station:
    return Station(
        name=name,
        code="X",
        priority=priority,
        administration_code=None,
        uic_code=None,
    )


def test_match_score_returns_zero_without_match() -> None:
    assert station_match_score(_station("MADRID PTA. ATOCHA"), "SEVILLA") == 0.0
    assert station_match_score(_station("MADRID PTA. ATOCHA"), "") == 0.0


def test_match_score_exact_beats_prefix() -> None:
    exact = station_match_score(_station("ATOCHA"), "ATOCHA")
    prefix = station_match_score(_station("ATOCHAVEGA"), "ATOCHA")
    assert exact > prefix


def test_match_score_prefix_beats_word_boundary() -> None:
    prefix = station_match_score(_station("TOCHAVILA"), "TOCHA")
    boundary = station_match_score(_station("VILA TOCHAVILA"), "TOCHA")
    assert prefix > boundary


def test_match_score_word_boundary_beats_substring() -> None:
    boundary = station_match_score(_station("VILA TOCHAVILA"), "TOCHA")
    substring = station_match_score(_station("SANTOCHAVILA"), "TOCHA")
    assert boundary > substring


def test_match_score_priority_breaks_ties() -> None:
    lower_priority = station_match_score(_station("MADRID PTA. ATOCHA", priority=2), "ATOCHA")
    higher_priority = station_match_score(_station("MADRID PTA. ATOCHA", priority=500), "ATOCHA")
    assert lower_priority > higher_priority


def test_catalog_suggestions_rank_atecha_first() -> None:
    catalog = StationCatalog()
    ranked = sorted(
        (
            station
            for station in catalog.stations
            if "ATOCHA" in normalize_station_key(station.name)
        ),
        key=lambda station: -station_match_score(station, "ATOCHA"),
    )
    assert ranked[0].name == "MADRID PTA. ATOCHA - ALMUDENA GRANDES"
