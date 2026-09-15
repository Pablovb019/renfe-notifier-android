import asyncio
from datetime import UTC, date, datetime, time
from pathlib import Path

import pytest

from app.followups.database import FollowUpRepository
from app.followups.domain import AlertState, AvailabilityState, FollowUp, FollowUpMode
from app.renfe.client import RenfeClientError, SearchMetrics, SearchResult
from app.renfe.parser import Availability, ParseStatus, Train, TrainList
from app.renfe.stations import Station, StationCatalog
from app.scheduler.service import SchedulerService

FAKE_NOW = datetime(2026, 3, 1, 12, tzinfo=UTC)
TRAVEL_DATE = date(2026, 3, 15)


class FakeSearch:
    def __init__(self, results: list[SearchResult]) -> None:
        self._results = results
        self._index = 0
        self.calls: list[tuple[str, str, date, bool]] = []

    async def __call__(
        self, *, origin: Station, destination: Station, travel_date: date, plaza_h: bool
    ) -> SearchResult:
        self.calls.append((origin.code, destination.code, travel_date, plaza_h))
        result = self._results[self._index]
        self._index += 1
        return result


@pytest.fixture
def repo(tmp_path: Path) -> FollowUpRepository:
    repository = FollowUpRepository(tmp_path / "data" / "test.db")
    repository.initialize()
    return repository


@pytest.fixture
def catalog() -> StationCatalog:
    return StationCatalog()


@pytest.fixture
def sample_stations(catalog: StationCatalog) -> tuple[Station, Station]:
    concrete = [station for station in catalog.stations if not station.is_group]
    return concrete[0], concrete[1]


def _search_result(train_list: TrainList) -> SearchResult:
    metrics = SearchMetrics(request_count=1, duration_s=0.0, bytes_received=0, requests=())
    return SearchResult(trains=train_list, metrics=metrics)


def _followup(
    followup_id: str = "f1",
    origin: Station | None = None,
    destination: Station | None = None,
    plaza_h: bool = False,
) -> FollowUp:
    return FollowUp.create(
        followup_id=followup_id,
        origin_code=origin.code if origin else "A",
        destination_code=destination.code if destination else "B",
        travel_date=TRAVEL_DATE,
        mode=FollowUpMode.FIRST,
        plaza_h=plaza_h,
        now=FAKE_NOW,
    )


def _available_train(identifier: str = "T-001") -> Train:
    return Train(
        identifier=identifier,
        departure=time(10),
        arrival=time(12),
        price=None,
        availability=Availability.AVAILABLE,
    )


def _no_availability_train(identifier: str = "T-001") -> Train:
    return Train(
        identifier=identifier,
        departure=time(10),
        arrival=time(12),
        price=None,
        availability=Availability.NO_AVAILABILITY,
    )


def _empty_train_list() -> TrainList:
    return TrainList(status=ParseStatus.NO_TRAINS, plaza_h_requested=False, trains=())


@pytest.mark.asyncio
async def test_run_once_skips_when_no_active_followups(repo: FollowUpRepository) -> None:
    async def unexpected_search(**kwargs: object) -> SearchResult:
        raise AssertionError("no debería buscar sin seguimientos activos")

    service = SchedulerService(
        repository=repo,
        search_fn=unexpected_search,
        catalog=StationCatalog(),
        clock=lambda: FAKE_NOW,
    )

    await service.run_once()


@pytest.mark.asyncio
async def test_run_once_queries_renfe_and_persists_followup(
    repo: FollowUpRepository, catalog: StationCatalog, sample_stations: tuple[Station, Station]
) -> None:
    origin, destination = sample_stations
    followup = _followup(origin=origin, destination=destination)
    repo.create(followup)

    search = FakeSearch(
        [
            _search_result(
                TrainList(
                    status=ParseStatus.OK, plaza_h_requested=False, trains=(_available_train(),)
                )
            )
        ]
    )
    service = SchedulerService(
        repository=repo, search_fn=search, catalog=catalog, clock=lambda: FAKE_NOW
    )

    await service.run_once()

    assert search.calls == [(origin.code, destination.code, TRAVEL_DATE, False)]
    loaded = repo.get("f1")
    assert loaded is not None
    assert loaded.availability is AvailabilityState.AVAILABLE
    assert loaded.alert_state is AlertState.PENDING
    assert loaded.episode == 1
    assert loaded.seen_available_train_ids == frozenset({"real:T-001"})


@pytest.mark.asyncio
async def test_run_once_handles_renfe_client_error_keeping_followup_unchanged(
    repo: FollowUpRepository, catalog: StationCatalog, sample_stations: tuple[Station, Station]
) -> None:
    origin, destination = sample_stations
    followup = _followup(origin=origin, destination=destination)
    repo.create(followup)

    async def failing_search(**kwargs: object) -> SearchResult:
        raise RenfeClientError("fallo sintético")

    service = SchedulerService(
        repository=repo, search_fn=failing_search, catalog=catalog, clock=lambda: FAKE_NOW
    )

    await service.run_once()

    loaded = repo.get("f1")
    assert loaded is not None
    assert loaded.availability is AvailabilityState.UNKNOWN
    assert loaded.alert_state is AlertState.IDLE
    assert loaded.episode == 0


