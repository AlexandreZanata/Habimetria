# C13 — Connected Account Screens Execution

Started 2026-11-14 at `48648bb083b166f365b8882d7da32d9b21d98f47` on dev.
Contracts: [authentication](../../04-backend/account_authentication.md),
[recovery](../../04-backend/account_recovery.md) and
[mobile experience](../../05-mobile/subscription_experience.md).

## Public account transport

`AccountEnrollment.kt` connects registration, verification, recovery initiation
and password replacement to the real JSON endpoints through the existing pinned
HTTPS session transport. Initiation retains the server's neutral response; a
successful status code alone is insufficient without the expected acknowledgment.
Requests are bounded to the API's 4096-byte limit. No new local collection access,
automatic login or production mock is introduced. Tests exercise all four routes,
explicit acknowledgment, oversized requests and insecure-origin denial.
`make verify`, `make verify-mobile` and the three account-enrollment tests
passed locally. Logs: `/tmp/habimetria-c13a-verify.log` and
`/tmp/habimetria-c13a-final-mobile.log`. These checks do not constitute a
native UI or live-provider acceptance test.

The C13 UI, profile/device controls, logout queue isolation and native screen
verification are not delivered by this transport. The current Android default
and iOS development origin are HTTP, which the account transport rejects; a
configured HTTPS origin is required before connected features can operate.
Local work remains independent. Native device, iOS and owner acceptance remain
UNVERIFIED. The implementation commit and CI are recorded in the PR.
