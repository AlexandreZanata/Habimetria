# Customer-Owned Password Recovery

Extends [authentication](account_authentication.md) and the existing
[mail worker](account_registration.md), without payment or local workflow gates.
The same User/credential policy is reused. Administrative MFA/recovery remains
C14; the trusted primary-owner CLI is retained and never becomes customer login.

## Eligibility and initiation

Only active enrolled ordinary Users can receive a recovery proof. Unknown,
pending, disabled, unenrolled and staff identities receive the same neutral
response. No identity is created, activated or promoted, and requesting recovery
does not change credentials or sessions. Legacy verified status is not inferred.
ASCII email/connection-peer validation and the 250ms normal-path response floor
match signup. Separate HMAC domains apply source 10/address 3 per 15 minutes,
plus a 60-second resend cooldown. Existing registration counters are reused with
independent keys. Overload/outages can exceed the floor; no constant-time claim.

`0013_account_password_recovery.sql` adds a dedicated purpose-bound digest table.
Proofs contain 256 random bits, expire after 30 server minutes and are single-use.
Resend atomically replaces the proof and cancels only its prior recovery mail.
The existing AEAD envelope binds purpose, User and operation; old verification
envelopes remain compatible and cannot be opened as recovery envelopes.

## Atomic password replacement

The consumer requires the existing password policy and matching confirmation.
It locks the User before deleting the current unexpired password_recovery proof,
then replaces the credential, removes browser/API sessions and activation links,
invalidates outstanding proof mail, writes a minimal self-owned password_reset
audit and enqueues a password_changed security notice in the same transaction.
Audit/notice/storage failure rolls everything back. No automatic login occurs.
Unknown, replaced, expired, replayed and cross-purpose proofs fail identically.
Changing credential, role, active status or email invalidates recovery proofs;
restoring old state never resurrects a proof. No SEALED/private artifacts change.

The notice has no token, password or arbitrary body. A digest of the intended
recipient address prevents delivery to a subsequently changed email; it is
pseudonymized personal data, not anonymous data. The existing leased worker
handles recovery links/notices with bounded retry, erasure and safe failure
codes. SMTP acceptance is not inbox delivery. Notices may arrive late and never
claim a recent event, certified identity or full compromise remediation.

Consumed proofs need no master key: previously delivered links can complete
while mail/signup are disabled, with the security notice queued durably. New
initiation requires the existing private proof-key runtime, approved SMTP setup
and matching configured web/mail origins. No SMTP secret enters the API mount.

## Delivery boundaries

Rollout: pause older workers, apply migration 0013 and start the C11 worker
before enabling C11 API initiation. Older verification-only workers cannot
dispatch the new templates. Roll back initiation by disabling account mail;
retain the current API/worker and schema for outstanding proofs/notices. Do not
erase immutable audit facts or downgrade a worker with recovery mail queued.

Source index: `service/account/recovery.go`, `request_policy.go`,
`delivery_message.go` and its [README](../../backend/internal/service/account/README.md);
`store/postgres/account_recovery_request.go`, `account_password_reset.go`, existing
lease/completion queries; `internal/privacy/account_fields.go`; the purpose and
mail [proof package](../../backend/internal/accountproof/README.md) and
[template package](../../backend/internal/mail/README.md). Fixture mail capture
supports purpose-specific Message-IDs through `FindKind`.

C11.a supplies the recovery service/store, encrypted delivery and real tests.
C11.b supplies complete JSON and web transport. Native account screens stay C13;
device refresh families stay C12. Neither implementation activates production
SMTP, subscriptions or owner gates.

The design follows the
[OWASP recovery guidance](https://cheatsheetseries.owasp.org/cheatsheets/Forgot_Password_Cheat_Sheet.html).
Legal retention durations and production deliverability remain owner-controlled.
