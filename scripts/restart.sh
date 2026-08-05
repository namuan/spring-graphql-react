#!/usr/bin/env bash
#
# restart.sh - tear down and rebuild the showroom Podman stack in one command.
#
# Equivalent to running scripts/stack-down.sh then scripts/stack-up.sh.
# PostgreSQL data is preserved by default (set PURGE_DATA=1 to wipe it).
# All stack-up.sh / stack-down.sh variables (WEB_PORT, POD_NAME, ...) apply.

set -euo pipefail
# shellcheck source=scripts/lib.sh
source "$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)/lib.sh"

log "==> restarting stack (${POD_NAME})"
"${SCRIPT_DIR}/stack-down.sh"
"${SCRIPT_DIR}/stack-up.sh"
log "==> restart complete"
