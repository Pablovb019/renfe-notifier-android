import sqlite3
from datetime import UTC, date, datetime, time
from pathlib import Path

import pytest

from app.db.connection import backup_database, connect
from app.followups.database import FollowUpRepository
from app.followups.domain import (
    AlertState,
    AvailabilityState,
    FollowUp,
    FollowUpMode,
    Lifecycle,
    Observation,
    ObservationKind,
    TrainSnapshot,
)
from app.renfe.parser import Availability

NOW = datetime(2026, 3, 1, 12, tzinfo=UTC)
TRAVEL_DATE = date(2026, 3, 15)


@pytest.fixture
def repo(tmp_path: Path) -> FollowUpRepository:
    repository = FollowUpRepository(tmp_path / "data" / "test.db")
    repository.initialize()
    return repository


def _followup(followup_id: str = "followup-1") -> FollowUp:
    return FollowUp.create(
        followup_id=followup_id,
        origin_code="00001",
        destination_code="00002",
        travel_date=TRAVEL_DATE,
        mode=FollowUpMode.SPECIFIC,
        now=NOW,
        departure_time=time(18),
        specific_train_id="real:100",
    )


def _train() -> TrainSnapshot:
    return TrainSnapshot(
        departure=time(18), arrival=time(20), availability=Availability.AVAILABLE, real_id="100"
    )


def _schema_versions(db_path: str) -> list[int]:
    connection = sqlite3.connect(db_path)
    try:
        return [row[0] for row in connection.execute("SELECT version FROM schema_migrations")]
    finally:
        connection.close()


def test_initialize_creates_schema_with_wal_and_pragmas(
    repo: FollowUpRepository, tmp_path: Path
) -> None:
    assert (tmp_path / "data" / "test.db").exists()

    connection = connect(tmp_path / "data" / "test.db")
    try:
        assert connection.execute("PRAGMA journal_mode").fetchone()[0] == "wal"
        assert connection.execute("PRAGMA busy_timeout").fetchone()[0] == 5000
        assert connection.execute("PRAGMA foreign_keys").fetchone()[0] == 1
    finally:
        connection.close()
    assert _schema_versions(str(tmp_path / "data" / "test.db")) == [1, 2, 3, 4, 5]


def test_initialize_is_idempotent_across_restarts(repo: FollowUpRepository, tmp_path: Path) -> None:
    restarted = FollowUpRepository(tmp_path / "data" / "test.db")
    restarted.initialize()
    restarted.initialize()

    assert _schema_versions(str(tmp_path / "data" / "test.db")) == [1, 2, 3, 4, 5]


def test_create_get_update_roundtrip(repo: FollowUpRepository) -> None:
    followup = _followup()
    repo.create(followup)
    assert repo.get(followup.followup_id) == followup

    result = followup.apply(Observation(ObservationKind.VALID, NOW, (_train(),)))
    acknowledged = result.followup.acknowledge()
    repo.save(acknowledged)

    reloaded = repo.get(followup.followup_id)
    assert reloaded is not None
    assert reloaded.availability is AvailabilityState.AVAILABLE
    assert reloaded.alert_state is AlertState.ACKNOWLEDGED
    assert reloaded.episode == 1
    assert reloaded.seen_available_train_ids == frozenset({"real:100"})


def test_restart_persists_followups(tmp_path: Path) -> None:
    db_path = tmp_path / "data" / "test.db"
    first = FollowUpRepository(db_path)
    first.initialize()
    first.create(_followup())

    restarted = FollowUpRepository(db_path)
    restarted.initialize()
    assert restarted.get("followup-1") == _followup()


def test_plaza_h_roundtrip_in_repository(repo: FollowUpRepository) -> None:
    followup = FollowUp.create(
        followup_id="followup-plazah",
        origin_code="00001",
        destination_code="00002",
        travel_date=TRAVEL_DATE,
        mode=FollowUpMode.FIRST,
        plaza_h=True,
        now=NOW,
    )
    repo.create(followup)

    loaded = repo.get("followup-plazah")

    assert loaded is not None
    assert loaded.plaza_h is True


def test_episodes_roundtrip_and_unique_per_episode(repo: FollowUpRepository) -> None:
    repo.create(_followup())
    first = repo.add_episode("followup-1", 1, NOW, frozenset({"real:100"}))
    replay = repo.add_episode("followup-1", 1, NOW, frozenset({"real:777"}))

    episodes = repo.list_episodes("followup-1")
    assert len(episodes) == 1
    assert episodes[0].episode == 1
    assert episodes[0].observed_at == NOW
    assert episodes[0].train_ids == frozenset({"real:100"})
    assert first.episode_id == replay.episode_id
    assert first.episode_id == episodes[0].episode_id


def test_list_filter_by_lifecycle_and_purge_deleted(repo: FollowUpRepository) -> None:
    active = _followup("followup-active")
    deleted = _followup("followup-deleted")
    repo.create(active)
    repo.create(deleted)
    repo.save(deleted.delete())

    assert {f.followup_id for f in repo.list()} == {"followup-active", "followup-deleted"}
    assert {f.followup_id for f in repo.list(Lifecycle.ACTIVE)} == {"followup-active"}

    assert repo.purge_deleted() == 1
    assert repo.get("followup-deleted") is None
    assert repo.get("followup-active") is not None


def test_config_roundtrip(repo: FollowUpRepository) -> None:
    assert repo.get_config("missing") is None
    repo.set_config("last_episode_id", "42")
    repo.set_config("last_episode_id", "43")
    assert repo.get_config("last_episode_id") == "43"


def test_duplicate_create_fails_and_leaves_previous_intact(repo: FollowUpRepository) -> None:
    repo.create(_followup())
    with pytest.raises(sqlite3.IntegrityError):
        repo.create(_followup())
    assert repo.get("followup-1") == _followup()
    assert len(repo.list()) == 1


def test_backup_is_consistent_and_independent(tmp_path: Path) -> None:
    db_path = tmp_path / "data" / "test.db"
    first = FollowUpRepository(db_path)
    first.initialize()
    first.create(_followup())
    first.add_episode("followup-1", 1, NOW, frozenset({"real:100"}))

    backup_path = tmp_path / "backups" / "backup.db"
    backup_database(db_path, backup_path)
    assert backup_path.exists()

    restored = FollowUpRepository(backup_path)
    restored.initialize()
    assert restored.get("followup-1") == _followup()
    assert len(restored.list_episodes("followup-1")) == 1

    db_path.unlink()
    reopened = FollowUpRepository(backup_path)
    reopened.initialize()
    assert reopened.get("followup-1") is not None


def test_backup_missing_source_raises(tmp_path: Path) -> None:
    with pytest.raises(FileNotFoundError):
        backup_database(tmp_path / "missing.db", tmp_path / "out.db")


def test_recovery_creates_fresh_database_after_removal(tmp_path: Path) -> None:
    db_path = tmp_path / "data" / "test.db"
    first = FollowUpRepository(db_path)
    first.initialize()
    first.create(_followup())
    for suffix in ("", "-wal", "-shm"):
        path = Path(f"{db_path}{suffix}")
        if path.exists():
            path.unlink()

    recovered = FollowUpRepository(db_path)
    recovered.initialize()
    assert recovered.get("followup-1") is None
    assert _schema_versions(str(db_path)) == [1, 2, 3, 4, 5]
