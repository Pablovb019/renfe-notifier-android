"""Tests del CLI administrativo (envío controlado de notificación de prueba)."""

from pathlib import Path

import pytest

import app.cli as cli
from app.pairing.database import PairingRepository
from app.pairing.service import PairingService


class FakeSender:
    """Sustituto del emisor FCM: no toca la red ni las credenciales."""

    def __init__(self, **_: object) -> None:
        self.calls: list[str] = []

    async def send_test(self, *, fcm_token: str) -> str:
        self.calls.append(fcm_token)
        return "messages/msg-test-1"


@pytest.fixture
def cli_db(tmp_path: Path, monkeypatch: pytest.MonkeyPatch) -> tuple[Path, pytest.MonkeyPatch]:
    db = tmp_path / "cli.db"
    monkeypatch.setenv("RENFE_NOTIFIER_DATABASE_PATH", str(db))
    monkeypatch.setenv("RENFE_NOTIFIER_ENVIRONMENT", "development")
    monkeypatch.setenv("RENFE_NOTIFIER_FCM_PROJECT_ID", "test-project")
    monkeypatch.setenv("RENFE_NOTIFIER_FCM_APP_PACKAGE", "com.pablovb019.renfenotifier")
    monkeypatch.delenv("RENFE_NOTIFIER_SECRET_KEY", raising=False)
    return db, monkeypatch


def _service(db: Path) -> PairingService:
    service = PairingService(PairingRepository(db), code_ttl_s=600.0, max_attempts=3)
    service.initialize()
    return service


def _claim_device(db: Path, fcm_token: str | None) -> None:
    service = _service(db)
    code = service.create_pairing_code()
    device_id = "device-test"
    service.claim_pairing(code, device_id, "realme")
    if fcm_token is not None:
        assert service.register_fcm_token(device_id, fcm_token)


def test_subcomando_existe() -> None:
    args = cli.build_parser().parse_args(["test-notification"])
    assert args.command == "test-notification"


def test_envia_al_primer_dispositivo_con_token(cli_db, capsys, monkeypatch) -> None:
    db, _ = cli_db
    _claim_device(db, "tok-1")
    fake = FakeSender()
    monkeypatch.setattr("app.notifications.fcm.FcmNotificationSender", lambda **_kw: fake)

    assert cli.main(["test-notification"]) == 0
    captured = capsys.readouterr()
    assert "messages/msg-test-1" in captured.out
    assert fake.calls == ["tok-1"]


def test_sin_dispositivo_activo(cli_db, capsys) -> None:
    db, _ = cli_db
    _service(db)
    assert cli.main(["test-notification"]) == 1
    assert "No hay un dispositivo activo" in capsys.readouterr().err


def test_sin_token_fcm(cli_db, capsys, monkeypatch) -> None:
    db, _ = cli_db
    _claim_device(db, None)
    fake = FakeSender()
    monkeypatch.setattr("app.notifications.fcm.FcmNotificationSender", lambda **_kw: fake)
    assert cli.main(["test-notification"]) == 1
    assert "El dispositivo no tiene token FCM" in capsys.readouterr().err
    assert fake.calls == []


def test_sin_configuracion_fcm(cli_db, capsys, monkeypatch) -> None:
    db, m = cli_db
    _claim_device(db, "tok-1")
    m.delenv("RENFE_NOTIFIER_FCM_PROJECT_ID")
    fake = FakeSender()
    monkeypatch.setattr("app.notifications.fcm.FcmNotificationSender", lambda **_kw: fake)
    assert cli.main(["test-notification"]) == 1
    assert "FCM no configurado" in capsys.readouterr().err
    assert fake.calls == []
