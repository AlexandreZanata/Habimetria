# Explicit SMTP capture fixture

`capture.go` validates loopback-only MAIL_TEST_SMTP/HTTP settings and polls the
actual Mailpit API for a fixed operation Message-ID. HTTP requests never use
proxy environment or follow redirects. No production SMTP defaults are read.
An absent optional pair skips; partial, invalid or unavailable endpoints fail.

Used by mail transport and account outbox integration tests under C08's
`make verify-infra`. [Contract](../../../../docs/04-backend/account_registration.md).
