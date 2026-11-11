# Shared Account Authentication

Depends on [registration](account_registration.md) and
[identity lifecycle](../03-architecture/account_lifecycle.md). No subscription
or local collection dependency. Passwords are chosen only after email proof.

## Atomic verification (C10.a)

The service accepts a strict 32-byte base64url proof and a UTF-8 password of
15 or more characters, at most 1024 bytes. Shared `internal/credential/password.go`
preserves the existing v1 panel encoding: PBKDF2-HMAC-SHA256, 600,000 iterations,
random 16-byte salt and 32-byte digest. Malformed/absent credentials use a dummy
derivation on login; no second password policy or identity database exists.
`internal/credential/README.md` indexes its compatibility tests.

`service/account/verification.go` calls `store/postgres/account_verification.go`.
The transaction locks the pending ordinary User before consuming its current
`email_verification` digest. Expiry is rechecked against the server clock after
lock acquisition. Exactly one consumer can set the password, activate the User
and set `email_verified_at`. Unknown, replaced, expired, cross-purpose and replayed
proofs produce the same unavailable-proof result. Existing identities are untouched.

The same transaction erases pending/sending mail ciphertext and leases, removes
any pending panel link and writes one self-owned `email_verified` audit event.
An audit/storage failure rolls everything back. It creates no session: the
customer must deliberately sign in. SMTP delivery never proves ownership.
Migration `0011_account_verification_audit.sql` extends only the audit allowlist;
rollback must not erase already committed immutable audit facts.

`service/account/verification_integration_test.go` checks concurrent consumers,
expiry, replacement, policy, rollback, purpose separation and panel login with
the verified credential. All fixtures are synthetic and use disposable PostgreSQL.

## Security references

Generic authentication failures and shared derivation follow the
[OWASP authentication guidance](https://cheatsheetseries.owasp.org/cheatsheets/Authentication_Cheat_Sheet.html).
Separate cookie/bearer transports and digest-only session storage follow
[OWASP session guidance](https://cheatsheetseries.owasp.org/cheatsheets/Session_Management_Cheat_Sheet.html).
This is implementation evidence, not a security or store release approval.
