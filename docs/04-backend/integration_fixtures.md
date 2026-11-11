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
