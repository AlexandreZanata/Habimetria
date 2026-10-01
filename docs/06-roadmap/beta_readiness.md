# Beta Readiness — Device Pass Runbook

Consolidates the already-planned gates from
[mobile phases](mobile-phases.md) (M5),
[acceptance](local_validation_acceptance.md) and the
[acceptance report](acceptance-report.md).
No new scope; Linux green is the entry ticket, devices are the exam.

## Entry gates (must already be green)

- `make verify-all` green on the release commit.
- `make bench-apk` release APK vs 15 MB budget recorded.
- Independent verifier accepts `backend/testdata/fixture-package/`.
- No unresolved data-loss, missing-evidence, false-trust or safe-area defect.

## Device matrix (record, do not claim)

- macOS: `make verify-ios-macos` (framework export + `xcodebuild` host build).
- One mid-range Android + one iPhone, API stopped, network blocked:
  scenarios 1–11 of `local_validation_acceptance.md`.
- Window classes: small/large phone, tablet, foldable, split-screen,
  portrait/landscape, light/dark, keyboard open/closed, large fonts.
- Camera/location matrix + TalkBack/VoiceOver pass per the acceptance doc.
- 100-photo fixture: all photos and findings present in extracted PDF text
  and visual inspection; opens in independent readers; QR scans to local
  verification (never a local server URL).

## Evidence to record per run

Commit/build ID, toolchain (Xcode/iOS versions on macOS), OS/device,
scenario, expected/observed result, test logs, screenshots or recordings,
sample PDF/package, verifier output, timings (cold start, shutter latency,
100-photo render). Use synthetic fixtures; exclude keys, real addresses
and real-user location/photos.

## Beta exit criteria

- Closed beta (100 users) per M5: crash-free > 99.5%, ANR < 0.5%.
- All budgets in `app-performance.md` green on 2 mid-range devices.
- Anything not executed stays explicitly unverified; "all devices"
  is not a test result. No beta without this file's matrix filled.
