from datetime import UTC, date, datetime, time

import pytest

from app.followups.domain import (
    AlertState,
    AvailabilityState,
    FollowUp,
    FollowUpMode,
    Lifecycle,
    Observation,
    ObservationKind,
    TrainSnapshot,
    arrival_at_utc,
    departure_at_utc,
    end_of_travel_day_utc,
)
from app.renfe.parser import Availability

NOW = datetime(2026, 3, 1, 12, tzinfo=UTC)
TRAVEL_DATE = date(2026, 3, 15)


def _followup(
    mode: FollowUpMode = FollowUpMode.SPECIFIC, train_id: str | None = "real:100"
) -> FollowUp:
    return FollowUp.create(
        followup_id="synthetic-followup",
        origin_code="00001",
        destination_code="00002",
        travel_date=TRAVEL_DATE,
        mode=mode,
        now=NOW,
        departure_time=time(18),
        specific_train_id=train_id if mode is FollowUpMode.SPECIFIC else None,
    )


def _train(
    *, available: Availability, real_id: str = "100", departure: time = time(18)
) -> TrainSnapshot:
    return TrainSnapshot(
        departure=departure, arrival=time(20), availability=available, real_id=real_id
    )


def _valid(*trains: TrainSnapshot) -> Observation:
    return Observation(ObservationKind.VALID, NOW, trains)


def test_new_availability_episode_acknowledgement_and_reappearance() -> None:
    followup = _followup()

    first = followup.apply(_valid(_train(available=Availability.AVAILABLE)))
    acknowledged = first.followup.acknowledge()
    unchanged = acknowledged.apply(_valid(_train(available=Availability.AVAILABLE)))
    unavailable = unchanged.followup.apply(_valid(_train(available=Availability.NO_AVAILABILITY)))
    reappeared = unavailable.followup.apply(_valid(_train(available=Availability.AVAILABLE)))

    assert first.new_episode is True
    assert acknowledged.lifecycle is Lifecycle.ACTIVE
    assert acknowledged.alert_state is AlertState.ACKNOWLEDGED
    assert unchanged.new_episode is False
    assert unavailable.followup.alert_state is AlertState.IDLE
    assert reappeared.new_episode is True
    assert reappeared.followup.episode == 2


@pytest.mark.parametrize("kind", [ObservationKind.ERROR, ObservationKind.STALE])
def test_errors_and_stale_data_do_not_create_new_episodes(kind: ObservationKind) -> None:
    followup = _followup().apply(_valid(_train(available=Availability.NO_AVAILABILITY))).followup

    result = followup.apply(Observation(kind, NOW, (_train(available=Availability.AVAILABLE),)))

    assert result.new_episode is False
    assert result.followup.availability is AvailabilityState.UNAVAILABLE


def test_pause_renew_and_delete_are_independent_transitions() -> None:
    followup = _followup().apply(_valid(_train(available=Availability.AVAILABLE))).followup
    paused = followup.pause()
    renewed = paused.renew(now=NOW, departure_time=time(18))
    deleted = renewed.delete()

    assert paused.lifecycle is Lifecycle.PAUSED
    assert paused.alert_state is AlertState.IDLE
    assert renewed.lifecycle is Lifecycle.ACTIVE
    assert deleted.lifecycle is Lifecycle.DELETED
    assert deleted.renew(now=NOW) is deleted


def test_all_mode_creates_an_episode_for_newly_available_train() -> None:
    followup = _followup(FollowUpMode.ALL, None)
    first = followup.apply(_valid(_train(available=Availability.AVAILABLE, real_id="100")))
    second = first.followup.apply(
        _valid(
            _train(available=Availability.AVAILABLE, real_id="100"),
            _train(available=Availability.AVAILABLE, real_id="200", departure=time(19)),
        )
    )

    assert first.new_episode is True
    assert second.new_episode is True
    assert second.followup.episode == 2


@pytest.mark.parametrize(
    ("mode", "expected_new_episode"),
    [(FollowUpMode.FIRST, True), (FollowUpMode.LAST, False)],
)
def test_first_and_last_select_the_boundary_train(
    mode: FollowUpMode, expected_new_episode: bool
) -> None:
    followup = _followup(mode, None)
    result = followup.apply(
        _valid(
            _train(available=Availability.AVAILABLE, real_id="early", departure=time(7)),
            _train(available=Availability.NO_AVAILABILITY, real_id="late", departure=time(22)),
        )
    )

    assert result.new_episode is expected_new_episode


def test_real_id_is_preferred_and_fallback_is_documented_composite_key() -> None:
    real = _train(available=Availability.AVAILABLE, real_id="abc-42")
    fallback = TrainSnapshot(
        departure=time(8), arrival=time(10), availability=Availability.AVAILABLE
    )

    assert (
        real.identity(origin_code="A", destination_code="B", travel_date=TRAVEL_DATE)
        == "real:abc-42"
    )
    assert fallback.identity(origin_code="A", destination_code="B", travel_date=TRAVEL_DATE) == (
        "fallback:A:B:2026-03-15:08:00:00:10:00:00"
    )


def test_expiry_is_limited_by_departure_and_all_mode_ends_at_local_day_end() -> None:
    specific = _followup()
    all_trains = _followup(FollowUpMode.ALL, None)

    assert specific.expires_at == departure_at_utc(TRAVEL_DATE, time(18))
    assert all_trains.expires_at == end_of_travel_day_utc(TRAVEL_DATE)


def test_midnight_arrival_and_summer_time_change_use_madrid_dates() -> None:
    midnight_arrival = arrival_at_utc(date(2026, 10, 24), time(23, 55), time(0, 20))
    spring_departure = departure_at_utc(date(2026, 3, 29), time(1, 30))
    spring_arrival = arrival_at_utc(date(2026, 3, 29), time(1, 30), time(3, 30))

    assert midnight_arrival == datetime(2026, 10, 24, 22, 20, tzinfo=UTC)
    assert spring_departure == datetime(2026, 3, 29, 0, 30, tzinfo=UTC)
    assert spring_arrival == datetime(2026, 3, 29, 1, 30, tzinfo=UTC)
