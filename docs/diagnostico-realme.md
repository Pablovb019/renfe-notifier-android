# Guía de Diagnóstico para realme GT Neo 2 (Renfe Notifier)

> Alcance: **diagnóstico dirigido al dispositivo realme** (realme GT Neo 2 / realme UI 4, Android 13). Complementa `docs/real-config-plan.md` (VM), `docs/validation.md` (pruebas backend/android) y `docs/notificaciones-fcm.md` (FCM/Doze/realme UI). Esta guía no sustituye ni duplica esos documentos; enlaza a la evidencia que ya existe.

## 1. Cuándo usar esta guía

Úsala si en el realme la app no avisa, el autocompletado no sugiere, o la sincronización con el backend falla. Resuelve por capas (más baratas primero, coste 0 €).

## 2. Capa 1 — App Android: instalada y versión correcta

1. Comprueba que la app instalada es `v0.1.10` (código 11):
   - Ajustes → Aplicaciones → Renfe Notifier → info (versión/número de versión).
   - O desde el APK instalado: `adb shell dumpsys package com.pablovb019.notifier | findstr versionCode` (compilación local, ver `docs/android-build.md`).
2. Si no está o es vieja: reinstala desde la release (ver `docs/instalacion-apk.md`, correspondiente al prompt de instalación/actualización manual).
3. Verifica la firma del APK instalado contra el keystore conservado (ver `docs/keystore.md`); evita firmas distintas que provoquen rechazo de actualización.

## 3. Capa 2 — Notificaciones (FCM) y Doze en realme UI

El flujo de notificaciones y las restricciones de batería de realme (Doze, ahorro de energía, batería en segundo plano) están documentados con el diseño y la evidencia de pruebas en:

- `docs/notificaciones-fcm.md` — canales, payload, comportamiento bajo Doze/realme UI.
- `docs/notificaciones-fcm.md` §Doze/realme — pasos de configuración de batería en el dispositivo.

Pasos básicos en el realme:
1. Ajustes → Batería → activar "No optimizar" / gestión manual para Renfe Notifier (quita la optimización agresiva).
2. Ajustes → Notificaciones → permitir notificaciones y el canal de alta prioridad.
3. Comprobar que el backend tiene FCM habilitado y el `google-services.json` correcto (config real: `docs/real-config-plan.md`).

## 4. Capa 3 — Conectividad con el backend

1. Desde el realme, abre en el navegador la IP/puerto del backend (p. ej. `https://IP_VM:8000/health`) y comprueba un `200 OK`.
2. Si el dispositivo no alcanza la VM: revisa firewall/IAP y la pila wireguard/Tailscale (ver `docs/security.md` y `docs/ci-cd.md`).
3. Verifica en la app que el estado de conexión/última comprobación se muestra correctamente (pantalla de seguimiento, `docs/validation.md`).

## 5. Capa 4 — Flujo DWR con Renfe y seguimientos

Si el backend no responde o no hay datos de Renfe, aplica el diagnóstico del flujo DWR (problema de token `generateId` resuelto y verificado, evidencia en):

- `docs/renfe-dwr-diagnosis/` — diagnóstico, causa raíz (regex de token) y fix desplegado.
- `docs/validation.md` — 190 pruebas backend OK (bloque D, mediciones en la VM real, evidencia).

Verifica en la VM (acceso solo con aprobación, ver `docs/real-config-plan.md`):
```bash
sudo systemctl status renfe-notifier-backend.service   # activo, sin errores de scheduler
tail -n 50 /var/log/renfe-notifier/renfe-notifier.log   # ciclos de comprobación OK
python -m app.cli version                               # versión del esquema
```

## 6. Escalado del problema

Si tras las capas 1–4 sigue fallando, recopila evidencia sin inventar nada y regístrala en `PROGRESS.md`:
- Captura de pantalla del estado de conexión en la app.
- Log del backend (`journalctl -u renfe-notifier-backend.service -n 200`).
- Versión de APK y de backend (commit/`version`).
- Mediciones de recursos sólo si existe `docs/measurements.json` para la VM (no medir de nuevo salvo que el usuario lo autorice).

> Regla: **no inventar**. Todo diagnóstico registrado en `PROGRESS.md` debe reflejar evidencia real capturada (hash, logs, pantallas). Ver `docs/validation.md` para el formato de evidencia verificado.
