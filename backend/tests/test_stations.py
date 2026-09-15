from app.renfe.stations import StationCatalog, normalize_station_key


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
