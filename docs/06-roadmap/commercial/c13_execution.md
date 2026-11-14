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

## Account UI and device controls

Account screens in `app/composeApp/src/commonMain/kotlin/vault/ui/account/`
connect signup, verification, sign-in, recovery, profile, session list and
confirmed device revocation to the real services. The user selects an HTTPS
origin explicitly; the old development HTTP origin cannot receive credentials.
Saved credentials remain pinned to their original origin and User. Email links
are reduced to bounded purpose-specific proofs before submission. Passwords and
proofs are transient state, never navigation or saved-instance state.

Logout forgets connected credentials before network revocation and reports when
remote revocation is unconfirmed; explicit recovery of corrupt protected state
also discloses that limitation. It does not delete local collections. Native apps
currently use LOCAL_ONLY and have no owner-scoped cloud queue to cancel. Future
owner-scoped workers must implement the C16 account-switch and cancellation
contract before activation; this screen does not enroll or start cloud sync.

Local `make verify`, Kotlin shared JVM tests, Android APK and shared/Compose iOS
Kotlin compilations passed. Profile scope, authorization denial, saved-origin
isolation and proof purpose have focused tests. Disposable real PostgreSQL 18.6
integration passed with cleanup (`/tmp/habimetria-c13-integration.log`). On a
clean Android emulator, verified local startup with location denied, Settings →
Account navigation, HTTPS-origin entry, login and registration layout using a
synthetic origin; no real credentials or server request were used. Logs:
`/tmp/habimetria-c13-final-verify.log`, `/tmp/habimetria-c13-final-all.log`.

Live HTTPS account/mail delivery, full device-session interaction and owner
accessibility checks still need an actual configured environment and user-owned
validation. iOS host, Keychain runtime and owner release gates remain UNVERIFIED.
The commit and exact-head CI are recorded on the PR; a queued run is not a pass.
