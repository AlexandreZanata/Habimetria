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
The pushed SHA/exact CI is recorded in the next sub-slice, without a circular hash.

Android/iOS protected storage follows as separate sub-slices; C12 remains in
progress. Native account screens remain C13. G01-G12, native runtime and all
owner-controlled store/release/device gates remain UNVERIFIED.
