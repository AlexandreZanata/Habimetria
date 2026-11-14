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

Commit `6e20396c0534a97765fa8681222f4661e1ff6adb` was pushed to origin/dev;
[CI run 361](https://github.com/AlexandreZanata/Habimetria/actions/runs/43032457156)
passed backend and mobile-linux before C05.b began.

## C05.b — Service and query validation

Added typed ServicePlan/version/allowance records, a real PostgreSQL store and
the internal catalog service. Validate stable codes, canonical UUIDs, trimmed
names, monthly individual format, hosted-only finite allowances and exact
units/windows. Pagination uses bounded version-number keysets. SQL and service
validation both reject paid local/required-control capabilities. Reads reject
unknown terms/JSON fields rather than returning partially understood rights.

Appends allocate monotonically under the plan row lock; retirement serializes
with them and is idempotent/terminal. Failed writes leave no partial version
or consumed number. Committed terms survive caller edits and retirement;
server-only instants and integer precision are checked in real roundtrips.
Storage outages stay errors. Concurrent appends and append/retirement races
are exercised on disposable PostgreSQL, including unknown-future-format reads.

The catalog package has a new 95% coverage floor (measured 100% in its real-DB
suite). No catalog transport/mutation UI, StoreProduct mapping, price, provider
activation or entitlement is introduced. Staff authorization/audit exposure
remains C07/C41, and store mapping/readback remains C24.

Executed with the same disposable PostgreSQL 18.6-alpine database:

- `go test ./internal/service/commerce/catalog -count=1 -cover`: PASS,
  100% statement coverage of the catalog service/validation package.
- `GOTOOLCHAIN=go1.25.13 make verify test-integration cover` with
  TEST_DATABASE_URL: PASS, including all existing floors and new catalog 95%.
- `go test -race ./internal/service/commerce/catalog ./internal/store/postgres`
  with the same DB/toolchain: PASS.
- `make secrets`, diff checks, document links/new-file references and changed
  file line limits: PASS.

Final commit/ref and exact-commit CI are checked through Git/PR after push;
its own not-yet-known commit hash is not fabricated. No native/store/customer
gate is inferred by these internal services.

## Manual gates

G01-G12 remain UNVERIFIED. No store price, quantity, rollout, publication,
native purchase or production customer approval is inferred by this catalog.
