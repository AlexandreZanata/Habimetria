# C08 Qualification Baseline — 2026-11-11

This supplements the historical `060d968` plan baseline; it does not replace
past evidence or establish production readiness. C08 started at `8ef969e`.
Disposable infrastructure was delivered at `2d4c421`; the final fixture delivery
and exact-head CI are linked from the PR/Git history and [execution](c08_execution.md).

## Measured setup

Local Linux/amd64, Docker Compose v2.27.0, Go 1.25.13 (the cached toolchain with
working coverage tooling), PostgreSQL exactly 18.6-alpine. The isolated profile
pins Garage v2.5.0, Mailpit v1.27.4 and HAProxy 3.2-alpine by registry digest.
Dependencies use an internal network; fixed-target gateway ports bind random
127.0.0.1 addresses. Per-run credentials and config are temporary/synthetic.
The existing API/development deployment is neither started nor modified.

## Executed checks

- `GOTOOLCHAIN=go1.25.13 make verify-infra`: regular vet/format/test,
  real-DB integration and all existing coverage floors, plus five Python
  isolation/lifecycle tests, actual SMTP capture/recipient rejection and
  real child-failure/SIGTERM resource cleanup.
- Verbose uncached `TestMultiOwnerMigrationPreservationIntegration`,
  `TestPrivateCollectionStorageIsolationIntegration` and
  `TestPrivateS3RoundtripIntegration`: all PASS with endpoints configured.
- `go test -race -count=1 ./internal/testutil/collectionfixture
  ./internal/storage/s3store ./internal/store/postgres` and targeted panel
  storage-isolation race test: PASS with disposable dependencies.
- Explicit S3_TEST_ENDPOINT=http://127.0.0.1:1 roundtrip: expected FAILURE,
  not SKIP/PASS; configured dependency outages propagate and cleanup runs.
- `make secrets`, diff checks, file bounds and documentation references: PASS.

The runner performs project-label checks after every teardown. No labelled
containers, volumes or networks remained after the recorded normal, failure
and handled-signal checks. Abrupt host/daemon loss remains a manual cleanup
scenario; this proof does not claim finally blocks execute after SIGKILL.

## Observed boundaries

Two explicit owners and unclaimed legacy data are seeded at schema 0004.
Current migration replay preserves core rows, Manifest/signature, Evidence/PDF
bytes, device claims versus server instants and resource ownership. It creates
no customer enrollment, Report availability or ServicePlan. Injected SQL failure
rolls back DDL, data and the migration ledger. A second database stays separate.

Actual panel/router sessions with private S3 reject cross-owner, anonymous,
staff-without-grant and legacy access; exact Report grants/revocation behave
on subsequent requests. Public verification omits fixture identities/object
locators. Synthetic PDFs demonstrate byte integrity, not visual PDF quality.

The new scenario found post-seal Room creation. Single/template writes now
require DRAFT while holding the seal's Inspection row lock; real lock-queue
checks deny writes after a concurrent seal. Template failure rolls back its
partial Rooms. Missing-parent, duplicate, DRAFT and idempotent behavior pass.

## Still unverified

G01-G12 remain UNVERIFIED. C09-C64 are pending. SMTP capture is not application
outbox or real email delivery. No provider purchase/sandbox, mobile account
session, cloud enrollment/snapshot/restore, real customer migration, production
backup/recovery/load/monitoring or legal/retention approval is established.
Device, visual PDF, native iOS and user-session acceptance remain owner-owned.
All local features remain free; these fixtures do not activate subscriptions.
