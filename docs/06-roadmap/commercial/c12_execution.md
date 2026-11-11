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
remains queued without jobs. The official GitHub status API reported Actions
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

iOS Keychain storage follows as a separate sub-slice; C12 remains in progress.
Native account screens remain C13. G01-G12, iOS runtime and all owner-controlled
store/release/device gates remain UNVERIFIED.
