#!/usr/bin/env python3
"""Synthetic render-only regression for the explicit account worker overlay."""
import json
import os
from pathlib import Path
import subprocess
import tempfile

ROOT = Path(__file__).resolve().parents[1]


def render(values, overlay=False, api=False, failure=False):
    args = ["docker", "compose", "--project-name", "account-mail-config-fixture",
            "--env-file", os.devnull, "-f", str(ROOT / "infra/docker-compose.yml")]
    if overlay:
        args += ["-f", str(ROOT / "infra/docker-compose.account-mail.yml")]
    if api:
        args += ["-f", str(ROOT / "infra/docker-compose.account-api.yml")]
    result = subprocess.run(args + ["config", "--format", "json"], env=values,
                            text=True, capture_output=True, timeout=30)
    if failure:
        assert result.returncode != 0, "worker source was silently invented"
        return None
    assert result.returncode == 0, "account worker render failed"
    return json.loads(result.stdout)["services"]


def main():
    values = {"PATH": os.environ.get("PATH", "")}
    base = render(values)
    assert "account-mail" not in base
    assert base["api"]["environment"]["ACCOUNT_MAIL_ENABLED"] == "false"
    assert not base["api"].get("volumes")
    render(values, overlay=True, failure=True)
    render(values, api=True, failure=True)
    with tempfile.TemporaryDirectory(prefix="account-mail-render-") as directory:
        enabled = dict(values, ACCOUNT_MAIL_SECRETS_SOURCE_DIR=directory,
                       ACCOUNT_MAIL_ENABLED="true", ACCOUNT_MAIL_ORIGIN="https://accounts.example.test",
                       ACCOUNT_MAIL_SMTP_ADDRESS="smtp.example.test:465", ACCOUNT_MAIL_FROM="sender@example.test",
                       ACCOUNT_MAIL_SMTP_MODE="tls", ACCOUNT_MAIL_SMTP_USERNAME="synthetic-fixture",
                       ACCOUNT_MAIL_SMTP_PASSWORD_FILE="/run/secrets/account-mail/password",
                       ACCOUNT_MAIL_PROOF_KEY_FILE="/run/secrets/account-mail/proof-key")
        services = render(enabled, overlay=True)
        worker = services["account-mail"]
        assert worker["entrypoint"] == ["/account-mail"] and not worker.get("ports")
        for key, value in enabled.items():
            if key.startswith("ACCOUNT_MAIL_") and key != "ACCOUNT_MAIL_SECRETS_SOURCE_DIR":
                assert worker["environment"][key] == value
        mounts = worker["volumes"]
        assert len(mounts) == 1
        mount = mounts[0]
        assert mount["source"] == directory and mount["target"] == "/run/secrets/account-mail"
        assert mount["type"] == "bind" and mount["read_only"]
        assert not mount.get("bind", {}).get("create_host_path", False)
        assert not services["api"].get("volumes"), "worker credentials leaked into API mount"
        enabled["ACCOUNT_API_PROOF_KEY_SOURCE_FILE"] = str(Path(directory) / "proof-key")
        services = render(enabled, overlay=True, api=True)
        mounts = services["api"]["volumes"]
        assert len(mounts) == 1
        mount = mounts[0]
        assert mount["source"] == enabled["ACCOUNT_API_PROOF_KEY_SOURCE_FILE"]
        assert mount["target"] == "/run/secrets/account-api/proof-key" and mount["read_only"]
        assert not mount.get("bind", {}).get("create_host_path", False)
        assert services["api"]["environment"]["ACCOUNT_MAIL_PROOF_KEY_FILE"] == mount["target"]
        assert services["account-mail"]["volumes"][0]["target"] == "/run/secrets/account-mail"
    print("Account worker opt-in, env parity and private non-creating mount: PASS")


if __name__ == "__main__":
    main()
