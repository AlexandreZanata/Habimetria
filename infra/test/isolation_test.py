import json
import os
from pathlib import Path
import subprocess
import tempfile
import unittest
from unittest.mock import patch

from environment import configuration, host_environment, test_environment
from stack import Stack


class IsolationTest(unittest.TestCase):
    def test_poisoned_environment_cannot_select_customer_services(self):
        poison = {"PATH": os.environ["PATH"], "HOME": os.environ["HOME"],
                  "DATABASE_URL": "postgres://real.invalid/customer", "S3_ENDPOINT": "https://real.invalid",
                  "SMTP_HOST": "real.invalid", "COMMERCE_NEW_PURCHASES_ENABLED": "true",
                  "COMPOSE_FILE": "/customer/stack.yml", "TEST_DATABASE_URL": "customer",
                  "AWS_PROFILE": "customer", "PGSERVICE": "customer", "MP_SMTP_RELAY_ALL": "true"}
        with patch.dict(os.environ, poison, clear=True), tempfile.TemporaryDirectory() as directory:
            values = configuration(directory)
            self.assertEqual(host_environment(), {"PATH": poison["PATH"], "HOME": poison["HOME"]})
            for key in poison.keys() - {"PATH", "HOME"}:
                self.assertNotIn(key, values)
            self.assertEqual(Path(values["FIXTURE_GARAGE_CONFIG"]).stat().st_mode & 0o777, 0o600)
            env = test_environment(values, {"postgres": 54321, "objects": 54322, "smtp": 54323, "mail": 54324})
            self.assertNotIn("real.invalid", " ".join(env.values()))
            stack = Stack(directory, values)
            rendered = json.loads(stack.compose(["config", "--format", "json"]))
            self.assertEqual(set(rendered["services"]), {"postgres", "objects", "mail", "gateway"})
            self.assertTrue(rendered["networks"]["default"]["internal"])
            self.assertEqual(rendered["services"]["postgres"]["image"], "postgres:18.6-alpine")
            for name, service in rendered["services"].items():
                self.assertNotIn("container_name", service)
                if name != "gateway":
                    self.assertEqual(set(service["networks"]), {"default"})
                    self.assertFalse(service.get("ports"))
                for port in service.get("ports", []):
                    self.assertEqual(port["host_ip"], "127.0.0.1")
                    self.assertIn(port.get("published"), (None, "0"))
            mail = rendered["services"]["mail"]["environment"]
            self.assertEqual(mail["MP_SMTP_ALLOWED_RECIPIENTS"], r"@example\.test\z")
            self.assertFalse(any("RELAY" in key or "FORWARD" in key for key in mail))
            for volume in rendered["volumes"].values():
                self.assertFalse(volume.get("external", False))

    def test_remote_daemons_are_rejected_before_container_commands(self):
        for endpoint in ["ssh://customer.invalid", "tcp://127.0.0.1:2375", "unix://relative", ""]:
            stack = Stack("/tmp", {"DOCKER_HOST": endpoint, "DOCKER_CONTEXT": "selected"})
            with patch.object(stack, "docker", return_value=json.dumps([{"Endpoints": {"docker": {"Host": endpoint}}}])):
                with self.assertRaisesRegex(RuntimeError, "local Unix"):
                    stack.local_daemon()

    def test_docker_errors_do_not_echo_generated_secrets(self):
        stack = Stack("/tmp", {"FIXTURE_DB_PASSWORD": "private"})
        with patch("subprocess.run", return_value=subprocess.CompletedProcess([], 1, "password=private", "secret=private")):
            with self.assertRaises(RuntimeError) as error:
                stack.docker(["info"])
        self.assertNotIn("private", str(error.exception))


if __name__ == "__main__":
    unittest.main()
