# Disposable Commercial Integration Fixtures

C08 qualifies test infrastructure; no production deployment or sale is enabled.
See [operations](commercial_operations.md), [database](database.md) and
[execution evidence](../06-roadmap/commercial/c08_execution.md).

## Isolation contract

`make verify-infra` creates a unique Compose project on a local Unix-socket
Docker daemon. PostgreSQL is exactly `18.6-alpine`; the S3-compatible Garage and
Mailpit images are versioned and digest-pinned. There is no API deployment,
host directory containing customer data, shared volume, fixed container name,
external network, relay or production service configuration in this profile.
Only a digest-pinned HAProxy gateway publishes random 127.0.0.1 host ports.
It forwards four fixed destinations into the internal service network; only
the gateway also joins the project ingress bridge. Dependency containers have
no external route or published port.

Each execution generates random fixture credentials and a private temporary
Garage configuration. Application, database, cloud, store, SMTP and Compose
variables from the caller are discarded. Docker selection is accepted only
for local Unix sockets; remote contexts/hosts fail before container operations.
The command forwards only documented host/toolchain settings and generated
TEST variables. It does not load repository `.env` or print credential values.

Readiness and explicit SMTP/API probes must pass before the test command runs.
Mailpit accepts only `example.test` recipients, with no forwarding/relay and
version checking disabled. Its HTTP API confirms the exact synthetic message.
This proves the mail sandbox, not an implemented application mail/outbox flow
(C09). S3 tests exercise actual adapter bytes, presigned access, anonymous and
tampered-signature denial, and deletion. Configured unavailable services fail;
only an absent optional TEST endpoint skips an ordinary unit-suite invocation.

Normal exit, command failure, startup failure and handled SIGINT/SIGTERM run
project-specific teardown and verify no labelled containers, networks or
volumes remain. SIGKILL/daemon/host loss cannot execute finally blocks: retain
the printed nonsecret project ID and use the exact cleanup command in the
runner README. Cleanup failure is an error, never silently reported as a pass.

## Commands and files

- [Infrastructure runner and file index](../../infra/test/README.md).
- `backend/internal/storage/s3store/private_integration_test.go` exercises the
  actual private object store without a production credential fallback.
- `backend/internal/storage/s3store/s3_test.go` retains independent mock coverage.
- `make verify` remains the regular check. `make verify-infra` additionally
  provisions the complete disposable dependencies and runs real integration.
- CI runs the same runner. Production-equivalent backups, real email delivery,
  provider sandboxes, cloud enrollment/restore and manual gates remain unverified.

## Upstream contracts

The [Garage quick start](https://garagehq.deuxfleurs.fr/documentation/quick-start/)
documents the versioned single-node/default-bucket setup used only here.
[Mailpit Docker](https://mailpit.axllent.org/docs/install/docker/) and
[runtime options](https://mailpit.axllent.org/docs/configuration/runtime-options/)
define capture ports and recipient restrictions. These fixtures do not change
the existing development/production storage provider or qualify production
redundancy, licensing, capacity or operational retention.

[HAProxy configuration](https://docs.haproxy.org/3.2/configuration.html) defines
the TCP-only gateway. Internal-only Docker networks cannot publish host ports;
see [Docker networking](https://docs.docker.com/compose/how-tos/networking/).

## Multi-owner and migration qualification

The reusable [S3 fixture](../../backend/internal/testutil/s3fixture/README.md)
uses only explicit loopback TEST settings. The
[collection fixture](../../backend/internal/testutil/collectionfixture/README.md)
creates synthetic DRAFT/SEALED collections using real services/storage. It
accepts explicit owner IDs or an unclaimed legacy Property; it never claims
existing data. Originals/Reports are tiny synthetic documents, not real photos
or visually qualified PDFs. Each test deletes only its generated object keys.

Migration tests seed two owners and NULL-owned legacy data at schema 0004,
apply/replay every current migration, compare complete core rows and object
digests, verify no automatic enrollment/grants/catalog seed, and inject a failed
migration to prove DDL/ledger rollback. A second isolated database remains
untouched. HTTP tests use the same fixture with real private S3 and current
panel sessions, proving owner/staff/anonymous scope, exact Report grants and
revocation, sealed-write rejection and unchanged bytes/Manifests.

- `backend/internal/httpapi/panelweb/infrastructure_fixture_test.go` wires either
  local or explicit real S3 storage into the same actual router.
- `backend/internal/httpapi/panelweb/storage_isolation_integration_test.go`
  exercises the multi-owner/legacy HTTP scenario against S3.
- [Measured qualification baseline](../06-roadmap/commercial/qualification_baseline.md)
  records executed commands and limits. No native/store/production gate is
  inferred from these synthetic results.

The real-storage scenario exposed post-seal Room creation. PostgreSQL
`rooms.go` now uses `room_write.go` to require DRAFT under an Inspection row
lock for both single creation and atomic template insertion. This shares the
seal lock, keeps unknown-parent/duplicate behavior and rejects writes after
seal with the existing ErrSealed/HTTP 409 contract; it does not rewrite history.
`backend/internal/store/postgres/room_write_test.go` proves seal/write ordering,
DRAFT behavior, template rollback and unchanged sealed Room inventory.
