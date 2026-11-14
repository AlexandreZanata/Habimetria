# C10 — Shared Verified Authentication Execution

Date: 2026-11-11. Starting commit: `f4c7b1454d1aed038131ae1beabd41e8cb7ad305`.
Contract: [authentication](../../04-backend/account_authentication.md).

## C10.a — Atomic verification

Shared versioned credentials preserve existing panel passwords. A pending
ordinary User can choose a password and prove email ownership once, atomically
with proof consumption, encrypted queue erasure and minimal audit. No automatic
session, pre-registration password, role elevation or local/paywall dependency.
Tests exercise replay, expiry, concurrent consumers, replacement, cross-purpose
proofs, rollback and real browser-session service compatibility.

Passed in disposable PostgreSQL 18.6, private S3 and SMTP fixtures:
`make verify test-integration cover`; `go test -race ./internal/service/account
./internal/service/panel ./internal/credential`. After strict token-length
hardening, reran `make verify` and targeted coverage: proof 90.2%, credential
94.7%, account 92.6%. New credential floor: 85%; existing floors unchanged.
Disposable cleanup passed. Logs: `/tmp/habimetria-c10a-check.log` and
`/tmp/habimetria-c10a-final.log`. Diff, file bounds, local doc links and secret
scan passed. The pushed commit/CI is recorded in the next sub-slice to avoid
a circular self-hash. C10 remains in progress until API/web integration.
G01-G12 remain UNVERIFIED; no production SMTP or real user mail was activated.

C10.a pushed: `7cf886ca14f2d125d179f92c70e33981b61c59ef` on `origin/dev`.
Exact PR workflow [43079570081](https://github.com/AlexandreZanata/Habimetria/actions/runs/43079570081)
(#383): backend and mobile-linux both passed before C10.b implementation.

## C10.b — Shared credentials and short-lived API authority

One password verifier serves browser and API issuance. The API audience is
ordinary-User-only, digest-stored, 15 minutes and invalidated durably on identity
changes. Existing panel roles, cookies and immutable collection data are retained.
Device refresh families remain C12. Real-DB tests cover separation, expiry,
revocation, staff denial and issuance with a stale credential.

Passed: disposable `make verify test-integration cover` and
`go test -race -cover ./internal/service/account` (94.0%). The inventory check
initially caught the new session table's missing privacy purpose/retention entry;
it was registered and the full command rerun successfully. Cleanup, diff,
new-file bounds, local links and secret checks passed. Local log:
`/tmp/habimetria-c10b-check.log`. Exact pushed commit/CI follows in the next update.

C10.b pushed: `c3a1a0171911bc277be112d0c116d535db41e952` on `origin/dev`.
Exact PR workflow [43081323611](https://github.com/AlexandreZanata/Habimetria/actions/runs/43081323611)
(#385): backend and mobile-linux passed before C10.c implementation.

## C10.c — Working JSON and browser integration

Bounded classified account routes and same-origin web forms use the shared
services, counters and identity. Native access resolves current ownership and
never falls back to browser cookies. Fragment handling supports new/old links
and signed-in browsers without GET consumption or automatic account switching.
API runtime reads only a separately mounted proof key; signup exposure requires
matching configured panel/mail origins. SMTP delivery remains in its worker.

Passed with disposable PostgreSQL 18.6, private objects and SMTP capture:
`make verify test-integration cover verify-commerce-config` and
`go test -race -cover ./internal/service/account ./internal/httpapi/...`.
Measured account 94.3%, API 84.3%, accountauth 100%, panel 80.8%, routepolicy
97.5%. The first coverage attempt caught uncovered new API handlers; direct
real-DB endpoint/failure tests restored the existing 78% API floor. No floor was
lowered; accountauth now enforces 95%. After adding runtime isolation tests,
reran disposable `make verify` and `go test -race -cover ./cmd/api` (18.5%).
All disposable cleanup checks passed. Logs: `/tmp/habimetria-c10c-check.log`
and `/tmp/habimetria-c10c-runtime-check.log`.

Actual Google Chrome 157.0.8103.50 headless/CDP validation ran the built API and
mail worker with synthetic data only: signup -> captured SMTP -> verification
fragment -> password -> cookie login -> owned user area. It also checked signed-in
legacy root fragments, immediate fragment removal, no proof in request URLs,
empty web storage, keyboard order, Manrope, 390px/1440px layouts and enlarged text
without horizontal overflow. Screenshots were inspected. No production mail,
credentials or customer data were used. Command:
`GOTOOLCHAIN=go1.25.13 python3 infra/test/run.py -- python3 /tmp/habimetria_c10_browser.py`.
Log `/tmp/habimetria-c10-browser.log`; local screenshots `/tmp/habimetria-c10-ui/`.
These are performed automated/visual checks, not owner accessibility acceptance.

Diff, local links, all new file bounds and secret scan passed. The implementation
commit is identified by `feat(account): connect verified web and JSON authentication`
in `origin/dev` history; exact final-head CI evidence is maintained on
[PR 21](https://github.com/AlexandreZanata/Habimetria/pull/21) without guessing a
circular self-hash. Main is unchanged. C11 recovery, C12 refresh/protected native
storage, C13 app account screens and C14 staff MFA remain pending. Production
mail activation/deliverability and G01-G12 remain UNVERIFIED.
