# Account delivery runtime

- `secret_file.go` opens bounded 0400/0600 regular files under a pinned,
  non-writable-by-others directory, rejecting file/root symlinks and identity changes.
- `runtime.go` validates opt-in account-mail settings, reads the dedicated
  random 32-byte proof master and optional SMTP password, then wires the real
  registration/delivery services. No file or SMTP access when disabled.
- `registration.go` shares proof-key loading and provides API registration
  without reading a worker SMTP password. Runtime tests prove that isolation.
- `runtime_test.go` exercises file policy, disabled startup, configuration,
  key isolation/redaction and actual runtime setup using only synthetic files.

Master rotation: drain/expire queued ciphertext with the original key before
replacing it atomically and restarting. Delivered digest proofs remain verifiable
without that key; do not mark legacy emails verified or reset customer passwords.
No production secret is provisioned by this package. SMTP passwords contain no
CR/LF/NUL; generate files with exact content and private ownership for the worker.

The [C10 API/web](../../../docs/04-backend/account_authentication.md) wires public
signup only with working verification/password choice and matching origins.
The [worker](../../cmd/account-mail/README.md) runs delivery and safe
aggregate status. [Contract](../../../docs/04-backend/account_registration.md).
