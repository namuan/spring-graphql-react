#!/usr/bin/env bash
#
# e2e.sh - run the outside-in browser suite against the Podman stack.
#
# 1. Brings the stack up (scripts/stack-up.sh).
# 2. Installs the e2e Node package and the Chromium browser for Playwright.
# 3. Runs `npx playwright test` against BASE_URL (default
#    http://127.0.0.1:$WEB_PORT).
# 4. ALWAYS tears the stack down again (trap EXIT), no matter the outcome.
#
# The script owns the full lifecycle, so it starts from a clean slate:
# any leftovers from a previous crashed run are removed first.
#
# Extra arguments are forwarded to `npx playwright test`, e.g.:
#   scripts/e2e.sh --headed
#   scripts/e2e.sh tests/showroom.spec.ts --grep reload

set -euo pipefail

# Never reuse or delete the normal development pod/volume. Each E2E process
# owns an isolated stack and explicitly purges only that stack's test data.
POD_NAME="${E2E_POD_NAME:-showroom-e2e-$$}"
VOLUME_NAME="${E2E_VOLUME_NAME:-${POD_NAME}-pgdata}"
export POD_NAME VOLUME_NAME
# shellcheck source=scripts/lib.sh
source "$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)/lib.sh"

E2E_DIR="${ROOT_DIR}/e2e"
BASE_URL="${BASE_URL:-http://127.0.0.1:${WEB_PORT}}"

cleanup() {
    log "e2e: tearing the stack down"
    set +e
    PURGE_DATA=1 "${SCRIPT_DIR}/stack-down.sh"
    set -e
}
trap cleanup EXIT
trap 'exit 130' INT TERM

# Fresh stack: a previous crashed run must not leak into this one.
PURGE_DATA=1 "${SCRIPT_DIR}/stack-down.sh" || true

"${SCRIPT_DIR}/stack-up.sh"

log "e2e: installing e2e package in ${E2E_DIR}"
if [[ -f "${E2E_DIR}/package-lock.json" ]]; then
    ( cd "${E2E_DIR}" && npm ci --no-audit --no-fund )
else
    ( cd "${E2E_DIR}" && npm install --no-audit --no-fund )
fi

log "e2e: ensuring the Chromium browser is installed"
( cd "${E2E_DIR}" && npx playwright install chromium )

log "e2e: running Playwright tests against ${BASE_URL}"
export BASE_URL
export WEB_PORT
( cd "${E2E_DIR}" && npx playwright test "$@" )

log "e2e: all tests passed"
