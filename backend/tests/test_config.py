import pytest
from pydantic import ValidationError

from app.config import Settings


def test_production_requires_secret_key() -> None:
    with pytest.raises(ValidationError, match="RENFE_NOTIFIER_SECRET_KEY"):
        Settings(environment="production")


def test_environment_values_are_validated(monkeypatch: pytest.MonkeyPatch) -> None:
    monkeypatch.setenv("RENFE_NOTIFIER_PORT", "99999")

    with pytest.raises(ValidationError):
        Settings()
