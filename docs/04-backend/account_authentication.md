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

## API access authority (C10.b)

`service/account/authentication.go` reuses `panel.Authenticate` and the same
rate-limit authority. `store/postgres/account_sessions.go` issues a random
256-bit secret only after locking and rechecking the active ordinary User and
expected credential. Staff receive the same unavailable-login result. This
does not introduce native administrative login before C14 MFA.

Migration `0012_account_access_sessions.sql` stores SHA-256 digests with a fixed
`account_api` audience and a server-issued lifetime of 15 minutes. Reads recheck
expiry, current role and active status. A User trigger deletes access on any
credential, role or active-status change; reverting a change cannot resurrect
old access. Expired-row cleanup is bounded to 100 per issuance. Revocation is
idempotent. Browser cookies and API access never query each other's tables.
No raw access secret or credential copy is stored in the session table.
`internal/privacy/account_fields.go` registers its identity/digest/time purposes
and operational retention class; no legal retention duration is invented.

`service/account/authentication_integration_test.go` tests legacy credential
compatibility without fabricating verified status, staff/unknown denial,
audience separation, expiry, revocation and stale-credential checks at issuance.
HTTP entry points are specified below. Device refresh families,
rotation/reuse detection and protected native storage remain C12; app screens
remain C13. These short-lived credentials do not grant a subscription.

## JSON and web entry points (C10.c)

All account JSON operations use a 4096-byte object body, application/json,
known fields only and no trailing document. Native calls reject Origin/cookies;
public operations also reject Authorization. Account controls require one
Bearer header, recheck the current User and never accept a browser cookie.
All responses are no-store; errors use `{error,code}` with stable safe codes.

- POST `/api/v1/account/registrations`: `{email}`; neutral 202
  `verification_requested` for new/existing/cooldown/limited valid requests.
  Valid initiation has a common 250ms lower bound, not constant latency under
  load/outage. Only durable enqueue can succeed; missing config/storage is 503.
- POST `/api/v1/account/verification`: `{token,password}`; 200 `email_verified`;
  401 `invalid_verification` for unknown/expired/replayed/cross-purpose proof.
- POST `/api/v1/account/sessions`: `{email,password}`; 200 with `access_token`,
  `token_type=Bearer`, `expires_in=900`; neutral 401 `authentication_required`.
- GET `/api/v1/account/profile`: own `id,email,name`, without collection or
  fabricated legacy verification status. DELETE `/account/sessions/current`
  under the same API base revokes the presented access and returns 204.
  Repeating with revoked access is 401; the underlying revocation is idempotent.

Invalid syntax/policy is 400 (`invalid_request` / `password_policy`), mixed
transport 403, size 413, media type 415 and shared login/verification throttle
429 (`too_many_attempts`, Retry-After 900). Infrastructure failures are 503
`verification_unavailable`, without underlying errors. Login shares the panel's
source/email counters; verification shares its source budget. Source is the
actual connection peer only; forwarded headers never change that authority.
HTTPS termination is mandatory for hosted credentials in production.

GET/POST `/painel/cadastro` and `/painel/verificar` use the same services and
exact-Origin bounded forms. No GET consumes proof. New mail links target the
verification page; `assets/access.js` also handles earlier root fragments and
invites, even when another browser account is already signed in. It immediately
removes the fragment from browser history and fills only the appropriate form;
no query, localStorage/sessionStorage, automatic consume or automatic sign-in.
Password-policy retry retains the proof only in the no-store POST response.
GET `/painel/entrar` deliberately renders login even with an existing cookie.
Signup links/forms are hidden when registration is unavailable; existing login
and verification remain available independently of mail activation.

The legacy private-resource guard accepts this API audience through the same
current Property ownership lookup. Invalid/mixed bearer credentials never fall
back to cookies. Another User/staff/unknown token still gets 404. Native create
does not accept a supplied owner ID; enrollment stays C33. No paid rights are
granted and SEALED checks remain in the existing services/stores.

## Runtime activation and file index

`cmd/api/account.go` wires shared services; `accountdelivery/registration.go`
and C11's `requests.go` load only the private proof key, never a worker SMTP password. Signup requires
enabled mail, a readable key and identical nonempty PANEL_ORIGIN and
ACCOUNT_MAIL_ORIGIN. Missing key or mismatched origin disables signup with a
safe operator message; it does not disable login or previously issued proofs.

`infra/docker-compose.account-api.yml` is an explicit opt-in, read-only,
non-creating **single-file** proof mount. Set ACCOUNT_API_PROOF_KEY_SOURCE_FILE
to the same 32-byte private key used by the worker. The API target is fixed at
`/run/secrets/account-api/proof-key`; the worker retains its private directory
and SMTP credential. Use this overlay with `docker-compose.yml` and
`docker-compose.account-mail.yml`, the exact matching HTTPS origins, configured
SMTP identity and approved private files. Default compose/mail remains disabled;
no production activation or key provisioning is performed by code delivery.
`infra/verify_account_mail_config.py` tests both isolated and combined mounts.

HTTP files: `httpapi/account.go`, `account_input.go`, `account_input_test.go`,
`account_fixture_test.go`, `account_integration_test.go` and
[accountauth](../../backend/internal/httpapi/accountauth/README.md).
Web files: `panelweb/account_forms.go`, `account_services.go`, `views/registration.html`, existing
access/layout templates and styles/script. Test fixtures and executable proofs:
`panelweb/account_fixture_test.go`, `account_forms_integration_test.go`,
`account_api_integration_test.go`, `account_boundary_integration_test.go`.
They cover neutral bodies, cross-origin denial, shared throttling, purpose/replay,
own profile, cookie/bearer separation, cross-User Report privacy and SEALED writes.
`accountdelivery/runtime_test.go` proves the API does not read SMTP credentials.
`cmd/api/account_integration_test.go` proves that mail rollback/missing keys or
origin mismatch disable signup while login and delivered proofs remain usable.

## Security references

[C11 recovery](account_recovery.md) adds customer-owned password recovery and
security notices. Protected/rotating native sessions and account screens remain
C12-C13; no production administrative MFA or owner acceptance is claimed here.

Generic authentication failures and shared derivation follow the
[OWASP authentication guidance](https://cheatsheetseries.owasp.org/cheatsheets/Authentication_Cheat_Sheet.html).
Separate cookie/bearer transports and digest-only session storage follow
[OWASP session guidance](https://cheatsheetseries.owasp.org/cheatsheets/Session_Management_Cheat_Sheet.html).
This is implementation evidence, not a security or store release approval.
