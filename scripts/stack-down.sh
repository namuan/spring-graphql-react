#!/usr/bin/env bash
#
# stack-down.sh - tear down the showroom Podman stack.
#
# Removes the pod (and therefore every container in it). The Postgres volume
# is preserved by default. Set PURGE_DATA=1 to explicitly delete it. Images
# are kept for faster subsequent bring-ups.

set -euo pipefail
# shellcheck source=scripts/lib.sh
source "$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)/lib.sh"

if podman pod exists "${POD_NAME}" 2>/dev/null; then
    log "removing pod ${POD_NAME} (and its containers)"
    podman pod rm -f "${POD_NAME}"
else
    log "pod ${POD_NAME} not found; nothing to remove"
fi

if [[ "${PURGE_DATA:-0}" == "1" ]] && podman volume exists "${VOLUME_NAME}" 2>/dev/null; then
    log "removing volume ${VOLUME_NAME}"
    podman volume rm -f "${VOLUME_NAME}"
elif podman volume exists "${VOLUME_NAME}" 2>/dev/null; then
    log "keeping volume ${VOLUME_NAME} (set PURGE_DATA=1 to remove it)"
else
    log "volume ${VOLUME_NAME} not found; nothing to remove"
fi

log "stack is down"
