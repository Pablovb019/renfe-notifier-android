"""Sanitización de registros: nunca tokens, códigos ni cookies en claro."""

import logging
import re

_SENSITIVE_PATTERNS: tuple[tuple[re.Pattern[str], str], ...] = (
    (re.compile(r"(Authorization:\s*Bearer\s+)[A-Za-z0-9_-]+", re.IGNORECASE), r"\1********"),
    (re.compile(r"\bRF-\d{6}\b"), "RF-******"),
    (re.compile(r"(token[\s:=]+)[A-Za-z0-9_:-]{20,}", re.IGNORECASE), r"\1********"),
)

_COOKIE_PATTERN = re.compile(r"(cookie\s*[:=]\s*)([^;\s]+)", re.IGNORECASE)


def redact(text: str) -> str:
    """Redacta credenciales, códigos y cookies presentes en ``text``."""
    for pattern, replacement in _SENSITIVE_PATTERNS:
        text = pattern.sub(replacement, text)
    return _COOKIE_PATTERN.sub(r"\1********", text)


class SensitiveFormatter(logging.Formatter):
    """Formatter que redacta credenciales y códigos antes de emitir el log."""

    def __init__(self, fmt: str | None = None) -> None:
        super().__init__(fmt=fmt)

    def format(self, record: logging.LogRecord) -> str:
        return redact(super().format(record))
