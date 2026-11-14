#!/usr/bin/env python3
"""Render deployment profiles using synthetic env only; never start containers."""
import json
import os
from pathlib import Path
import subprocess
import tempfile

ROOT = Path(__file__).resolve().parents[1]


def require(condition, message):
    if not condition:
        raise RuntimeError(message)


def example_environment():
    values = {"PATH": os.environ.get("PATH", "")}
    for line in (ROOT / "infra/.env.example").read_text().splitlines():
        if line.strip() and not line.lstrip().startswith("#"):
            key, value = line.split("=", 1)
            values[key.strip()] = value.strip()
    return values


def render(values, mounted=False, failure=False):
    command = ["docker", "compose", "--project-name", "habimetria-c06-fixture",
               "--env-file", os.devnull, "-f", str(ROOT / "infra/docker-compose.yml")]
    if mounted:
        command += ["-f", str(ROOT / "infra/docker-compose.commerce.yml")]
    command += ["config", "--format", "json"]
    result = subprocess.run(command, cwd=ROOT, env=values, text=True,
                            capture_output=True, timeout=30)
    if failure:
        require(result.returncode != 0, "missing explicit secret source was accepted")
        return None
    require(result.returncode == 0, "Compose render failed: " + result.stderr.strip())
    return json.loads(result.stdout)["services"]["api"]


def main():
    values = example_environment()
    baseline = render(values)
    flags = ("GOOGLE_PLAY_ENABLED", "APP_STORE_ENABLED", "COMMERCE_NEW_PURCHASES_ENABLED")
    require(all(baseline["environment"][key] == "false" for key in flags),
            "default profile enabled commerce")
    require(baseline["environment"]["COMMERCE_ENV"] == "", "default invented store environment")
    require(not baseline.get("volumes"), "default stack mounted credentials")
    render(values, mounted=True, failure=True)
    with tempfile.TemporaryDirectory(prefix="habimetria-c06-config-") as source:
        active = dict(values, APP_ENV="production", COMMERCE_ENV="sandbox",
                      HMAC_KEY="synthetic-config-key-0123456789abcdef",
                      COMMERCE_SECRETS_SOURCE_DIR=source,
                      COMMERCE_NEW_PURCHASES_ENABLED="true",
                      GOOGLE_PLAY_ENABLED="true", APP_STORE_ENABLED="true",
                      GOOGLE_PLAY_PACKAGE_NAME="test.synthetic.android",
                      GOOGLE_PLAY_SECRET_REF="sandbox/google_play/fixture.json",
                      APP_STORE_BUNDLE_ID="test.synthetic.ios", APP_STORE_APP_ID="1234567890",
                      APP_STORE_ISSUER_ID="123e4567-e89b-12d3-a456-426614174000",
                      APP_STORE_KEY_ID="TESTKEY123", APP_STORE_SECRET_REF="sandbox/app_store/fixture.p8")
        configured = render(active, mounted=True)
        keys = [key for key in active if key.startswith(("COMMERCE_", "GOOGLE_PLAY_", "APP_STORE_"))
                and key != "COMMERCE_SECRETS_SOURCE_DIR"]
        require(all(configured["environment"][key] == active[key] for key in keys),
                "configured provider identity/reference was not forwarded exactly")
        mounts = configured.get("volumes", [])
        require(len(mounts) == 1, "commerce overlay added unexpected mounts")
        mount = mounts[0]
        require(mount["type"] == "bind" and mount["source"] == source
                and mount["target"] == active["COMMERCE_SECRETS_DIR"]
                and mount.get("read_only") is True
                # Compose JSON omits false fields; an omitted normalized bool is false.
                and mount.get("bind", {}).get("create_host_path", False) is False,
                "credential mount is not explicit, read-only and non-creating")
        paused = render(dict(active, COMMERCE_NEW_PURCHASES_ENABLED="false"), mounted=True)
        require(paused["environment"]["COMMERCE_NEW_PURCHASES_ENABLED"] == "false"
                and all(paused["environment"][key] == active[key] for key in keys
                        if key != "COMMERCE_NEW_PURCHASES_ENABLED")
                and paused["volumes"] == configured["volumes"],
                "new-sale rollback changed provider recovery configuration")
        incident = render(dict(active, GOOGLE_PLAY_ENABLED="false"), mounted=True)
        require(incident["environment"]["GOOGLE_PLAY_ENABLED"] == "false"
                and incident["environment"]["APP_STORE_ENABLED"] == "true",
                "provider incident control affected the other store")
        require(not render(values).get("volumes"), "base rollback retained credential exposure")
    print("Commercial Compose defaults, namespaces, read-only mount and sale rollback: PASS")


if __name__ == "__main__":
    main()
