# C01 Execution — Remove Seal/Payment Coupling

Started 2026-11-11 from `c7487ab`, branch dev. Scope: C01 only, with ordered
sub-slices from [commit breakdown](commit_breakdown.md). No subscription or
cloud entitlement implementation, store activation or manual approval.

## C01.a — Payment-independent sealing

- SealInput/SealService and API wiring no longer depend on PaymentPort.
- HTTP accepts and ignores legacy string `payment_proof`; absent proof and
  arbitrary unpaid proof follow the same seal path without provider requests.
- Existing ownership, empty-data validation, signing, terminality and frozen
  Manifest behavior remain. No signing bytes or database schema changes.
- Added `backend/internal/httpapi/panelweb/seal_integration_test.go`: real
  owner-scoped requests, no-proof/legacy-proof cases, concurrent 201/409,
  unauthorized 404, empty 422, invalid JSON 400 and unchanged Manifest after retry.

Executed checks:

- `make verify`: PASS.
- `GOTOOLCHAIN=go1.25.13 make cover`, `make secrets`, documentation links,
  changed-file line limits and `git diff --check`: PASS.
- `TEST_DATABASE_URL=<isolated PostgreSQL 18.6-alpine> make test-integration`:
  PASS, including the new real-DB seal suite and existing panel/document tests.
- `cd app && ./gradlew :shared:jvmTest --tests vault.local.LocalSealTest`:
  PASS (local snapshot signing, concurrency, invalid input and tamper checks).
- Exact-commit GitHub CI is inspected after push.

The sub-slice's commit/push identity is available in Git history and is
recorded explicitly in the next update rather than guessed before commit.

## C01.b — Pending

Retire unused provider code/configuration and the old default price. Preserve
production signing-key validation independently of billing; document deployment
migration and retain ignored JSON compatibility for old clients.

## Manual gates

Native device, visual PDF, iOS and user-session acceptance remain unverified
and user-owned. G01-G12 are not approved by these backend/code checks.
