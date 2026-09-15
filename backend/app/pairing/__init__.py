"""Emparejamiento de dispositivos para el único usuario del sistema.

Implementa el flujo aprobado en ``docs/security.md``: código OTP temporal
generado por CLI y reclamado una única vez por la app, que recibe un token de
dispositivo. Las credenciales se persisten solo como hash SHA-256.
"""
