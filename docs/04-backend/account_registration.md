# Pending Registration and Transactional Mail

C09 extends `users`; it does not create another identity database. Read
[account lifecycle](../03-architecture/account_lifecycle.md) and
[privacy](../03-architecture/commercial_privacy.md).

## Registration boundary

The internal registration service accepts a conventional ASCII email address
and a parsed source IP, normalizes case/whitespace and returns no account ID,
proof or existence result. No password is accepted before email ownership is
proved: C10 asks the verifier to choose the password using the existing policy.
This avoids activating a password chosen by someone else for that address.
Public API/web/native signup and token consumption are C10/C13 integrations;
there is no public signup exposure or unusable signup control in C09.

Only a new ordinary User becomes self-registration-pending: inactive, managed,
no password/session, with server registration instant and no verified instant.
Existing staff, legacy, activated or otherwise disabled accounts are unchanged;
they receive no signup proof. Staff cannot promote/activate pending signup or
issue its panel access link. Existing invited-account login remains compatible;
an old account's email verification is never fabricated by migration.

Source/address HMAC limits (10 per source and 3 per address per 15 minutes)
silently suppress excess requests. Pending resend has a 60-second cooldown.
Resend replaces the prior verification digest and cancels its queued mail.
Tokens have 256 random bits, one purpose (`email_verification`), a server
30-minute expiry, and only SHA-256 digests in the verification table. Invalid
syntax is an input error; database/queue failures propagate as unavailable,
never a false claim of durable acceptance. HTTP neutral response/timing and
trusted source-address transport checks belong to C10.

## Durable secret handling

User/purpose-bound verification plus mail outbox enqueue is one transaction.
The outbox holds only an AES-256-GCM encrypted proof, bound to User, purpose
and operation UUID; recipient is resolved through the existing User. Separate
HKDF domains derive encryption and throttle keys from an operator-provisioned
32-byte random master. Keys and raw proofs are not logged/serialized. No email
body, password, raw URL or arbitrary template/payload is stored in this queue.
Terminal delivery/cancellation removes ciphertext. Delivery is at-least-once;
proof invalidation remains authoritative even if an older email is in flight.

## SMTP transport

The transport uses a single bounded Portuguese text template and configured
origin, never a request Host header. Proofs travel in `/painel/#verificar=` URL
fragments; C10 consumes them with a POST. Message-ID is stable per operation.
Only explicit STARTTLS or implicit TLS with hostname/chain verification is
accepted for external SMTP. Local development may use unauthenticated plain
SMTP only on loopback with a loopback HTTP origin. There is no TLS downgrade,
insecure certificate option, arbitrary subject/body or plaintext AUTH fallback.

Every send has a connection/context deadline and cancellation closes the socket.
Only the SMTP acceptance after DATA counts as accepted by the transport; this
is not inbox receipt or account verification. Reply text, addresses, credentials
and proof URLs never enter returned errors. Failures have closed codes and
explicit transient/permanent classification. Worker/lease delivery follows in
the next C09 sub-slice. No local
feature, store entitlement or hosted collection is required by registration.
Real provider identities/deliverability and legal retention remain unverified.

## Files and checks

- `backend/migrations/0009_account_registration.sql`: pending identity,
  verification digests, encrypted queue and bounded registration limits.
- PostgreSQL `account_registration_schema_test.go` verifies upgrade/replay,
  preserved legacy identity and pending/proof/queue constraints.
- [Proof envelope](../../backend/internal/accountproof/README.md).
- [Registration service](../../backend/internal/service/account/README.md).
- [Mail test fixture](../../backend/internal/testutil/mailfixture/README.md).
- [Mail adapter](../../backend/internal/mail/README.md).
- `backend/internal/config/account_mail.go` and `account_mail_test.go` define
  disabled-by-default settings, environment parity and activation validation.
- PostgreSQL `account_registration.go` and `account_registration_limits.go`
  serialize registration/resend and enqueue atomically.
- `backend/internal/service/account/registration_integration_test.go` and
  `registration_safety_test.go` exercise real-DB duplicates, resends, limits,
  rollback, old-account isolation and unavailable storage.
- `backend/internal/privacy/account_fields.go` indexes identity/proof/queue data
  with purposes and retention classes; no legal duration is invented.
- [Execution record](../06-roadmap/commercial/c09_execution.md).

Security approach follows [OWASP token guidance](https://cheatsheetseries.owasp.org/cheatsheets/Forgot_Password_Cheat_Sheet.html);
this is a registration implementation, not password recovery (C11).

Account mail environment uses ACCOUNT_MAIL_ENABLED=false by default, an exact
configured origin, SMTP address/from/mode, optional username/password-file and
required proof-key file. Enabled production requires HTTPS, TLS and SMTP
credentials. Disabled settings do not require secrets or contact SMTP. Paths
are absolute references, not inline passwords; physical file policy is applied
when the delivery runtime opens them. Env example and Compose forward the same
fields. No file provisioning or production activation is performed by C09.
