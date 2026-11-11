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
Public JSON/web signup and token consumption now follow
[C10 authentication](account_authentication.md). Native app screens remain C13.

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
never a false claim of durable acceptance. C10 applies a common 250ms response
floor to valid initiation paths, shared neutral bodies and connection-peer-only
source checks. Overload/outages can exceed the floor; no perfect timing claim.

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
origin, never a request Host header. Proofs travel in `/painel/verificar#verificar=` URL
fragments; C10 consumes them with a POST. Message-ID is stable per operation.
Only explicit STARTTLS or implicit TLS with hostname/chain verification is
accepted for external SMTP. Local development may use unauthenticated plain
SMTP only on loopback with a loopback HTTP origin. There is no TLS downgrade,
insecure certificate option, arbitrary subject/body or plaintext AUTH fallback.

Every send has a connection/context deadline and cancellation closes the socket.
Only the SMTP acceptance after DATA counts as accepted by the transport; this
is not inbox receipt or account verification. Reply text, addresses, credentials
and proof URLs never enter returned errors. Failures have closed codes and
explicit transient/permanent classification. Leased delivery applies the durable contract below. No local
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

## Durable delivery

Migration 0010 adds 60-second job leases. Claims use SKIP LOCKED; the SMTP
operation runs outside database transactions with its 15-second deadline.
Before each send, current pending identity, verification digest and server
expiry are rechecked. Digest/ciphertext binding must agree. Replacement cancels
pending/sending jobs; an in-flight old email cannot restore its invalid proof.

Completion requires the exact unexpired lease. Abandoned jobs are reclaimable;
stale workers cannot overwrite a newer lease. Transient failures retry up to
five claims with server exponential delay (30-300 seconds plus bounded jitter).
Certificate/configuration, rejected recipient, invalid proof and expired or
ineligible identity become terminal safe codes. Terminal states remove proof
ciphertext. Expired verification/limit rows are pruned in bounded batches;
operational delivery history awaits approved retention, not an invented period.

SMTP acceptance followed by process/DB failure can cause a duplicate email.
The same operation Message-ID/proof is reused; no exactly-once inbox promise.
The verification digest remains the authority and C10 consumes it once.
The worker exposes only aggregate counters, never bodies, recipients or links.
Database failures propagate; zero batch exit means results persisted, not that
all messages reached an inbox. No resend impersonates an existing customer.

- PostgreSQL `account_mail_lease.go` and `account_mail_completion.go` own the
  claims, eligibility, guarded completion, counters and bounded cleanup.
- Account `delivery.go`, `delivery_integration_test.go` and
  `delivery_limits_test.go` and `delivery_recovery_test.go` test concurrency, SMTP outage/retry, invalid/expired
  proofs, lease loss/reclaim, replacement, terminal erasure and real SMTP capture.
- `backend/migrations/0010_account_mail_leases.sql` adds guarded delivery leases.
- [Delivery runtime](../../backend/internal/accountdelivery/README.md) and
  [worker command](../../backend/cmd/account-mail/README.md) configure the actual
  private-file/SMTP adapter independently of public registration.

`infra/verify_account_mail_config.py` verifies explicit worker exposure, complete
env forwarding and its read-only non-creating private mount. The API receives
no worker secret mount; the separate C10 API overlay mounts only the proof key.
The image includes the worker and CA certificates.
The deployment overlay never sends mail unless explicitly configured/enabled.
