# Implementar autenticación de un usuario

Implementa el mecanismo aprobado de emparejamiento.

Incluye:
- Alta segura sin registro público.
- Credencial revocable por dispositivo.
- Caducidad y límite de intentos del emparejamiento.
- Protección adecuada de credenciales almacenadas.
- Revocación y recuperación documentadas.
- Limitación local de peticiones.
- Logs sin tokens, cookies ni secretos.

No incluyas claves compartidas en código o APK.
No expongas públicamente el servidor.

Prueba credenciales inválidas, revocadas y acceso no autorizado.