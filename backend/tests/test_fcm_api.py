from pathlib import Path

from tests.helpers import auth_headers, claim_device, make_app

FCM_TOKEN = "APA91bDz8q-FZm1x2y3w4v5u6t7s8r9q0p1o2n3m4l5k6j7i8h9g0f1e2d3c4b5a6z7y8x9" * 2


def test_fcm_requires_auth(tmp_path: Path) -> None:
    with make_app(tmp_path / "data" / "test.db") as client:
        response = client.put("/api/v1/fcm/token", json={"fcm_token": FCM_TOKEN})

    assert response.status_code == 401


def test_fcm_registers_and_renews_token(tmp_path: Path) -> None:
    with make_app(tmp_path / "data" / "test.db") as client:
        token = claim_device(client)["device_token"]
        headers = auth_headers(token)
        first = client.put("/api/v1/fcm/token", json={"fcm_token": FCM_TOKEN}, headers=headers)
        renewed = client.put(
            "/api/v1/fcm/token", json={"fcm_token": "new" + FCM_TOKEN}, headers=headers
        )

    assert first.status_code == 200
    assert first.json() == {"registered": True}
    assert renewed.status_code == 200


def test_fcm_rejects_malformed_token(tmp_path: Path) -> None:
    with make_app(tmp_path / "data" / "test.db") as client:
        token = claim_device(client)["device_token"]
        headers = auth_headers(token)
        short = client.put("/api/v1/fcm/token", json={"fcm_token": "too-short"}, headers=headers)
        invalid = client.put(
            "/api/v1/fcm/token", json={"fcm_token": "invalid chars ######"}, headers=headers
        )

    assert short.status_code == 422
    assert invalid.status_code == 422
