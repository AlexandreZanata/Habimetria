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

Final commit/ref and exact-commit CI are checked after push; no self-hash is
invented. Multi-owner/legacy fixtures and baseline refresh remain C08.b.

## Manual gates

G01-G12 remain UNVERIFIED. No production-equivalent recovery, real mail,
store, native device/iOS, legal/privacy or owner approval is claimed.
