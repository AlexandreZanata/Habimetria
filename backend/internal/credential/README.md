# Shared password credentials

`password.go` owns the existing v1 PBKDF2-HMAC-SHA256 encoding and policy.
`password_test.go` checks legacy compatibility, Unicode, limits and malformed
credentials. Browser and connected-account services use this same package.
See [authentication](../../../docs/04-backend/account_authentication.md).
