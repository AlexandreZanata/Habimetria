#!/usr/bin/env python3
"""Real Docker cleanup proof after child failure and handled SIGTERM."""
from pathlib import Path
import os
import selectors
import signal
import subprocess
import sys
import time

ROOT = Path(__file__).resolve().parents[2]
RUNNER = ROOT / "infra/test/run.py"


def require_cleanup(output, code, expected):
    if code != expected or "Disposable containers, volumes and network removed: PASS" not in output:
        raise RuntimeError("Disposable lifecycle proof failed:\n" + output[-4000:])


def main():
    failed = subprocess.run([sys.executable, str(RUNNER), "--", sys.executable,
                             "-c", "raise SystemExit(17)"], cwd=ROOT, text=True,
                            stdout=subprocess.PIPE, stderr=subprocess.STDOUT, timeout=180)
    require_cleanup(failed.stdout, failed.returncode, 17)
    print("Real child-failure cleanup: PASS", flush=True)
    child = "import time; print('FIXTURE_CHILD_READY', flush=True); time.sleep(120)"
    with subprocess.Popen([sys.executable, str(RUNNER), "--", sys.executable, "-c", child],
                          cwd=ROOT, stdout=subprocess.PIPE, stderr=subprocess.STDOUT, bufsize=0) as runner:
        output = []
        selector = selectors.DefaultSelector()
        selector.register(runner.stdout, selectors.EVENT_READ)
        deadline = time.monotonic() + 120
        ready = False
        try:
            while time.monotonic() < deadline and runner.poll() is None:
                if selector.select(timeout=1):
                    output.append(os.read(runner.stdout.fileno(), 4096))
                    if b"FIXTURE_CHILD_READY" in b"".join(output):
                        ready = True
                        break
        finally:
            selector.close()
            if runner.poll() is None:
                runner.send_signal(signal.SIGTERM)
            rest, _ = runner.communicate(timeout=45)
            output.append(rest)
        require_cleanup(b"".join(output).decode(), runner.returncode, 1)
        if not ready:
            raise RuntimeError("Signal proof never reached its running child")
    print("Real SIGTERM child-group and resource cleanup: PASS", flush=True)


if __name__ == "__main__":
    main()
