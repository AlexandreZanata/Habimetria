# C04 Execution — Legacy Ownership Boundaries

Started 2026-11-11 at `d97c57a`, branch dev. Scope C04 only; C05-C64 pending.
Apply the ordered [commit breakdown](commit_breakdown.md).

## C04.a — Private API ownership

The legacy guard now defaults to rejecting unclaimed resources and anonymous
Property creation even when the panel is disabled. Every private response is
no-store; missing authorization fails closed before dispatch. Claimed resources
still require the exact owner's current database-backed session; staff identity,
body/query ownership claims and worker headers confer no authority.

`LEGACY_API_MODE=owned` is the runtime/Compose/env-example default. Explicit
`unclaimed-development` is validated only with APP_ENV=development, local HTTP
verification origin and no panel. Production, HTTPS/non-local origins, unknown
modes and panel-enabled exceptions fail configuration validation. No schema or
automatic ownership assignment changes any existing collection.

New real-DB regressions check all 24 owner routes/HEAD across panel-on/off,
owned/unclaimed data and distinct actors, plus invalid scope/ID, missing
authorization and DB failure. Config tests pin defaults and rejected modes.
Executed with a disposable PostgreSQL 18.6-alpine database:

- `GOTOOLCHAIN=go1.25.13 make verify test-integration cover` with
  TEST_DATABASE_URL: PASS, all existing coverage floors retained.
- `go test -race ./internal/httpapi/panelweb ./internal/store/postgres` with
  the same DB/toolchain: PASS.
- `docker compose -f infra/docker-compose.yml config --quiet`: PASS.
- `make secrets`, `git diff --check`, document links/new-file references and
  changed-file limits: PASS.

Commit `aa3eb9d15f5c31b7ee912dc0961f24c91a8f6baa` was pushed to origin/dev.
[CI run 357](https://github.com/AlexandreZanata/Habimetria/actions/runs/43028320115)
passed backend and mobile-linux before C04.b began.

The inspection also found comparison Evidence references validated only by
existence. Writes now require the selected Room/Inspection, and scoped reads
reject legacy invalid Room/Evidence references rather than exposing them.
Real-DB tests prove foreign/wrong-side IDs cannot create or replace a tag.

## C04.b — Legacy, revocation and public compatibility

Added a real anonymous API journey only under validated development mode:
Property, Tenancy, Inspection, Room, multipart Evidence, seal, immutable PDF
attachment and deliberate Share. Switching to owned mode blocks collection
access without modifying signed bytes or assigning ownership; health, ready,
ping and authorized public recipients still work without a subscription.

Real-DB tests also cover suspension/reactivation/logout, fresh-session recovery,
current staff permission and role changes, and the distinction between a
Report-specific staff grant and private owner API access. Development exceptions
never open claimed resources. Forged worker/owner headers are denied by C04.a;
no Go job executor exists and no service-wide job bypass was introduced.

The [operator runbook](../../04-backend/legacy_ownership_runbook.md) defines
inventory, independently verified titleholder approval, NULL-only transactional
assignment/audit, immutable artifact checks and safe exposure rollback. A
synthetic rehearsal preserves actual original/PDF bytes and frozen DB rows,
proves bootstrap never claims records, denies other/staff access and rejects
reassignment. No schema change or production record assignment was executed.

Executed with the same disposable PostgreSQL 18.6-alpine database:

- `GOTOOLCHAIN=go1.25.13 make verify test-integration cover` with
  TEST_DATABASE_URL: PASS; existing floors unchanged.
- `go test -race ./internal/httpapi/panelweb -run
  'Test(UnclaimedDevelopment|DevelopmentException|LegacyOwnerSession|ReportCapability|ReviewedLegacy)'
  -count=1` with that DB/toolchain: PASS.
- `make secrets`, diff checks, new-file references, changed document links and
  the 200-line file limit: PASS.

Final commit/ref and exact-commit CI are checked through Git/PR after push;
its own not-yet-known commit hash is not fabricated here.

## Manual gates

G01-G12 remain UNVERIFIED. Production legacy assignment requires an operator's
verified ownership evidence and explicit reviewed action; code delivery cannot
claim those approvals or rewrite SEALED artifacts.
