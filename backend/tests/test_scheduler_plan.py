from datetime import UTC, date, datetime, time

from app.followups.domain import FollowUp, FollowUpMode, ObservationKind
from app.renfe.parser import Availability, ParseStatus, Train, TrainList
from app.scheduler.plan import (
    GroupKey,
    build_plan,
    error_observation,
    train_list_to_observation,
)

NOW = datetime(2026, 3, 1, 12, tzinfo=UTC)
TRAVEL_DATE = date(2026, 3, 15)


def _followup(
    followup_id: str = "f1",
    origin_code: str = "A",
    destination_code: str = "B",
    plaza_h: bool = False,
) -> FollowUp:
    return FollowUp.create(
        followup_id=followup_id,
        origin_code=origin_code,
        destination_code=destination_code,
        travel_date=TRAVEL_DATE,
        mode=FollowUpMode.FIRST,
        plaza_h=plaza_h,
        now=NOW,
    )


def _train_list(*trains: Train) -> TrainList:
    return TrainList(status=ParseStatus.OK, plaza_h_requested=False, trains=trains)


def test_build_plan_single_group_collects_compatible_followups() -> None:
    followups = [_followup(f"f{i}", "X", "Y") for i in range(5)]

    plan = build_plan(followups)

    assert len(plan) == 1
    key, group = plan[0]
    assert key == GroupKey("X", "Y", TRAVEL_DATE, False)
    assert [f.followup_id for f in group] == [f"f{i}" for i in range(5)]


def test_build_plan_splits_groups_by_plaza_h_and_route() -> None:
    followups = [
        _followup("f1", "A", "B"),
        _followup("f2", "A", "B", plaza_h=True),
        _followup("f3", "A", "C"),
    ]

    plan = build_plan(followups)

    assert len(plan) == 3
    keys = {group[0] for group in plan}
    assert keys == {
        GroupKey("A", "B", TRAVEL_DATE, False),
        GroupKey("A", "B", TRAVEL_DATE, True),
        GroupKey("A", "C", TRAVEL_DATE, False),
    }


def test_build_plan_empty_input() -> None:
    assert build_plan([]) == []


def test_train_list_to_observation_creates_valid_snapshots() -> None:
    train = Train(
        identifier="T-001",
        departure=time(8),
        arrival=time(10),
        price=None,
        availability=Availability.AVAILABLE,
    )

    observation = train_list_to_observation(_train_list(train), NOW)

    assert observation.kind is ObservationKind.VALID
    assert observation.observed_at == NOW
    assert len(observation.trains) == 1
    snapshot = observation.trains[0]
    assert snapshot.real_id == "T-001"
    assert snapshot.departure == time(8)
    assert snapshot.arrival == time(10)
    assert snapshot.availability is Availability.AVAILABLE


def test_train_list_to_observation_maps_every_train() -> None:
    trains = (
        Train(
            identifier="T-1",
            departure=time(8),
            arrival=time(10),
            price=None,
            availability=Availability.AVAILABLE,
        ),
        Train(
            identifier="T-2",
            departure=time(9),
            arrival=time(11),
            price=None,
            availability=Availability.NO_AVAILABILITY,
        ),
    )

    observation = train_list_to_observation(_train_list(*trains), NOW)

    assert [s.real_id for s in observation.trains] == ["T-1", "T-2"]
    assert observation.trains[1].availability is Availability.NO_AVAILABILITY


def test_error_observation_has_error_kind() -> None:
    observation = error_observation(NOW)

    assert observation.kind is ObservationKind.ERROR
    assert observation.observed_at == NOW
    assert observation.trains == ()