@pytest.mark.asyncio
async def test_run_once_skips_group_with_unknown_station(
    repo: FollowUpRepository, catalog: StationCatalog
) -> None:
    followup = FollowUp.create(
        followup_id="f1",
        origin_code="ZZZZZ",
        destination_code="YYYYY",
        travel_date=TRAVEL_DATE,
        mode=FollowUpMode.FIRST,
        now=FAKE_NOW,
    )
    repo.create(followup)

    async def unexpected_search(**kwargs: object) -> SearchResult:
        raise AssertionError("la estación desconocida no debe provocar búsqueda")

    service = SchedulerService(
        repository=repo, search_fn=unexpected_search, catalog=catalog, clock=lambda: FAKE_NOW
    )

    await service.run_once()

    loaded = repo.get("f1")
    assert loaded is not None
    assert loaded.availability is AvailabilityState.UNKNOWN


@pytest.mark.asyncio
async def test_run_once_processes_separate_groups_even_with_concurrency_one(
    repo: FollowUpRepository, catalog: StationCatalog, sample_stations: tuple[Station, Station]
) -> None:
    origin, destination = sample_stations
    repo.create(_followup("f1", origin, destination, plaza_h=False))
    repo.create(_followup("f2", origin, destination, plaza_h=True))

    search = FakeSearch(
        [
            _search_result(_empty_train_list()),
            _search_result(_empty_train_list()),
        ]
    )
    service = SchedulerService(
        repository=repo, search_fn=search, catalog=catalog, clock=lambda: FAKE_NOW, concurrency=1
    )

    await service.run_once()

    assert len(search.calls) == 2
    assert set(search.calls) == {
        (origin.code, destination.code, TRAVEL_DATE, False),
        (origin.code, destination.code, TRAVEL_DATE, True),
    }


@pytest.mark.asyncio
async def test_run_once_does_not_create_duplicate_episode_while_still_available(
    repo: FollowUpRepository, catalog: StationCatalog, sample_stations: tuple[Station, Station]
) -> None:
    origin, destination = sample_stations
    repo.create(_followup(origin=origin, destination=destination))

    available = _search_result(
        TrainList(status=ParseStatus.OK, plaza_h_requested=False, trains=(_available_train(),))
    )
    search = FakeSearch([available, available])
    service = SchedulerService(
        repository=repo, search_fn=search, catalog=catalog, clock=lambda: FAKE_NOW
    )

    await service.run_once()
    await service.run_once()

    loaded = repo.get("f1")
    assert loaded is not None
    assert loaded.episode == 1
    assert loaded.alert_state is AlertState.PENDING


@pytest.mark.asyncio
async def test_run_once_creates_new_episode_when_availability_reappears(
    repo: FollowUpRepository, catalog: StationCatalog, sample_stations: tuple[Station, Station]
) -> None:
    origin, destination = sample_stations
    repo.create(_followup(origin=origin, destination=destination))

    available = _search_result(
        TrainList(status=ParseStatus.OK, plaza_h_requested=False, trains=(_available_train(),))
    )
    unavailable = _search_result(
        TrainList(
            status=ParseStatus.OK, plaza_h_requested=False, trains=(_no_availability_train(),)
        )
    )
    search = FakeSearch([available, unavailable, available])
    service = SchedulerService(
        repository=repo, search_fn=search, catalog=catalog, clock=lambda: FAKE_NOW
    )

    await service.run_once()
    first = repo.get("f1")
    assert first is not None
    assert first.episode == 1

    await service.run_once()
    after_gap = repo.get("f1")
    assert after_gap is not None
    assert after_gap.availability is AvailabilityState.UNAVAILABLE
    assert after_gap.alert_state is AlertState.IDLE
    assert after_gap.episode == 1

    await service.run_once()
    after_reappearance = repo.get("f1")
    assert after_reappearance is not None
    assert after_reappearance.episode == 2
    assert after_reappearance.alert_state is AlertState.PENDING


@pytest.mark.asyncio
async def test_run_forever_checks_active_followups_each_tick(
    tmp_path: Path, sample_stations: tuple[Station, Station]
) -> None:
    origin, destination = sample_stations
    repo = FollowUpRepository(tmp_path / "data" / "test.db")
    repo.initialize()
    repo.create(_followup(origin=origin, destination=destination))

    search_count = 0

    async def counting_search(**kwargs: object) -> SearchResult:
        nonlocal search_count
        search_count += 1
        return _search_result(_empty_train_list())

    sleep_count = 0

    async def counting_sleep(delay: float) -> None:
        nonlocal sleep_count
        sleep_count += 1
        if sleep_count >= 3:
            raise asyncio.CancelledError

    service = SchedulerService(
        repository=repo,
        search_fn=counting_search,
        catalog=StationCatalog(),
        clock=lambda: FAKE_NOW,
        sleep=counting_sleep,
    )

    with pytest.raises(asyncio.CancelledError):
        await service.run_forever()

    assert sleep_count == 3
    assert search_count == 3
