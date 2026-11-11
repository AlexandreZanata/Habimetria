"""Local-only Compose lifecycle; errors never dump generated configuration."""
import json
from pathlib import Path
import subprocess
import uuid

from environment import ROOT


class Stack:
    def __init__(self, directory, values):
        self.values = values
        self.project = "habimetria-test-" + uuid.uuid4().hex
        self.command = ["docker", "compose", "--project-name", self.project,
                        "--project-directory", directory, "--env-file", "/dev/null",
                        "-f", str(ROOT / "infra/test/compose.yml")]

    def docker(self, arguments, timeout=120):
        result = subprocess.run(["docker", *arguments], env=self.values, text=True,
                                capture_output=True, timeout=timeout)
        if result.returncode:
            raise RuntimeError("Docker operation failed: " + self.redact(result.stderr[-2000:]))
        return result.stdout.strip()

    def redact(self, output):
        for key, value in self.values.items():
            if key.startswith("FIXTURE_") and value:
                output = output.replace(value, "[fixture value]")
        return output

    def local_daemon(self):
        endpoint = self.values.get("DOCKER_HOST", "")
        if self.values.get("DOCKER_CONTEXT") or not endpoint:
            context = json.loads(self.docker(["context", "inspect"]))
            endpoint = context[0]["Endpoints"]["docker"]["Host"]
        if not endpoint.startswith("unix:///") or not Path(endpoint[7:]).is_absolute():
            raise RuntimeError("Disposable fixtures require a local Unix-socket Docker daemon")

    def compose(self, arguments, timeout=120):
        return self.docker(self.command[1:] + arguments, timeout)

    def start(self):
        try:
            self.compose(["up", "--detach", "--wait", "--wait-timeout", "90"], 180)
        except RuntimeError as error:
            logs = self.redact(self.compose(["logs", "--tail", "15"]))
            raise RuntimeError(str(error) + "\n" + logs) from error
        ports = {}
        for key, port in [("postgres", 5432), ("objects", 3900), ("smtp", 1025), ("mail", 8025)]:
            binding = self.compose(["port", "gateway", str(port)])
            host, value = binding.rsplit(":", 1)
            if host != "127.0.0.1" or not value.isdecimal() or not 0 < int(value) < 65536:
                raise RuntimeError("Fixture port is not a loopback binding: " + repr(binding))
            ports[key] = int(value)
        return ports

    def cleanup(self):
        self.compose(["down", "--volumes", "--remove-orphans", "--timeout", "5"])
        label = "label=com.docker.compose.project=" + self.project
        for resource in (["ps", "--all", "--quiet"], ["volume", "ls", "--quiet"],
                         ["network", "ls", "--quiet"]):
            if self.docker([*resource, "--filter", label]):
                raise RuntimeError("Fixture cleanup left project resources: " + self.project)
