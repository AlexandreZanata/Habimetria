# C08 Execution — Disposable Infrastructure and Migration Fixtures

Started 2026-11-11 at `8ef969e`, branch dev. Scope C08 only; C09-C64 pending.
Ordered sub-slices follow [commit breakdown](commit_breakdown.md).

## C08.a — Disposable dependencies

Defined the [fixture contract](../../04-backend/integration_fixtures.md) before
code. Added PostgreSQL 18.6-alpine, digest-pinned Garage/Mailpit and a fixed-target
HAProxy gateway. Dependencies have an internal network without external routes;
only the gateway publishes random loopback ports on a project ingress bridge.
The Docker daemon does not publish ports on internal-only networks; this was
observed and the gateway topology verified rather than relaxing dependency
isolation. Production/development storage configuration is unchanged.

The Python runner uses per-run random credentials/private temporary config,
explicit host-variable allowlisting, local Unix-socket Docker validation and
no repository `.env`. Readiness and actual SMTP/API capture precede checks;
non-example.test recipients are rejected, without mail relay/forwarding.
Application signup/outbox/delivery remains C09, not claimed by this sandbox.

Replaced implicit localhost S3 probing/skipping with explicit TEST-only fixture
configuration. Actual adapter tests compare stored/presigned bytes, reject
anonymous/tampered access and verify deletion. Unconfigured optional S3 tests
skip honestly; configured unavailable integration fails. Existing unit mock
coverage stays independent. CI invokes the same `make verify-infra` runner.

Normal/failing commands and handled signals remove only labelled project
containers, volumes and both networks, then verify absence. Command process
groups are bounded/stopped; cleanup failure cannot become success. Documentation
covers abrupt SIGKILL/daemon-loss recovery without global pruning.

Executed on synthetic disposable services:

- `GOTOOLCHAIN=go1.25.13 make verify-infra`: PASS, including `make verify`,
  real PostgreSQL/S3 integration and all existing coverage floors.
- Python isolation/lifecycle tests: PASS, including poisoned caller settings,
  rendered topology, remote-daemon denial and failure propagation.
- Real command exit 17 and SIGTERM cleanup proofs: PASS.
- Explicit verbose/no-cache private S3 roundtrip: PASS (not skipped).
- `make secrets`, diff checks, document links/new-file references and changed
  file line limits: PASS.

Commit `2d4c421acf8b9cd11e00acc0670dcdb260a7b921` was pushed to origin/dev;
[CI run 373](https://github.com/AlexandreZanata/Habimetria/actions/runs/43048198306)
passed backend (including the new stack/lifecycle proofs) and mobile-linux
before C08.b began.

## C08.b — Multi-owner fixtures and measured baseline

Added reusable explicit TEST-only S3 and real-service collection fixtures;
shared panel test wiring now accepts local or real private storage. Two owners
and an unclaimed collection contain DRAFT/SEALED data, Room/Evidence/Finding,
Manifest, Report and Occurrence. Every generated object has bounded teardown.
Upgrading from schema 0004 and replaying current migrations preserves core rows,
private bytes, signed anchors, ownership and distinct device/server instants.
There is no automatic enrollment/grant/offer. SQL failure injection proves DDL,
data and migration-ledger rollback; a second database remains independent.

Real-storage HTTP tests prove exact owner/staff/anonymous/legacy boundaries,
Report availability/revocation, public minimization and immutable PDF retries.
They exposed post-seal Room creation (HTTP 201). Corrected single/template writes
with a DRAFT guard under the seal's Inspection row lock. Tests now require HTTP
409 and unchanged rows/bytes. Actual seal/write queues prove stale writes are
denied; template failure leaves no partial Rooms. Existing missing-parent,
duplicate, DRAFT and idempotent behavior remains covered. No migration or
historical data rewrite is needed for this store-boundary correction.

Updated the [qualification baseline](qualification_baseline.md) with measured
commands and explicit limits. Executed on disposable PostgreSQL/Garage/Mailpit:

- `GOTOOLCHAIN=go1.25.13 make verify-infra`: PASS, including `make verify`,
  integration, coverage, SMTP proof and real lifecycle cleanup.
- Verbose/no-cache migration, private collection and S3 roundtrip checks: PASS.
- Race detector for collection fixtures, S3 adapter, PostgreSQL and targeted
  panel S3 isolation: PASS.
- Configured unavailable S3 check: expected failure (not skipped), cleanup PASS.
- `make secrets`, diff checks, documentation links/new-file references and
  changed-file line bounds: PASS.

Final commit/ref and exact-commit CI are checked after push; no self-hash is
invented here. C09-C64 remain pending.

## Manual gates

G01-G12 remain UNVERIFIED. No production-equivalent recovery, real mail,
store, native device/iOS, legal/privacy or owner approval is claimed.
