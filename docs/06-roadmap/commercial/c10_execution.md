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
