# C12 — Protected Rotating Native Sessions Execution

Date: 2026-11-11. Starting commit: `e0848ae866f43038ae877a991a927d1ed665d3b7`.
Contract: [native sessions](../../04-backend/device_sessions.md).

## C12.a — Session and rotation backend

Device-aware login, family-scoped API authority, single-use refresh history,
committed replay revocation, own-session controls and identity/reset invalidation.
Legacy short-lived API and browser login remain compatible. No paid/local gates.
Real-DB tests cover rollback, concurrent consume, two devices, expiry, metadata,
scope and technical limits. Passed in disposable PostgreSQL 18.6/private
storage/SMTP: `make verify test-integration cover`; targeted account race checks
(`Device|Refresh|Families`). After adding HTTP integration cases, reran disposable
`make verify` and full HTTP race/coverage (85.5%). All existing coverage floors
passed; the filtered account race run is not a full-package coverage claim.
Privacy schema inventory, new-file bounds, local links, secret and diff checks
passed. All disposable resources were removed. Logs:
`/tmp/habimetria-c12a-check.log`, `/tmp/habimetria-c12a-api-check.log`.
C12.a was pushed to dev as `c2a4c213fe9bb4226a2de7924f0e83ce13e970a6`.
Exact-head CI [#393](https://github.com/AlexandreZanata/Habimetria/actions/runs/43096912855)
was queued without jobs when inspected. The official GitHub status API reported Actions
major_outage on 2026-11-11; independent native implementation continued while
this external check was unavailable. Queued CI is not a pass.

## C12.b — Shared coordinator and Android protected storage

Added lazy origin-pinned HTTPS transport, bounded/redacted credentials,
serialized single-use refresh and durable pending state before any consume.
Access stays in memory with monotonic expiry. Devices/revocation and logout
preserve User/family scope; local forgetting precedes network and reports remote
revocation separately. Optional AppContainer injection leaves local work free.

Android uses non-exportable AES-GCM Keystore keys, app/namespace authenticated
binding and a no-backup AtomicFile with data/directory sync and readback. Locked,
corrupt or unavailable storage fails closed; explicit recovery only erases account
credentials. No plaintext fallback or collection/database changes.

Passed `make verify`, `make verify-mobile` (12 new session tests included), Android
test APK build and isolated AccountStorageRunner on 2311DRK48G / Android 16:
non-exportable key, encrypted bytes, restart, competing compare-and-set, pending
state, corruption denial and explicit erase/reuse. The probe uses only a random
namespace, removed afterwards. APKs were installed in place without deleting app
data. Runner selection is a Gradle property; PDF probes retain their default.
Logs: `/tmp/habimetria-c12b-backend-check.log`,
`/tmp/habimetria-c12b-final-mobile.log`, `/tmp/habimetria-c12b-probe-build.log`,
`/tmp/habimetria-c12b-device-check.log`. This is automated native evidence,
not owner locked-device, visual, accessibility or release acceptance.
C12.b was pushed to dev as `8e038facb534a4b7954a2fbb16c4fb62236ef773`.
Exact-head required `backend` and `mobile-linux` passed in
[CI #395](https://github.com/AlexandreZanata/Habimetria/actions/runs/43099441971),
covering the earlier backend commit as well. The previous queue is not treated
as evidence; these later successful jobs are the independent recorded result.

## C12.c — iOS protected storage

Added bounded Keychain generic-password operations with
WhenUnlockedThisDeviceOnly, no synchronization or shared access group. Background
compare-and-set/readback preserves the shared pending-rotation contract. A
non-secret installation marker is excluded from backup; a missing marker purges
surviving credentials before acknowledgment. Purge failure/corruption denies
connected authority; explicit forget can recover without touching local data.
MainViewController injects the lazy adapter. Android path resolution is now lazy
as well, avoiding account-directory IO at local startup.

Passed `make verify`, `make verify-mobile`, shared Kotlin compilation for
iosArm64/iosSimulatorArm64 and Compose UI Kotlin compilation for
iosSimulatorArm64. The extra UI compile exposed a pre-existing missing
`kotlinx.coroutines.IO` import in PropertyCoverPicker; fixed the import and
reran successfully. These are compilations, not an Xcode/Swift host/link pass.
All 16 session/installation tests passed, including four real temporary-filesystem
marker tests for reinstall, purge-before-acknowledgment, retry and corruption.
Local documentation links/file bounds and diff checks passed. Logs:
`/tmp/habimetria-c12c-backend-check.log`, `/tmp/habimetria-c12c-mobile-check.log`,
`/tmp/habimetria-c12c-final-mobile.log`. Secret checks passed before committing.

C12.c was pushed to dev as `c616cb8d427e508313b5e3a02298852f680d4e53`.
Required `backend` and `mobile-linux` both passed in
[CI #397](https://github.com/AlexandreZanata/Habimetria/actions/runs/43112608475).
All 232 JVM tests passed locally, with zero failures/errors/skips. C12 is
CODE_VERIFIED. The final evidence-only commit is identified by
`docs(auth): record verified native session delivery` in dev history;
its exact-head CI is maintained on
[PR 21](https://github.com/AlexandreZanata/Habimetria/pull/21) without a circular
self-hash. Main is unchanged; C13-C64 remain pending.

Native account screens remain C13. Apple Keychain runtime/entitlements,
locked-screen behavior, reinstall/backup/restore, G01-G12 and all owner-controlled
store/release/device/visual-PDF/user-session gates remain UNVERIFIED.
