# C05 Execution — Immutable Monthly ServicePlan Catalog

Started 2026-11-11 at `e236879`, branch dev. Scope C05 only; C06-C64 pending.
Ordered sub-slices follow [commit breakdown](commit_breakdown.md).

## C05.a — Schema and constraints

Defined the [catalog contract](../../04-backend/service_plan_catalog.md) before
adding migration 0006. ServicePlan identity is stable; terms are append-only
monthly/individual version records. Registered hosted capabilities require
finite positive integer allowances with exact units/windows. Local features,
required account controls, unknown fields, ambiguous units and nonmonthly
offers are rejected. No offer, quantity, price or provider product is seeded.

DB triggers protect identity/history against update/delete/truncate and
serialize insertion with terminal retirement. Existing owned/unclaimed core
records are not changed. StoreProduct mappings remain C24; nothing is exposed
for sale. Code-level service/query validation is C05.b.

Added an isolated PostgreSQL test fixture and real-DB schema regressions for
populated upgrade/replay, invalid values, unique versions and immutable history.
Executed against disposable PostgreSQL 18.6-alpine:

- `GOTOOLCHAIN=go1.25.13 make verify test-integration cover` with
  TEST_DATABASE_URL: PASS; existing coverage floors retained, including the
  live-schema privacy inventory with both catalog tables registered as system
  configuration and catalog-label handling. No retention duration is invented.
- `go test -race ./internal/store/postgres -run TestServicePlan -count=1`
  with that DB/toolchain: PASS.
- `make secrets`, diff checks, document links/new-file references and changed
  file line limits: PASS.

Commit/ref and exact-commit CI are checked after push; this file does not
invent its own not-yet-known commit hash. C05.b is still pending.

## Manual gates

G01-G12 remain UNVERIFIED. No store price, quantity, rollout, publication,
native purchase or production customer approval is inferred by this catalog.
