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

Final commit/ref and exact-commit CI are checked after push; no self-hash is
invented. SMTP transport/configuration and durable delivery remain C09.b/C09.c.

## Manual gates

G01-G12 remain UNVERIFIED. No public signup, token-consume/login integration,
real provider deliverability, production credentials/activation, native device,
iOS, legal/privacy or owner acceptance is claimed here.
