# C06 Execution — Safe Commercial Configuration

Started 2026-11-11 at `5eee0a1`, branch dev. Scope C06 only; C07-C64 pending.
Ordered sub-slices follow [commit breakdown](commit_breakdown.md).

## C06.a — Conditional configuration and environment parity

Defined the [configuration contract](../../04-backend/commercial_configuration.md)
before implementing explicit independent provider/new-sale flags, app/store
identities, sandbox/production namespaces and mounted-file secret references.
Defaults leave both providers and new sales disabled with no publisher data.
Missing inactive configuration never blocks local-only startup; activation
requires that provider's complete structurally valid identifiers/reference.
Production commerce requires hardened APP_ENV=production; sandbox may use a
hardened server without being mistaken for production transactions.

Validation rejects unknown flags, mixed reference namespaces/stores, inline
credentials/traversal and invalid identities without echoing supplied values.
Updated FromEnv, Config.Validate, env example and Compose forwarding together.
Native/store adapter verification and publication remain later work, not a
claim established by configuration. C06.b adds activation/rollback isolation.

Executed against disposable PostgreSQL 18.6-alpine:

- `go test ./internal/config -count=1 -cover`: PASS, 100% statement coverage;
  raised the existing configuration coverage floor from 85% to 95%.
- `GOTOOLCHAIN=go1.25.13 make verify test-integration cover` with
  TEST_DATABASE_URL: PASS, including existing ownership, catalog and privacy
  regressions and all coverage floors.
- `go test -race ./internal/config` with Go 1.25.13: PASS.
- `docker compose -f infra/docker-compose.yml config --quiet`: PASS.
- `make secrets`, diff checks, document links/new-file references and changed
  file line limits: PASS.

Commit/ref and exact-commit CI are checked after push; this file does not
invent its own not-yet-known commit hash. C06.b remains pending.

## Manual gates

G01-G12 remain UNVERIFIED. No provider identity, credential permission,
store transaction, production secret mount or sale activation is approved here.
