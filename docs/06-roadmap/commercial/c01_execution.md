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
- Commit `3a41539` pushed to origin/dev.
- [GitHub CI run 345](https://github.com/AlexandreZanata/Habimetria/actions/runs/43016613560):
  backend and mobile-linux PASS on that exact commit.

## C01.b — Legacy retirement and independent signing protection

- Removed unused PaymentPort/Stripe/Asaas adapters and their obsolete tests,
  configuration/default price and HTTP payment-error mapping.
- Added APP_ENV development/production validation. Production and non-local
  or HTTPS advertised origins require configured 32+ byte signing keys;
  invalid environments/origins fail before API startup. Local development
  stays usable. Retired payment environment settings have no effect.
- Preserved deprecated JSON compatibility. Documented configuration migration
  and preserving existing signing keys/history in
  [payments](../../03-architecture/payments.md).
- Added `backend/internal/service/seal_integration_test.go`: real stored bytes,
  independently computed digest/HMAC, server/device-time separation, missing/
  empty/sealed behavior and surfaced database failures.
- Retiring previously covered adapters exposed insufficient service coverage
  without DB tests. Added real seal-service proof rather than lowering the
  55% floor. Run coverage with TEST_DATABASE_URL as already configured in CI.

Executed checks after retirement:

- `TEST_DATABASE_URL=<disposable PostgreSQL 18.6> make verify`: PASS, including
  all configured integration tests; dedicated `make test-integration` also PASS.
- `TEST_DATABASE_URL=<disposable PostgreSQL 18.6> GOTOOLCHAIN=go1.25.13 make cover`:
  PASS with the unchanged coverage floor.
- `go test -race -run 'TestSeal(ServiceIntegrity|WithoutPayment)Integration'
  ./internal/service ./internal/httpapi/panelweb` with that DB: PASS.
- `docker compose -f infra/docker-compose.yml config --quiet`: PASS.
- `make secrets`, changed-file size/reference checks and diff checks: PASS.

Commit `7f992cc` was pushed to origin/dev;
[GitHub CI run 347](https://github.com/AlexandreZanata/Habimetria/actions/runs/43017823202)
passed backend and mobile-linux on that exact commit.
The complete C01 is code-verified. C02-C64 remain pending. This does not
implement hosted billing/entitlement or approve production activation.

## Manual gates

Native device, visual PDF, iOS and user-session acceptance remain unverified
and user-owned. G01-G12 are not approved by these backend/code checks.
