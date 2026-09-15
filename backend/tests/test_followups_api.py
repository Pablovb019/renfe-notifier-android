from datetime import UTC, datetime, timedelta
from pathlib import Path
from typing import cast

from fastapi import FastAPI

from app.followups.database import FollowUpRepository
from app.followups.domain import (
    MADRID,
    Observation,
    ObservationKind,
    TrainSnapshot,
)
from app.renfe.parser import Availability
from tests.helpers import auth_headers, claim_device, make_app

ORIGIN_CODE = "60000"
DESTINATION_CODE = "71801"


def _future_date(days: int = 5) -> str:
    return (datetime.now(MADRID).date() + timedelta(days=days)).isoformat()


def _base_payload(**overrides: object) -> dict[str, object]:
    payload: dict[str, object] = {
        "origin_code": ORIGIN_CODE,
        "destination_code": DESTINATION_CODE,
        "travel_date": _future_date(),
        "mode": "first",
        "plaza_h": False,
    }
    payload.update(overrides)
    return payload


def _repository(app: FastAPI) -> FollowUpRepository:
    repository = app.state.followups_repository
    if not isinstance(repository, FollowUpRepository):
        raise RuntimeError("FollowUpRepository no inicializado")
    return repository


def _make_available(followup_id: str, app: FastAPI, /) -> None:
    repository = _repository(app)
    followup = repository.get(followup_id)
    assert followup is not None
    observation = Observation(
        kind=ObservationKind.VALID,
        observed_at=datetime.now(UTC),
        trains=(
            TrainSnapshot(
                departure=None,
                arrival=None,
                availability=Availability.AVAILABLE,
                real_id="AVE-001|08:00|09:30",
            ),
        ),
    )
    result = followup.apply(observation)
    assert result.new_episode is True
    repository.save(result.followup)


def test_create_followup_returns_server_generated_id(tmp_path: Path) -> None:
    with make_app(tmp_path / "data" / "test.db") as client:
        token = claim_device(client)["device_token"]
        response = client.post(
            "/api/v1/followups",
            json=_base_payload(),
            headers=auth_headers(token),
        )

    assert response.status_code == 201
    body = response.json()
    assert body["followup_id"]
    assert body["origin_name"] is not None
    assert body["destination_name"] is not None
    assert body["origin_code"] == ORIGIN_CODE
    assert body["mode"] == "first"
    assert body["lifecycle"] == "active"
    assert body["availability"] == "unknown"
    assert body["alert_state"] == "idle"
    assert body["episode"] == 0


def test_create_specific_mode_requires_specific_train_id(tmp_path: Path) -> None:
    with make_app(tmp_path / "data" / "test.db") as client:
        token = claim_device(client)["device_token"]
        missing = client.post(
            "/api/v1/followups",
            json=_base_payload(mode="specific"),
            headers=auth_headers(token),
        )
        ok = client.post(
            "/api/v1/followups",
            json=_base_payload(mode="specific", specific_train_id="real:AVE-007|12:00|13:30"),
            headers=auth_headers(token),
        )
        conflict = client.post(
            "/api/v1/followups",
            json=_base_payload(mode="first", specific_train_id="real:AVE-007|12:00|13:30"),
            headers=auth_headers(token),
        )

    assert missing.status_code == 400
    assert ok.status_code == 201
    assert ok.json()["specific_train_id"].startswith("real:")
    assert conflict.status_code == 400


def test_create_rejects_unknown_station_and_past_date(tmp_path: Path) -> None:
    with make_app(tmp_path / "data" / "test.db") as client:
        token = claim_device(client)["device_token"]
        unknown = client.post(
            "/api/v1/followups",
            json=_base_payload(origin_code="99999"),
            headers=auth_headers(token),
        )
        past = client.post(
            "/api/v1/followups",
            json=_base_payload(
                travel_date=(datetime.now(MADRID).date() - timedelta(days=1)).isoformat()
            ),
            headers=auth_headers(token),
        )
        same = client.post(
            "/api/v1/followups",
            json=_base_payload(destination_code=ORIGIN_CODE),
            headers=auth_headers(token),
        )

    assert unknown.status_code == 400
    assert past.status_code == 400
    assert same.status_code == 400


def test_create_followup_requires_auth(tmp_path: Path) -> None:
    with make_app(tmp_path / "data" / "test.db") as client:
        response = client.post("/api/v1/followups", json=_base_payload())

    assert response.status_code == 401


def test_list_and_getdetail(tmp_path: Path) -> None:
    with make_app(tmp_path / "data" / "test.db") as client:
        token = claim_device(client)["device_token"]
        first = client.post("/api/v1/followups", json=_base_payload(), headers=auth_headers(token))
        client.post(
            "/api/v1/followups",
            json=_base_payload(mode="all"),
            headers=auth_headers(token),
        )
        listing = client.get("/api/v1/followups", headers=auth_headers(token))
        active = client.get(
            "/api/v1/followups", params={"lifecycle": "active"}, headers=auth_headers(token)
        )
        detail = client.get(
            f"/api/v1/followups/{first.json()['followup_id']}",
            headers=auth_headers(token),
        )
        missing = client.get("/api/v1/followups/not-a-real-id-12345", headers=auth_headers(token))

    assert listing.status_code == 200
    assert listing.json()["total"] == 2
    assert len(listing.json()["items"]) == 2
    assert active.json()["total"] == 2
    assert detail.status_code == 200
    assert detail.json()["episodes"] == []
    assert missing.status_code == 404


