# Installation boundary probes

`SessionInstallationTest.kt` uses real temporary filesystem operations to verify
the non-secret marker: purge before acknowledgment, restart preservation,
reinstall/missing-marker purge, failed-purge retry and corrupt-marker denial.
Run `:shared:jvmTest`. These checks do not emulate or approve Apple Keychain.
