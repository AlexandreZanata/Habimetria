# C09 Execution — Pending Registration and Transactional Delivery

Started 2026-11-11 at `b833bd8`, branch dev. Scope C09 only; C10-C64 pending.
Further split the mandatory [breakdown](commit_breakdown.md) into registration,
SMTP transport/configuration and durable delivery/worker for reviewability.

## C09.a — Pending identity and encrypted enqueue

Defined [registration](../../04-backend/account_registration.md) before schema
0009 and code. Reused users with explicit unverified pending state, without
inventing legacy verification, password or session. Existing identities remain
unchanged and staff/panel activation cannot promote pending registrations.
The internal service returns no proof, identity or existence result; public
transport, verification/password choice and login remain C10/C13 work.

Added purpose-bound digest verification and transactional encrypted mail enqueue,
source/address HMAC limits, resend cooldown and prior-proof/queue replacement.
Independent HKDF-derived AES-GCM/HMAC domains bind random 256-bit proofs to the
User/purpose/operation. Keys redact formatting/serialization; queue plaintext
contains no token/URL/body/address. Privacy inventory records exact purposes
and classes without inventing legal retention periods.

Tests cover concurrency, duplicate/case normalization, limits, resend/cancellation,
old/staff/disabled identity isolation, pending authority constraints, rollback
on enqueue failure, unavailable DB, key/proof tampering, binding and redaction.
Schema upgrade/replay preserves existing identity and rejects malformed proofs.

Executed with C08 disposable PostgreSQL 18.6/Garage/Mailpit:

- `GOTOOLCHAIN=go1.25.13 make verify-infra`: PASS, including regular verify,
  integration, all existing coverage floors and lifecycle cleanup.
- `go test -race -cover ./internal/accountproof ./internal/service/account`:
  PASS, 89.7% proof and 95.2% registration statement coverage; new floors 85%.
  Cryptographic primitive construction failures are not artificially injected.
- Secrets, diff, documentation links/new-file references and file bounds: PASS.

Commit `bbd43fdb535065cd5b2d509b5ae821e715927d51` was pushed to origin/dev;
[CI run 377](https://github.com/AlexandreZanata/Habimetria/actions/runs/43069832334)
passed backend and mobile-linux before C09.b began.

## C09.b — Real SMTP transport and activation configuration

Added a fixed bounded Portuguese message, stable operation Message-ID and
configured fragment proof origin. Remote SMTP requires explicit TLS/STARTTLS
with chain/hostname verification; local unauthenticated plaintext is restricted
to development loopback. No optional TLS/downgrade or insecure certificate flag.
Per-send deadlines/cancellation bound network work. Closed failure codes
preserve retryability for transport outages and redact server-controlled replies,
recipients and credentials; SMTP DATA acceptance is not inbox receipt.

Added disabled-by-default account-mail settings with env-example/Compose parity,
HTTPS/TLS/authentication requirements in production and private-file references.
The real SMTP fixture tests TLS/authentication, untrusted certificate and missing
STARTTLS denial before credentials/body, temporary/permanent rejection, dropped
TLS transport and cancellation. The reusable loopback Mailpit capture fixture
uses bounded polling without inherited proxies/redirects; actual SMTP proof
content is verified through its API. Durable leasing/retry/worker is C09.c.

Executed on disposable dependencies:

- `GOTOOLCHAIN=go1.25.13` runner with `make verify test-integration cover
  verify-commerce-config`: PASS, including all existing floors.
- `go test -race -cover ./internal/mail ./internal/config`: PASS,
  88.4% mail statement coverage (new floor 85%), configuration 100%.
- Actual Mailpit SMTP/capture plus protocol/TLS tests: PASS, not skipped.
- Secrets, diff, documentation references/links and changed-file bounds: PASS.

Commit `aa7573bf1f0d2cfc3bcc4c0e0b1a97e6fb40021d` was pushed to origin/dev;
[CI run 379](https://github.com/AlexandreZanata/Habimetria/actions/runs/43072070416)
passed backend and mobile-linux before C09.c began.

## C09.c — Leased delivery and operational runtime

Added migration 0010, SKIP LOCKED claims, reclaimable 60-second leases,
current identity/proof/expiry checks, guarded completion and bounded retry
(up to five claims, server backoff/jitter). Terminal states remove ciphertext.
Resend cancels pending/sending proofs; stale completion cannot overwrite a
new lease. Bounded cleanup expires proofs/limits without inventing operational
history retention. SMTP/DB failure does not fabricate delivery or verification.

Wired the actual runtime using a dedicated private master/SMTP password,
strict file/alias/bounds checks, safe error formatting and independent account
configuration. Added account-mail status/once/daemon command with aggregate
JSON and graceful shutdown, explicit read-only/non-creating worker overlay,
image binary/CA certificates and a synthetic profile-render check in CI.
No public registration control or token consumption is added before C10.

Real PostgreSQL/Mailpit tests cover the complete encrypted enqueue-to-SMTP
path, duplicate batch replay, concurrent workers, real SMTP outage/recovery,
permanent recipient rejection, expired/ineligible/corrupt proof erasure,
interrupted claims, stale leases, resend during flight, completion DB failure,
exhaustion, safe status without SMTP/store configuration and daemon shutdown.
Command fixtures explicitly preserve the isolated database DSN; private test
files use private directories rather than relying on the process umask.

Executed with disposable C08 dependencies:

- `GOTOOLCHAIN=go1.25.13 make verify-infra`: PASS, including regular verify,
  real integration, existing coverage floors and real teardown/signal proofs.
- `make verify-commerce-config`: PASS, including the new account-worker profile.
- `go test -race -cover` for account, runtime, SMTP and command: PASS.
  Statement coverage: account 91.0%, runtime 81.9%, SMTP 88.4%, command 80.8%.
  New enforced runtime floor: 75%; prior account/SMTP floors remain unchanged.
- Local Docker image build: PASS; binary and CA-bundle check with network
  disabled: PASS; invalid option rejected with expected exit 1.
- Secrets, diff, document links/new-file references and file line bounds: PASS.

Final commit/ref and exact-commit CI are checked after push; no self-hash is
invented here. C10-C64 and all owner-controlled gates remain pending.

## Manual gates

G01-G12 remain UNVERIFIED. No public signup, token-consume/login integration,
real provider deliverability, production credentials/activation, native device,
iOS, legal/privacy or owner acceptance is claimed here.
