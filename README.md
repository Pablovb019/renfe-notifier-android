# Renfe Notifier Android

This project contains an Android native app (Kotlin + Jetpack Compose) and a lightweight Python backend to replace the existing Renfe notifier bot (Telegram). The goal is to provide reliable train availability notifications via Firebase Cloud Messaging (FCM) while maintaining zero operating cost.

## Structure
- `backend/` – Python 3.12 backend (Renfe checker, scheduler, FCM sender, API).
- `app/` – Android source code (to be added in later hits).

## License
MIT