"""Configuración del backend cargada y validada desde el entorno."""

from pathlib import Path
from typing import Literal, Self

from pydantic import Field, model_validator
from pydantic_settings import BaseSettings, SettingsConfigDict


class Settings(BaseSettings):
    """Opciones necesarias para arrancar el backend mínimo.

    Los valores de desarrollo permiten probar el proceso solo en localhost.
    Producción exige una clave de aplicación proporcionada mediante el entorno.
    """

    model_config = SettingsConfigDict(
        env_file=".env",
        env_file_encoding="utf-8",
        env_prefix="RENFE_NOTIFIER_",
        extra="ignore",
    )

    environment: Literal["development", "test", "production"] = "development"
    host: str = Field(default="127.0.0.1", description="Interfaz de escucha de Uvicorn")
    port: int = Field(default=8000, ge=1, le=65535, description="Puerto de escucha")
    debug: bool = Field(default=False, description="Habilita documentación local")
    database_path: Path = Field(
        default=Path("data/renfe-notifier.db"),
        description="Ruta SQLite; siempre fuera de directorios reemplazables",
    )
    reminder_initial_delay_s: float = Field(
        default=60.0, gt=0, description="Retraso del primer recordatorio tras detectar el episodio"
    )
    reminder_interval_s: float = Field(
        default=300.0, gt=0, description="Separación mínima entre recordatorios del mismo evento"
    )
    reminder_max_attempts: int = Field(
        default=3, ge=1, description="Tope de recordatorios por evento antes de darlo por entregado"
    )
    scheduler_enabled: bool = Field(
        default=False,
        description="Arranca el planificador y la entrega de avisos como tareas del lifespan",
    )
    scheduler_interval_s: float = Field(
        default=60.0, gt=0, description="Intervalo del ciclo de búsqueda del planificador"
    )
    delivery_interval_s: float = Field(
        default=20.0, gt=0, description="Intervalo entre ciclos de entrega de avisos pendientes"
    )
    delivery_batch: int = Field(
        default=20, ge=1, description="Máximo de avisos debidos procesados por ciclo de entrega"
    )
    pairing_code_ttl_s: float = Field(
        default=600.0, gt=0, description="Caducidad del código de emparejamiento en segundos"
    )
    pairing_max_attempts: int = Field(
        default=3, ge=1, description="Límite de reclamaciones de un mismo código"
    )
    rate_limit_window_s: float = Field(
        default=60.0, gt=0, description="Ventana del límite de peticiones de emparejamiento"
    )
    rate_limit_max_requests: int = Field(
        default=10, ge=1, description="Máximo de peticiones de emparejamiento por ventana e IP"
    )
    fcm_project_id: str | None = Field(
        default=None, min_length=1, description="ID del proyecto Firebase para FCM HTTP v1"
    )
    fcm_timeout_s: float = Field(default=10.0, gt=0, description="Timeout por petición HTTP a FCM")
    fcm_default_ttl_s: int = Field(
        default=300,
        gt=0,
        le=2_419_200,
        description="TTL por defecto de los mensajes FCM (declaración, no garantía)",
    )
    fcm_app_package: str | None = Field(
        default=None, min_length=1, description="Paquete Android restringido opcional en FCM"
    )
    secret_key: str | None = Field(default=None, min_length=32, repr=False)

    @model_validator(mode="after")
    def require_secret_in_production(self) -> Self:
        """Evita que un proceso de producción arranque sin material secreto."""
        if self.environment == "production" and not self.secret_key:
            raise ValueError("RENFE_NOTIFIER_SECRET_KEY es obligatoria en producción")
        return self
