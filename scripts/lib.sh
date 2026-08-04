#!/usr/bin/env bash
#
# lib.sh - shared configuration and helpers for the showroom Podman stack.
#
# Consumed by scripts/stack-up.sh, scripts/stack-down.sh and scripts/e2e.sh.
# Everything runs on Podman directly: no Docker, no docker-compose,
# no podman-compose. All containers live in a single pod, so they share
# the pod network namespace and reach each other on 127.0.0.1.

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ROOT_DIR="$(cd "${SCRIPT_DIR}/.." && pwd)"

# --- overridable configuration (environment wins) ---------------------------
POD_NAME="${POD_NAME:-showroom}"
WEB_PORT="${WEB_PORT:-8080}"
POSTGRES_IMAGE="${POSTGRES_IMAGE:-docker.io/library/postgres:17-alpine}"
POSTGRES_DB="${POSTGRES_DB:-showroom}"
POSTGRES_USER="${POSTGRES_USER:-showroom}"
POSTGRES_PASSWORD="${POSTGRES_PASSWORD:-showroom}"
VOLUME_NAME="${VOLUME_NAME:-${POD_NAME}-pgdata}"
IMAGE_TAG="${IMAGE_TAG:-latest}"
WAIT_TIMEOUT="${WAIT_TIMEOUT:-180}"

VEHICLE_IMAGE="${VEHICLE_IMAGE:-showroom/vehicle-config-service:${IMAGE_TAG}}"
ORCHESTRATOR_IMAGE="${ORCHESTRATOR_IMAGE:-showroom/showroom-orchestrator:${IMAGE_TAG}}"
FRONTEND_IMAGE="${FRONTEND_IMAGE:-showroom/frontend:${IMAGE_TAG}}"

DB_CONTAINER="${POD_NAME}-db"
VEHICLE_CONTAINER="${POD_NAME}-vehicle"
ORCHESTRATOR_CONTAINER="${POD_NAME}-orchestrator"
FRONTEND_CONTAINER="${POD_NAME}-frontend"

# Container-side ports. Only the frontend is published to the host; backend
# readiness is checked from the Postgres container inside the shared pod.
VEHICLE_PORT=8081
ORCHESTRATOR_PORT=8082
FRONTEND_CONTAINER_PORT=80
POSTGRES_PORT=5432

# --- logging -----------------------------------------------------------------
log() { printf '[showroom] %s\n' "$*" >&2; }

die() {
    log "ERROR: $*"
    exit 1
}

# --- diagnostics --------------------------------------------------------------
# Best-effort state dump; must never fail the caller.
diagnostics() {
    local reason="${1:-unspecified failure}"
    {
        printf '\n[showroom] ================= DIAGNOSTICS (%s) =================\n' "${reason}"
        echo '[showroom] podman ps -a:'
        podman ps -a --format 'table {{.Names}}\t{{.Status}}\t{{.Ports}}' || true
        for c in "${DB_CONTAINER}" "${VEHICLE_CONTAINER}" "${ORCHESTRATOR_CONTAINER}" "${FRONTEND_CONTAINER}"; do
            if podman container exists "${c}" 2>/dev/null; then
                echo "[showroom] ---- logs: ${c} (tail 100) ----"
                podman logs --tail 100 "${c}" 2>&1 | sed 's/^/  /' || true
            fi
        done
        echo '[showroom] ================= END DIAGNOSTICS ========================'
    } >&2 || true
}

# --- waits -------------------------------------------------------------------
# wait_container_health NAME : poll podman health status until healthy.
wait_container_health() {
    local name="$1"
    local timeout="${WAIT_TIMEOUT}"
    local deadline=$((SECONDS + timeout))
    log "waiting for container health: ${name} (timeout ${timeout}s)"
    while (( SECONDS < deadline )); do
        local status
        status="$(podman inspect --format '{{if .State.Health}}{{.State.Health.Status}}{{else}}none{{end}}' "${name}" 2>/dev/null || echo none)"
        case "${status}" in
            healthy)
                log "${name} is healthy"
                return 0
                ;;
            unhealthy)
                log "ERROR: ${name} reported unhealthy"
                return 1
                ;;
            *)
                # still starting (status: starting / none) - keep polling
                ;;
        esac
        sleep 2
    done
    log "ERROR: timeout waiting for ${name} to become healthy"
    return 1
}

# wait_ready NAME HOST_PORT HEALTH_PATH : require a successful response from a
# public health endpoint.
wait_ready() {
    local name="$1"
    local port="$2"
    local path="$3"
    local timeout="${WAIT_TIMEOUT}"
    local deadline=$((SECONDS + timeout))
    local url="http://127.0.0.1:${port}${path}"
    log "waiting for ${name} at ${url} (timeout ${timeout}s)"
    while (( SECONDS < deadline )); do
        if curl -fsS -o /dev/null --connect-timeout 3 --max-time 5 "${url}" 2>/dev/null; then
            log "${name} is ready (HTTP success on ${path})"
            return 0
        fi
        sleep 2
    done
    log "ERROR: timeout waiting for ${name} at ${url}"
    return 1
}

# wait_internal_ready NAME PORT HEALTH_PATH : check a service from inside the
# pod, without publishing its port to the host. The Postgres Alpine image
# provides BusyBox wget and shares the pod's localhost network namespace.
wait_internal_ready() {
    local name="$1"
    local port="$2"
    local path="$3"
    local timeout="${WAIT_TIMEOUT}"
    local deadline=$((SECONDS + timeout))
    local url="http://127.0.0.1:${port}${path}"
    log "waiting for ${name} inside the pod at ${url} (timeout ${timeout}s)"
    while (( SECONDS < deadline )); do
        if podman exec "${DB_CONTAINER}" wget -q -O /dev/null -T 5 "${url}" 2>/dev/null; then
            log "${name} is ready"
            return 0
        fi
        sleep 2
    done
    log "ERROR: timeout waiting for ${name} at ${url}"
    return 1
}

# --- build -------------------------------------------------------------------
# build_image IMAGE CONTEXT_DIR : build from the directory's Containerfile
# (Dockerfile fallback if only that exists).
build_image() {
    local image="$1"
    local dir="$2"
    local dockerfile=""
    if [[ -f "${dir}/Containerfile" ]]; then
        dockerfile="${dir}/Containerfile"
    elif [[ -f "${dir}/Dockerfile" ]]; then
        dockerfile="${dir}/Dockerfile"
        log "no Containerfile in ${dir}; using Dockerfile"
    else
        die "no Containerfile or Dockerfile found in ${dir}"
    fi
    log "==> building ${image} from ${dockerfile}"
    podman build --pull=missing -t "${image}" -f "${dockerfile}" "${dir}"
}
