import signal
import unittest
from unittest.mock import MagicMock, patch

import run


class LifecycleTest(unittest.TestCase):
    def tearDown(self):
        for signum in (signal.SIGINT, signal.SIGTERM):
            signal.signal(signum, signal.SIG_DFL)

    def test_partial_start_and_child_failure_always_cleanup(self):
        for error in (RuntimeError("startup failure"), InterruptedError("interrupted"), None):
            stack = MagicMock()
            stack.project = "synthetic-project"
            stack.start.side_effect = error
            with patch("run.Stack", return_value=stack), patch("run.probe_mail"), \
                    patch("run.test_environment", return_value={}), patch("run.check_command", return_value=17):
                if error:
                    with self.assertRaises(type(error)):
                        run.run(["synthetic-check"])
                else:
                    self.assertEqual(run.run(["synthetic-check"]), 17)
            stack.cleanup.assert_called_once()

    def test_cleanup_failure_cannot_report_success(self):
        stack = MagicMock()
        stack.project = "synthetic-project"
        stack.cleanup.side_effect = RuntimeError("cleanup failed")
        with patch("run.Stack", return_value=stack), patch("run.probe_mail"), \
                patch("run.test_environment", return_value={}), patch("run.check_command", return_value=0):
            with self.assertRaisesRegex(RuntimeError, "cleanup failed"):
                run.run(["synthetic-check"])


if __name__ == "__main__":
    unittest.main()
