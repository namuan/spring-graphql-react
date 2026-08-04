# Aureline vehicle showroom

A complete vehicle configurator built as a small layered system:

```text
Browser
  -> nginx + React/Vite (public port 8080)
  -> Spring for GraphQL orchestrator (private port 8082)
  -> Spring Boot vehicle configuration service (private port 8081)
  -> PostgreSQL 17
```

The frontend only calls `/graphql`. The orchestrator is a thin BFF that calls
the domain service over REST. The domain service owns all persistence and price
validation.

## Requirements

- Podman 5 or newer, with `podman machine` running on macOS
- Java 21 and Maven 3.8+ for running JVM tests locally
- Node.js 22+ and npm for frontend and Playwright tests

No Compose provider is required. The scripts use Podman pods directly.

## Run the application

```bash
./scripts/stack-up.sh
open http://127.0.0.1:8080
```

Stop the containers while preserving PostgreSQL data:

```bash
./scripts/stack-down.sh
```

Delete the application data explicitly:

```bash
PURGE_DATA=1 ./scripts/stack-down.sh
```

If port 8080 is occupied, select another public port:

```bash
WEB_PORT=18080 ./scripts/stack-up.sh
```

The backend ports are private to the pod. The public GraphQL endpoint is
`http://127.0.0.1:8080/graphql`.

## Run the outside-in E2E test

```bash
./scripts/e2e.sh
```

The script builds all three application images, starts PostgreSQL and the real
services in an isolated Podman pod, installs Chromium, and runs Playwright from
the browser boundary. It selects a seeded vehicle, saves a configuration, then
reads that configuration back through GraphQL to verify the PostgreSQL row and
option relationship. The E2E pod and volume are unique to each run and cannot
delete normal development data.

Use another host port if needed:

```bash
WEB_PORT=18080 ./scripts/e2e.sh
```

Failure screenshots, traces, and the HTML report are written under `e2e/`.

## Run tests separately

```bash
(cd vehicle-config-service && mvn test)
(cd showroom-orchestrator && mvn test)
(cd frontend && npm ci && npm test && npm run build)
(cd e2e && npm ci && npm run typecheck)
```

Test coverage includes domain price and ownership rules, REST contracts,
GraphQL composition and error mapping, frontend states and interactions, and
the full browser-to-database path.

## Frontend development

Start the Podman stack, then run Vite in another terminal:

```bash
./scripts/stack-up.sh
cd frontend
npm ci
npm run dev
```

Vite proxies `/graphql` through the public BFF boundary on port 8080. For a
non-default stack port, set `VITE_GRAPHQL_URL` and enable the orchestrator's
optional development CORS with `SHOWROOM_CORS_ALLOWED_ORIGINS`.

## Main API operations

The GraphQL schema supports:

- `models` and `model(id)` for the vehicle catalogue
- `createConfiguration(input)` for validated, server-priced configurations
- `configuration(id)` for retrieving a saved configuration

Prices are integer pence values (`*PriceCents`) throughout the API and
database. A configuration total is calculated by the domain service from the
model base price, trim adjustment, and selected options.

## Production note

This repository is an architectural reference application. The supplied
runtime binds to loopback. Before exposing it publicly, add identity and
ownership for saved configurations, rate limiting, GraphQL query-depth limits,
TLS, and managed secret injection.

## Credentials

The `showroom` PostgreSQL database/user/password in `scripts/lib.sh` are
local development defaults only. Override `POSTGRES_PASSWORD` (and the other
`POSTGRES_*` variables) in any shared or deployed environment.

## License

MIT — see [LICENSE](LICENSE).
