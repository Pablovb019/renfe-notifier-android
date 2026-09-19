"""CLI administrativa local (único mecanismo de gestión de emparejamiento y backup).

Se ejecuta únicamente dentro de la VM/terminal del administrador; la API web
no expone operaciones administrativas. Nunca imprime tokens ni hashes.
"""

import argparse
import asyncio
import hashlib
import sys
from pathlib import Path

from app.config import Settings
from app.db.connection import (
    backup_database,
    connect,
    get_database_version,
    verify_database_integrity,
)
from app.db.migrations import apply_migrations
from app.pairing.database import PairingRepository
from app.pairing.service import PairingService

_EPILOG = "Acceso administrativo exclusivamente local; no exponer el servidor."


def build_parser() -> argparse.ArgumentParser:
    parser = argparse.ArgumentParser(
        prog="renfe-notifier-cli",
        description="Gestión local de emparejamientos del backend.",
        epilog=_EPILOG,
    )
    sub = parser.add_subparsers(dest="command", required=True)

    code = sub.add_parser("pairing-code", help="Genera un código temporal para la app")
    code.add_argument("--generate", action="store_true", help="Genera y muestra un código nuevo")

    devices = sub.add_parser("devices", help="Gestiona los dispositivos emparejados")
    devices.add_argument("--list", action="store_true", help="Lista los dispositivos")
    devices.add_argument("--revoke", metavar="DEVICE_ID", help="Revoca un dispositivo")
    devices.add_argument("--revoke-all", action="store_true", help="Revoca todos los dispositivos")

    sub.add_parser(
        "test-notification",
        help="Envía una notificación de prueba al primer dispositivo activo con token FCM",
    )

    backup_cmd = sub.add_parser(
        "backup", help="Crea backup consistente de la base de datos (WAL-aware)"
    )
    backup_cmd.add_argument("--output", "-o", metavar="PATH", help="Ruta del archivo de backup")

    restore_cmd = sub.add_parser("restore", help="Restaura base de datos desde backup")
    restore_cmd.add_argument(
        "--input", "-i", metavar="PATH", required=True, help="Ruta del backup a restaurar"
    )
    restore_cmd.add_argument(
        "--force", action="store_true", help="Sobrescribe BD existente sin confirmar"
    )

    verify_cmd = sub.add_parser("verify", help="Verifica integridad de la base de datos")
    verify_cmd.add_argument(
        "--path", metavar="PATH", help="Ruta de la BD a verificar (por defecto la configurada)"
    )

    version_cmd = sub.add_parser("version", help="Muestra versión de esquema aplicada")
    version_cmd.add_argument(
        "--path", metavar="PATH", help="Ruta de la BD (por defecto la configurada)"
    )

    return parser


def main(argv: list[str] | None = None) -> int:
    args = build_parser().parse_args(argv)
    settings = Settings()
    service = PairingService(
        PairingRepository(settings.database_path),
        code_ttl_s=settings.pairing_code_ttl_s,
        max_attempts=settings.pairing_max_attempts,
    )
    service.initialize()

    if args.command == "pairing-code" and args.generate:
        code = service.create_pairing_code()
        print(f"Código de emparejamiento (caduca en {int(settings.pairing_code_ttl_s)}s): {code}")
        print("Introduce este código en la app. No lo comparta ni lo guarde en repositorios.")
        return 0

    if args.command == "devices":
        if args.revoke_all:
            revoked = service.revoke_all()
            print(f"Dispositivos revocados: {revoked}")
            return 0
        if args.revoke:
            ok = service.revoke(args.revoke)
            print(
                f"Dispositivo revocado: {args.revoke}"
                if ok
                else f"No existe dispositivo activo: {args.revoke}"
            )
            return 0
        if args.list:
            devices = service.list_devices()
            if not devices:
                print("No hay dispositivos emparejados.")
                return 0
            for device in devices:
                state = "activo" if device.is_active else f"revocado {device.revoked_at:%Y-%m-%d}"
                print(
                    f"{device.device_id} | {device.device_name} | creado {device.created_at:%Y-%m-%d %H:%M} | {state}"
                )
            return 0

    if args.command == "test-notification":
        if not settings.fcm_project_id:
            print(
                "FCM no configurado (RENFE_NOTIFIER_FCM_PROJECT_ID ausente).",
                file=sys.stderr,
            )
            return 1
        active_device = next((d for d in service.list_devices() if d.is_active), None)
        if active_device is None:
            print("No hay un dispositivo activo.", file=sys.stderr)
            return 1
        fcm_token = active_device.fcm_token
        if fcm_token is None:
            print("El dispositivo no tiene token FCM.", file=sys.stderr)
            return 1
        from app.notifications.fcm import AutoTokenProvider, FcmNotificationSender

        sender = FcmNotificationSender(
            project_id=settings.fcm_project_id,
            token_provider=AutoTokenProvider(),
            timeout_s=settings.fcm_timeout_s,
            default_ttl_s=settings.fcm_default_ttl_s,
            package=settings.fcm_app_package,
        )
        try:
            message_id = asyncio.run(sender.send_test(fcm_token=fcm_token))
        except Exception as error:  # noqa: BLE001 - motivo sin exponer datos sensibles
            print(f"Fallo al enviar la notificación de prueba: {error}", file=sys.stderr)
            return 1
        print(f"Notificación de prueba enviada: {message_id}")
        return 0

    if args.command == "backup":
        output = Path(args.output) if args.output else settings.database_path.with_suffix(".db.bak")
        backup_database(settings.database_path, output)
        sha = hashlib.sha256(output.read_bytes()).hexdigest()
        version = get_database_version(settings.database_path)
        print(f"Backup creado: {output} (SHA256: {sha}, versión esquema: {version})")
        return 0

    if args.command == "restore":
        input_path = Path(args.input)
        if not input_path.exists():
            print(f"Backup no encontrado: {input_path}", file=sys.stderr)
            return 1
        target = settings.database_path
        if target.exists() and not args.force:
            print(
                f"La BD destino ya existe: {target}. Usa --force para sobrescribir.",
                file=sys.stderr,
            )
            return 1
        ok, msg = verify_database_integrity(input_path)
        if not ok:
            print(f"Backup corrupto: {msg}", file=sys.stderr)
            return 1
        backup_database(input_path, target)
        apply_migrations(connect(target))
        version = get_database_version(target)
        print(f"Restaurado: {target} (versión esquema: {version})")
        return 0

    if args.command == "verify":
        target = Path(args.path) if args.path else settings.database_path
        ok, msg = verify_database_integrity(target)
        if ok:
            print(f"OK: {target} - {msg}")
            return 0
        print(f"FALLO: {target} - {msg}", file=sys.stderr)
        return 1

    if args.command == "version":
        target = Path(args.path) if args.path else settings.database_path
        version = get_database_version(target)
        if version is None:
            print(f"Sin versión de esquema (tabla schema_migrations ausente): {target}")
            return 1
        print(f"Versión esquema aplicada: {version}")
        return 0

    print("Comando no reconocido. Usa --help para ver las opciones.", file=sys.stderr)
    return 2


if __name__ == "__main__":
    raise SystemExit(main())
