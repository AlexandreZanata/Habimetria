#!/usr/bin/env python3
"""Run checks with fresh dependencies and verified project-specific cleanup."""
import signal
import os
import subprocess
import sys
import tempfile

from environment import ROOT, configuration, test_environment
from mail_probe import probe_mail
from stack import Stack


def interrupted(signum, _frame):
    raise InterruptedError("Fixture run interrupted by signal " + str(signum))


def check_command(command, env):
    with subprocess.Popen(command, cwd=ROOT, env=env, start_new_session=True) as child:
        try:
            return child.wait(timeout=900)
        except BaseException:
            os.killpg(child.pid, signal.SIGTERM)
            try:
                child.wait(timeout=5)
            except subprocess.TimeoutExpired:
                os.killpg(child.pid, signal.SIGKILL)
                child.wait()
            raise


def run(command):
    with tempfile.TemporaryDirectory(prefix="habimetria-fixture-") as directory:
        values = configuration(directory)
        stack = Stack(directory, values)
        stack.local_daemon()
        print("Disposable project: " + stack.project, flush=True)
        try:
            env = test_environment(values, stack.start())
            probe_mail(env)
            return check_command(command, env)
        finally:
            # Ignore repeated interrupts while tearing down owned resources.
            for signum in (signal.SIGINT, signal.SIGTERM):
                signal.signal(signum, signal.SIG_IGN)
            stack.cleanup()
            print("Disposable containers, volumes and network removed: PASS", flush=True)


def main():
    for signum in (signal.SIGINT, signal.SIGTERM):
        signal.signal(signum, interrupted)
    command = sys.argv[1:]
    if command[:1] == ["--"]:
        command = command[1:]
    try:
        return run(command or ["make", "verify", "test-integration", "cover"])
    except (RuntimeError, InterruptedError, subprocess.TimeoutExpired) as error:
        print(str(error), file=sys.stderr)
        return 1


if __name__ == "__main__":
    sys.exit(main())