def test_pause_and_resume_are_independent_and_reversible(tmp_path: Path) -> None:
    with make_app(tmp_path / "data" / "test.db") as client:
        token = claim_device(client)["device_token"]
        created = client.post(
            "/api/v1/followups", json=_base_payload(), headers=auth_headers(token)
        )
        followup_id = created.json()["followup_id"]
        headers = auth_headers(token)

        paused = client.post(f"/api/v1/followups/{followup_id}/pause", headers=headers)
        pause_again = client.post(f"/api/v1/followups/{followup_id}/pause", headers=headers)
        resumed = client.post(f"/api/v1/followups/{followup_id}/resume", headers=headers)
        resume_again = client.post(f"/api/v1/followups/{followup_id}/resume", headers=headers)

    assert paused.status_code == 200
    assert paused.json()["lifecycle"] == "paused"
    assert pause_again.status_code == 409
    assert resumed.status_code == 200
    assert resumed.json()["lifecycle"] == "active"
    assert resume_again.status_code == 409


def test_renew_reactivates_expired(tmp_path: Path) -> None:
    with make_app(tmp_path / "data" / "test.db") as client:
        token = claim_device(client)["device_token"]
        created = client.post(
            "/api/v1/followups", json=_base_payload(), headers=auth_headers(token)
        )
        followup_id = created.json()["followup_id"]
        app = cast(FastAPI, client.app)
        repository = _repository(app)

        followup = repository.get(followup_id)
        assert followup is not None
        repository.save(followup.expire())
        expired = client.get(f"/api/v1/followups/{followup_id}", headers=auth_headers(token))
        renewed = client.post(f"/api/v1/followups/{followup_id}/renew", headers=auth_headers(token))
        renewed_again = client.post(
            f"/api/v1/followups/{followup_id}/renew", headers=auth_headers(token)
        )

    assert expired.json()["lifecycle"] == "expired"
    assert renewed.status_code == 200
    assert renewed.json()["lifecycle"] == "active"
    assert renewed_again.status_code == 200


def test_acknowledge_confirms_without_deleting(tmp_path: Path) -> None:
    with make_app(tmp_path / "data" / "test.db") as client:
        token = claim_device(client)["device_token"]
        created = client.post(
            "/api/v1/followups", json=_base_payload(), headers=auth_headers(token)
        )
        followup_id = created.json()["followup_id"]
        headers = auth_headers(token)

        no_pending = client.post(f"/api/v1/followups/{followup_id}/acknowledge", headers=headers)
        _make_available(followup_id, cast(FastAPI, client.app))
        pending = client.get(f"/api/v1/followups/{followup_id}", headers=headers)
        acknowledged = client.post(f"/api/v1/followups/{followup_id}/acknowledge", headers=headers)
        ack_again = client.post(f"/api/v1/followups/{followup_id}/acknowledge", headers=headers)

    assert no_pending.status_code == 409
    assert pending.json()["alert_state"] == "pending_alert"
    assert acknowledged.status_code == 200
    assert acknowledged.json()["alert_state"] == "acknowledged"
    assert acknowledged.json()["lifecycle"] == "active"
    assert ack_again.status_code == 409


def test_delete_is_soft_and_idempotent(tmp_path: Path) -> None:
    with make_app(tmp_path / "data" / "test.db") as client:
        token = claim_device(client)["device_token"]
        created = client.post(
            "/api/v1/followups", json=_base_payload(), headers=auth_headers(token)
        )
        followup_id = created.json()["followup_id"]
        headers = auth_headers(token)

        deleted = client.delete(f"/api/v1/followups/{followup_id}", headers=headers)
        deleted_again = client.delete(f"/api/v1/followups/{followup_id}", headers=headers)
        after = client.get(f"/api/v1/followups/{followup_id}", headers=headers)
        listing = client.get("/api/v1/followups", params={"lifecycle": "deleted"}, headers=headers)

    assert deleted.status_code == 204
    assert deleted_again.status_code == 204
    assert after.json()["lifecycle"] == "deleted"
    assert listing.json()["total"] == 1


def test_actions_require_existing_followup(tmp_path: Path) -> None:
    with make_app(tmp_path / "data" / "test.db") as client:
        token = claim_device(client)["device_token"]
        headers = auth_headers(token)
        pause = client.post("/api/v1/followups/nope-123/pause", headers=headers)
        delete = client.delete("/api/v1/followups/nope-123", headers=headers)

    assert pause.status_code == 404
    assert delete.status_code == 404
