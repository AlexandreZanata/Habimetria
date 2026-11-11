# C11 — Customer-Owned Password Recovery Execution

Date: 2026-11-11. Starting commit: `fe7e0aed020cead6d98dd0d44e6f91aa847fea3f`.
Contract: [recovery](../../04-backend/account_recovery.md).

## C11.a — Recovery service, revocation and security delivery

Purpose-bound recovery proofs reuse the existing User, credential policy and
leased SMTP outbox. Neutral, throttled initiation never changes authority.
Atomic consumption replaces the password, revokes old sessions/links and
persists audit plus a capability-free security notice. Pending/disabled/staff
accounts are ineligible; legacy verification status and sealed data are retained.
No public recovery transport is exposed until C11.b completes the flow.

Passed with disposable PostgreSQL 18.6/private storage/SMTP: `make verify
test-integration cover` and `go test -race -cover ./internal/service/account
./internal/accountproof ./internal/mail`. After adding explicit API revocation
independent of a changed credential encoding, reran disposable `make verify`
and account race/coverage. Measured account 93.1%, proof 91.1%, mail 89.8%; no
coverage floor lowered. The completed privacy inventory also passed its direct
real-schema test. All fixture containers/volumes/networks were removed.
Logs: `/tmp/habimetria-c11a-check.log`, `/tmp/habimetria-c11a-final.log`,
`/tmp/habimetria-c11a-privacy.log`. New-file bounds, local links, secret and diff
checks passed. Pushed SHA/exact CI is recorded in the next update to avoid a
circular self-hash. G01-G12, production mail activation and
deliverability remain UNVERIFIED. Native account screens remain C13.
