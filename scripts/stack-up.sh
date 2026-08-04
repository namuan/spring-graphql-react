#!/usr/bin/env bash
#
# stack-up.sh - build and start the full showroom stack under Podman.
#
# 1. Builds images from the vehicle-config-service, showroom-orchestrator
#    and frontend Containerfiles.
# 2. Creates a pod named $POD_NAME (default: showroom) whose containers
#    share localhost networking, publishing $WEB_PORT (default: 8080) to
#    the frontend's container port 80. Backend ports remain private to the pod.
# 3. Starts Postgres 17-alpine with database/user/password 'showroom'
#    (overridable), a named volume, and a pg_isready healthcheck that is
#    waited on before any service is started.
# 4. Starts vehicle-config-service on 8081 with its datasource pointing at
#    localhost Postgres, then showroom-orchestrator on 8082 with
#    VEHICLE_SERVICE_URL=http://localhost:8081, then the nginx frontend.
#
# Each component is polled until it answers before the next one starts.
# On failure the script prints pod/container/log diagnostics; on SIGINT or
# SIGTERM it tears the partial stack down. Errors leave the stack in place
# for inspection. Tear down cleanly with scripts/stack-down.sh.

set -euo pipefail
# shellcheck source=scripts/lib.sh
source "$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)/lib.sh"

# --- traps -------------------------------------------------------------------
on_error() {
    local rc=$?
    log "stack-up.sh failed (rc=${rc}); leaving the partial stack in place for inspection"
    diagnostics "stack-up.sh failed rc=${rc}"
    exit "${rc}"
}
trap on_error ERR

on_signal() {
    log "interrupted; tearing the stack down"
    set +e
    "${SCRIPT_DIR}/stack-down.sh"
    exit 130
}
trap on_signal INT TERM

# --- preflight ---------------------------------------------------------------
if ! command -v podman >/dev/null 2>&1; then
    die "podman is not installed or not on PATH"
fi
if ! podman info >/dev/null 2>&1; then
    die "podman is not usable - is the Podman machine running? (try: podman machine start)"
fi
if podman pod exists "${POD_NAME}" 2>/dev/null; then
    die "pod '${POD_NAME}' already exists - run scripts/stack-down.sh first"
fi

# --- build -------------------------------------------------------------------
log "==> building images (vehicle-config-service, showroom-orchestrator, frontend)"
build_image "${VEHICLE_IMAGE}" "${ROOT_DIR}/vehicle-config-service"
build_image "${ORCHESTRATOR_IMAGE}" "${ROOT_DIR}/showroom-orchestrator"
build_image "${FRONTEND_IMAGE}" "${ROOT_DIR}/frontend"

# --- volume + pod --------------------------------------------------------------
if ! podman volume exists "${VOLUME_NAME}" 2>/dev/null; then
    log "==> creating volume ${VOLUME_NAME}"
    podman volume create "${VOLUME_NAME}"
fi

log "==> creating pod ${POD_NAME} (localhost share; publishes only ${WEB_PORT}:80 on 127.0.0.1)"
podman pod create \
    --name "${POD_NAME}" \
    --publish "127.0.0.1:${WEB_PORT}:${FRONTEND_CONTAINER_PORT}"

# --- postgres ------------------------------------------------------------------
log "==> starting postgres (${POSTGRES_IMAGE}, database=${POSTGRES_DB}, user=${POSTGRES_USER})"
podman run -d \
    --name "${DB_CONTAINER}" \
    --pod "${POD_NAME}" \
    --volume "${VOLUME_NAME}:/var/lib/postgresql/data" \
    --env "POSTGRES_DB=${POSTGRES_DB}" \
    --env "POSTGRES_USER=${POSTGRES_USER}" \
    --env "POSTGRES_PASSWORD=${POSTGRES_PASSWORD}" \
    --health-cmd "pg_isready -U ${POSTGRES_USER} -d ${POSTGRES_DB}" \
    --health-interval 3s \
    --health-timeout 3s \
    --health-retries 20 \
    --health-start-period 15s \
    "${POSTGRES_IMAGE}"
wait_container_health "${DB_CONTAINER}" || {
    diagnostics "postgres did not become healthy"
    exit 1
}

# --- vehicle-config-service ------------------------------------------------------
log "==> starting ${VEHICLE_CONTAINER} (datasource jdbc:postgresql://localhost:${POSTGRES_PORT}/${POSTGRES_DB})"
podman run -d \
    --name "${VEHICLE_CONTAINER}" \
    --pod "${POD_NAME}" \
    --env "DB_HOST=localhost" \
    --env "DB_PORT=${POSTGRES_PORT}" \
    --env "DB_NAME=${POSTGRES_DB}" \
    --env "DB_USERNAME=${POSTGRES_USER}" \
    --env "DB_PASSWORD=${POSTGRES_PASSWORD}" \
    "${VEHICLE_IMAGE}"
wait_internal_ready "${VEHICLE_CONTAINER}" "${VEHICLE_PORT}" "/actuator/health" || {
    diagnostics "vehicle-config-service did not become ready"
    exit 1
}

# --- showroom-orchestrator -------------------------------------------------------
log "==> starting ${ORCHESTRATOR_CONTAINER} (VEHICLE_SERVICE_URL=http://localhost:${VEHICLE_PORT})"
podman run -d \
    --name "${ORCHESTRATOR_CONTAINER}" \
    --pod "${POD_NAME}" \
    --env "VEHICLE_SERVICE_URL=http://localhost:${VEHICLE_PORT}" \
    "${ORCHESTRATOR_IMAGE}"
wait_internal_ready "${ORCHESTRATOR_CONTAINER}" "${ORCHESTRATOR_PORT}" "/actuator/health" || {
    diagnostics "showroom-orchestrator did not become ready"
    exit 1
}

# --- frontend ---------------------------------------------------------------------
log "==> starting ${FRONTEND_CONTAINER} (nginx, container port ${FRONTEND_CONTAINER_PORT})"
podman run -d \
    --name "${FRONTEND_CONTAINER}" \
    --pod "${POD_NAME}" \
    "${FRONTEND_IMAGE}"
wait_ready "${FRONTEND_CONTAINER}" "${WEB_PORT}" "/health" || {
    diagnostics "frontend did not become ready"
    exit 1
}

# --- done -------------------------------------------------------------------------
log "==> stack is up"
log "    UI:        http://127.0.0.1:${WEB_PORT} (GraphQL proxied by nginx to :${ORCHESTRATOR_PORT})"
log "    GraphQL:   http://127.0.0.1:${WEB_PORT}/graphql (public BFF boundary)"
log "    Backends:  private to the pod on :${ORCHESTRATOR_PORT} and :${VEHICLE_PORT}"
log "    Postgres:  localhost:${POSTGRES_PORT}/${POSTGRES_DB} inside pod (volume ${VOLUME_NAME})"
log "    Tear down: scripts/stack-down.sh"
