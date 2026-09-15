#!/usr/bin/env bash
# deploy_backend.sh — Script de despliegue backend para VM (paso 29)
# Se ejecuta EN LA VM mediante gcloud compute ssh --tunnel-through-iap.
# NO contiene secretos; la autenticación es OIDC + IAP.
#
# Flujo transaccional:
# 1. Backup SQLite (.backup atómico, coherente con WAL)
# 2. Checkout del commit SHA exacto
# 3. Migraciones (python -m app.db.migrate)
# 4. docker compose up -d
# 5. Health check (GET /api/v1/diagnostics/health, 30s)
# Rollback automático en cualquier fallo.

set -euo pipefail

# =============================================================================
# Configuración (ajustar según entorno VM)
# =============================================================================
PROJECT_DIR="/home/ubuntu/renfe-notifier-android"
DATA_DIR="/data"
BACKUP_DIR="${DATA_DIR}/backups"
DB_PATH="${DATA_DIR}/renfe_notifier.db"
COMPOSE_FILE="${PROJECT_DIR}/docker-compose.yml"
HEALTH_ENDPOINT="http://localhost:8000/api/v1/diagnostics/health"
HEALTH_TIMEOUT=30
HEALTH_INTERVAL=2

# =============================================================================
# Utilidades
# =============================================================================
log() { echo "[$(date '+%Y-%m-%d %H:%M:%S')] $*"; }
die() { log "ERROR: $*"; exit 1; }

require_cmd() {
    command -v "$1" >/dev/null 2>&1 || die "Comando requerido no encontrado: $1"
}

# =============================================================================
# Validación de pre-requisitos
# =============================================================================
require_cmd sqlite3
require_cmd docker
require_cmd docker compose
require_cmd curl
require_cmd git

mkdir -p "${BACKUP_DIR}"

# =============================================================================
# Paso 1: Backup consistente de SQLite (WAL-aware)
# =============================================================================
log "Paso 1/5: Backup de base de datos..."
TIMESTAMP=$(date +%Y%m%d_%H%M%S)
BACKUP_FILE="${BACKUP_DIR}/backup_${TIMESTAMP}.db"

if [[ ! -f "${DB_PATH}" ]]; then
    die "Base de datos no encontrada: ${DB_PATH}"
fi

sqlite3 "${DB_PATH}" ".backup '${BACKUP_FILE}'"
log "Backup creado: ${BACKUP_FILE}"

# Verificar integridad del backup
sqlite3 "${BACKUP_FILE}" "PRAGMA integrity_check" | grep -q "^ok$" \
    || die "Backup corrupto (integrity_check falló)"

BACKUP_SHA256=$(sha256sum "${BACKUP_FILE}" | cut -d' ' -f1)
log "Backup verificado (SHA256: ${BACKUP_SHA256})"

# =============================================================================
# Paso 2: Actualización de código (checkout SHA exacto)
# =============================================================================
log "Paso 2/5: Actualizando código a commit ${DEPLOY_SHA}..."
cd "${PROJECT_DIR}"

# Guardar commit actual para posible rollback
PREVIOUS_SHA=$(git rev-parse HEAD)
log "Commit anterior: ${PREVIOUS_SHA}"

git fetch origin main --quiet
git checkout "${DEPLOY_SHA}" --quiet || die "No se pudo hacer checkout de ${DEPLOY_SHA}"
log "Código actualizado a $(git rev-parse --short HEAD)"

# =============================================================================
# Paso 3: Migraciones SQLite
# =============================================================================
log "Paso 3/5: Ejecutando migraciones..."
cd "${PROJECT_DIR}/backend"

# Usar el python del entorno virtual de la VM
if [[ -f ".venv/bin/python" ]]; then
    PYTHON=".venv/bin/python"
else
    PYTHON="python3"
fi

${PYTHON} -m app.db.migrate || {
    log "Migraciones fallaron. Iniciando rollback..."
    cd "${PROJECT_DIR}"
    git checkout "${PREVIOUS_SHA}" --quiet
    sqlite3 "${DB_PATH}" ".restore '${BACKUP_FILE}'"
    docker compose -f "${COMPOSE_FILE}" up -d --quiet-pull
    die "Rollback completado: código y BD restaurados"
}
log "Migraciones aplicadas correctamente"

# =============================================================================
# Paso 4: Reinicio de contenedor Docker
# =============================================================================
log "Paso 4/5: Reiniciando contenedor..."
cd "${PROJECT_DIR}"
docker compose -f "${COMPOSE_FILE}" up -d --quiet-pull || {
    log "docker compose up falló. Iniciando rollback..."
    git checkout "${PREVIOUS_SHA}" --quiet
    sqlite3 "${DB_PATH}" ".restore '${BACKUP_FILE}'"
    docker compose -f "${COMPOSE_FILE}" up -d --quiet-pull
    die "Rollback completado"
}
log "Contenedor reiniciado"

# =============================================================================
# Paso 5: Health Check con reintentos
# =============================================================================
log "Paso 5/5: Health check (${HEALTH_TIMEOUT}s máx)..."
ELAPSED=0
while (( ELAPSED < HEALTH_TIMEOUT )); do
    if curl -fsS --max-time 5 "${HEALTH_ENDPOINT}" >/dev/null 2>&1; then
        log "Health check OK: servicio respondiendo 200"
        log "=== DESPLIEGUE EXITOSO ==="
        exit 0
    fi
    sleep "${HEALTH_INTERVAL}"
    ELAPSED=$((ELAPSED + HEALTH_INTERVAL))
done

# Health check falló → rollback automático completo
log "Health check falló tras ${HEALTH_TIMEOUT}s. Iniciando rollback automático..."
cd "${PROJECT_DIR}"
git checkout "${PREVIOUS_SHA}" --quiet
sqlite3 "${DB_PATH}" ".restore '${BACKUP_FILE}'"
docker compose -f "${COMPOSE_FILE}" up -d --quiet-pull
log "Rollback automático completado: código, BD y servicio restaurados"
exit 1