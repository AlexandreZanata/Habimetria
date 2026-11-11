# Beta Readiness — Device Pass Runbook

Consolidates the already-planned gates from
[mobile phases](mobile-phases.md) (M5),
[acceptance](local_validation_acceptance.md) and the
[acceptance report](acceptance-report.md).
No new scope; Linux green is the entry ticket, devices are the exam.

## U12 Linux status (2026-10-01, build `7dfee44`)

Entry gates green on Linux: `make verify-all` (Go + 143/143 JVM tests),
`make test-integration` PASS on real PostgreSQL 18.6-alpine,
`make bench` 3 ms / 1 ms, `make bench-apk` 8.2 MB vs 15 MB,
independent verifier `accepted: true`. Full numbers and gate mapping in
the [acceptance report](acceptance-report.md). Still open and blocking
beta: the device matrix below, the five-user sessions per
[usability sessions](usability_sessions.md), and reference-device timings.

## Entry gates (must already be green)

- `.github/workflows/verify.yml` green on the release commit:
  `make vet test`, `make test-integration` (Postgres 18.6 service),
  independent verifier on `backend/testdata/fixture-package/`,
  plus `make verify-mobile` on Linux.
- `make test-integration` passes against a real PostgreSQL 18.6;
  an unconfigured suite (skip) never counts as DB integration.
- `infra/.env.example` matches `config.FromEnv` defaults;
  `docker compose -f infra/docker-compose.yml config` renders with
  `postgres`/`minio` healthy dependencies and the API `/healthz` gate.
- `APP_ENV=production` boot fails fast without a random 32+ byte `HMAC_KEY`,
  independently of billing; non-loopback/HTTPS advertised origins also enforce it
  (`config.Validate`, covered by `internal/config` tests).
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
