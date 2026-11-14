"""Explicit host allowlist and per-run synthetic configuration."""
import os
from pathlib import Path
import secrets

ROOT = Path(__file__).resolve().parents[2]
HOST_KEYS = ("PATH", "HOME", "XDG_RUNTIME_DIR", "DOCKER_HOST", "DOCKER_CONTEXT",
             "DOCKER_CONFIG", "GOTOOLCHAIN", "GOCACHE", "GOPATH", "GOMODCACHE")


def host_environment(source=None):
    source = os.environ if source is None else source
    return {key: source[key] for key in HOST_KEYS if key in source}


def configuration(directory):
    values = host_environment()
    values["FIXTURE_RPC_SECRET"] = secrets.token_hex(32)
    path = Path(directory) / "garage.toml"
    path.write_text('metadata_dir = "/data/meta"\ndata_dir = "/data/objects"\n'
                    'db_engine = "sqlite"\nreplication_factor = 1\n'
                    'rpc_bind_addr = "127.0.0.1:3901"\n'
                    'rpc_public_addr = "127.0.0.1:3901"\n'
                    f'rpc_secret = "{values["FIXTURE_RPC_SECRET"]}"\n'
                    '[s3_api]\ns3_region = "garage"\napi_bind_addr = "0.0.0.0:3900"\n')
    path.chmod(0o600)
    values.update(FIXTURE_DB_PASSWORD=secrets.token_hex(24),
                  FIXTURE_S3_ACCESS_KEY="GK" + secrets.token_hex(16),
                  FIXTURE_S3_SECRET_KEY=secrets.token_hex(32),
                  FIXTURE_S3_BUCKET="fixture-" + secrets.token_hex(8),
                  FIXTURE_PROXY_CONFIG=str(ROOT / "infra/test/haproxy.cfg"),
                  FIXTURE_GARAGE_CONFIG=str(path))
    return values


def test_environment(values, ports):
    env = {key: value for key, value in values.items() if key in HOST_KEYS}
    dsn = f"postgres://fixture:{values['FIXTURE_DB_PASSWORD']}@127.0.0.1:{ports['postgres']}/fixture?sslmode=disable"
    env.update(TEST_DATABASE_URL=dsn, DATABASE_URL=dsn,
               S3_TEST_ENDPOINT=f"http://127.0.0.1:{ports['objects']}",
               S3_TEST_REGION="garage", S3_TEST_BUCKET=values["FIXTURE_S3_BUCKET"],
               S3_TEST_ACCESS_KEY=values["FIXTURE_S3_ACCESS_KEY"],
               S3_TEST_SECRET_KEY=values["FIXTURE_S3_SECRET_KEY"],
               MAIL_TEST_SMTP=f"127.0.0.1:{ports['smtp']}",
               MAIL_TEST_HTTP=f"http://127.0.0.1:{ports['mail']}")
    return env
