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

Commit `34273f1e3ac302424fbd643cabbe89f32995df9e` was pushed to origin/dev;
[CI run 365](https://github.com/AlexandreZanata/Habimetria/actions/runs/43037371690)
passed backend and mobile-linux before C06.b began.

## C06.b — Isolated activation and exposure rollback

Added the internal [activation service](../../../backend/internal/service/commerce/activation/README.md)
and [operational runbook](../../04-backend/commercial_rollout.md). Provider setup
matches exact app/store/environment context, opens credentials only on demand
and keeps new-sale disablement separate from delivery/restore/reconciliation/
notification preparation. Local/account/recipient paths never use this gate.

Mounted credentials use pinned directory handles, namespace identity checks,
private regular files, nonempty/1 MiB bounds and bounded closable readers.
Symlink aliases, wrong contexts and exposed files are rejected. Credential
readers and error messages redact supplied data while preserving error causes.
Tests cover inactive startup, wrong namespaces, cancellation, per-store rollback,
credential bounds, atomic file replacement and post-close failure propagation.
Measured activation statement coverage: 85.4%; new enforced floor: 85%.

The optional Compose overlay adds an explicit read-only mount without creating
the source. A repeatable synthetic render check verifies default isolation,
exact field forwarding and independent exposure/incident rollback; backend CI
runs it. No actual provider client, native purchase, endpoint, entitlement or
production secret provisioning is claimed. No schema or sealed bytes change.

Executed with the same disposable PostgreSQL 18.6-alpine database:

- `go test ./internal/service/commerce/activation -count=1 -cover`: PASS,
  85.4% statement coverage; filesystem failure-cleanup paths are not falsely
  claimed as exhaustively injected. The new package floor is 85%.
- `GOTOOLCHAIN=go1.25.13 make verify test-integration cover` with
  TEST_DATABASE_URL: PASS, including all existing floors/configuration 95%.
- `go test -race ./internal/config ./internal/service/commerce/activation`
  with Go 1.25.13: PASS.
- `make verify-commerce-config`: PASS; synthetic profile rendering only,
  including normalized Compose JSON where false bind fields may be omitted.
- `make secrets`, diff checks, document links/new-file references and changed
  file line limits: PASS.

Final commit/ref and exact-commit CI are checked through Git/PR after push;
its own not-yet-known commit hash is not fabricated here.

## Manual gates

G01-G12 remain UNVERIFIED. No provider identity, credential permission,
store transaction, production secret mount or sale activation is approved here.
