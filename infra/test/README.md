# Disposable integration stack

Normative [contract](../../docs/04-backend/integration_fixtures.md) and
[C08 evidence](../../docs/06-roadmap/commercial/c08_execution.md).
Requires Python 3.10+, Docker Engine/Compose v2 and Go 1.25 with local Unix socket.
No `.env`, installed Python dependency or production credential is required.

From repository root:

```sh
python3 -m unittest discover -s infra/test -p '*_test.py'
GOTOOLCHAIN=go1.25.13 make verify-infra
python3 infra/test/run.py -- make verify test-integration cover
python3 infra/test/run.py -- python3 -c 'raise SystemExit(17)'
```

The last command intentionally returns 17 after verifying real cleanup. Each
run prints its generated `habimetria-test-<uuid>` project name, no credentials.
SIGINT/SIGTERM stop the command process group and clean project resources;
SIGKILL or daemon/host loss requires manual cleanup. List resources using
that exact printed project ID:

```sh
docker ps -aq --filter label=com.docker.compose.project=<printed-project-id>
docker volume ls -q --filter label=com.docker.compose.project=<printed-project-id>
docker network ls -q --filter label=com.docker.compose.project=<printed-project-id>
```

Stop/remove only the returned containers, then the returned volumes/network.
Never run a global prune. A normal runner exit verifies all three lists empty.
Runner variables: generated TEST_DATABASE_URL/DATABASE_URL, S3_TEST_ENDPOINT,
S3_TEST_REGION/BUCKET/ACCESS_KEY/SECRET_KEY and MAIL_TEST_SMTP/HTTP.
Inherited host settings are limited to PATH, HOME, XDG_RUNTIME_DIR, Docker
selection/configuration and GOTOOLCHAIN/GOCACHE/GOPATH/GOMODCACHE. Remote Docker
is rejected. No application/cloud/store/mail settings pass from the caller.

## Files

- `compose.yml`: PostgreSQL 18.6, digest-pinned Garage/Mailpit, private network,
  internal services, a fixed-target TCP gateway on random loopback ports and
  project-only volumes.
- `haproxy.cfg`: four fixed TCP destinations; no general forwarding proxy.
- `environment.py`: host allowlist, fresh secrets/private config and test env.
- `stack.py`: local daemon guard, bounded Compose readiness/ports and teardown.
- `run.py`: bounded child process group, handled interrupts and cleanup.
- `mail_probe.py`: real synthetic SMTP capture/API body and recipient rejection.
- `isolation_test.py`: poisoned config, rendered topology and remote-daemon denial.
- `verify_lifecycle.py`: real Docker cleanup proof after child failure and SIGTERM.
- `lifecycle_test.py`: startup/interruption/command/cleanup failure propagation.
