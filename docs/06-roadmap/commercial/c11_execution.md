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

C11.a pushed: `9bb01f9b507ac1284f1310149d4be625dc38f595` on `origin/dev`.
Exact PR workflow [43090155614](https://github.com/AlexandreZanata/Habimetria/actions/runs/43090155614)
(#389): backend and mobile-linux passed before C11.b implementation.

## C11.b — Complete browser and native JSON transport

Classified public endpoints and branded same-origin forms use the real recovery
service, shared credential policy and source budget. Completion requires a
matching confirmation and a deliberate new login. Mail-disabled startup retains
the password consumer. One private-key load wires both initiation purposes;
SMTP secrets remain exclusive to the worker.

Passed with disposable PostgreSQL 18.6/private storage/SMTP:
`make verify test-integration cover verify-commerce-config`, followed by
`go test -race -cover ./cmd/api ./internal/httpapi/... ./internal/accountdelivery`.
Measured API 85.3%, account transport 100%, panel 79.7%, routepolicy 97.5%,
delivery runtime 83.3%, API command 20.0%. Existing coverage floors are preserved.
No new file exceeds 200 lines; local links, diff and secret scan passed.
Log `/tmp/habimetria-c11b-check.log`; all disposable resources removed.

Actual Chrome 157 headless/CDP, built API and real local SMTP capture passed
signup/verification regression, recovery request, signed-in root fragment,
confirmation mismatch with cleared passwords/retained code, replacement,
revoked browser/API access, secret-free security notice and new-password login.
Checked Manrope, 390px/1440px layout, enlarged text, keyboard order, empty web
storage and proof absence from request URLs. Screenshots were inspected.
Command: `GOTOOLCHAIN=go1.25.13 python3 infra/test/run.py -- python3 /tmp/habimetria_c11_browser.py`.
Log `/tmp/habimetria-c11-browser.log`; local screenshots `/tmp/habimetria-c11-ui/`.
All mail was synthetic `.test` capture, with no production activation or customer data.

Final implementation commit is identified by
`feat(account): connect web and JSON password recovery` in `origin/dev` history.
Exact final-head CI evidence is maintained on
[PR 21](https://github.com/AlexandreZanata/Habimetria/pull/21) without guessing a
circular self-hash. Main is unchanged. C12 protected/rotating device sessions,
C13 native screens and C14 staff MFA remain pending. G01-G12 and production
mail provisioning/activation/deliverability remain UNVERIFIED. No manual
accessibility/device/iOS/visual-PDF or store acceptance is claimed.
