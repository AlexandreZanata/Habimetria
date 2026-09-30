# Acceptance Report — Local Validation (L8, Linux Pass)

First executable pass of [the acceptance matrix](local_validation_acceptance.md).
Build: `a4603ab` on `codex/local-validation-roadmap`, 2026-09-30.
Host: Linux x86_64, JDK 21 Temurin, Gradle 8.14.3, Android SDK
(platforms 34/35/36/36.1), Go 1.22.2, Kotlin 2.3.10, AGP 8.13.2,
Room 2.8.5, Compose Multiplatform 1.11.1.

## Executed checks (all green)

- `make verify`: Go vet + `go test ./...` — `cmd/verify` (5),
  `internal/httpapi`, `internal/service` pass.
- `make verify-mobile`: 75/75 JVM tests pass (suites below),
  `:shared:compileKotlinIosSimulatorArm64`,
  `:composeApp:assembleDebug` green.
- `make verify-all`: green.
- Independent cross-check: `go run ./cmd/verify --format json
  testdata/fixture-package` → `accepted: true`, evidences 2 checked,
  issuer `UNKNOWN` (fixture key unpinned, as designed).
- `make bench`: `canonicalManifest+sha(100ev)` 2 ms,
  `paginateModel(100photos)` <1 ms (JVM host-relative, not device claims).
- `make bench-apk`: release R8 APK **8.5 MB unsigned** vs 15 MB budget.

## Automated regression map

- Data loss: `OfflineJourneyTest` (full journey + relaunch resume),
  `LocalDatabaseTest` (close/reopen), `MigrationTest` (v1 file replay),
  `RecoveryTest` (disk-full atomicity, corrupt queue surfacing),
  `LocalVaultTest` (concurrent sequences unique).
- Pagination/completeness: `PdfContractTest` (100 photos exactly once,
  findings, appendix, rect fit), `LocalVaultTest` (repeats never fake it).
- Canonical signing: `ManifestCodecTest` (byte-exact, order-free),
  `SealSignerTest` (roundtrip, persistence, DER/raw), `TrustIssuerTest`.
- Tampering: `LocalSealTest` (label/signature), `ReportPipelineTest`
  (PDF bytes), `PackageFormatTest` (pdf/originals/key substitution),
  `cmd/verify` (same five, independent runtime).
- Network independence: `OfflineJourneyTest` (LOCAL_ONLY, no API in
  path), `LocationTest` (no fabricated coordinates).

## Defects found and fixed in L8

- Stale test directory reused across runs caused a false UNIQUE
  failure; fixed by directory cleanup (own suite caught it).
- Corrupt `queue.json` loaded as silent empty; migration now strict
  (`loadStrict`) with startup surfacing via `migrationError`.
- Trailing comma in the Go receipt rebuild broke cross-verification;
  the shared fixture test caught it before merge.

## Sample artifacts (committed)

- `backend/testdata/fixture-package/` — 2-photo package exported by
  the app, accepted by both verifiers.
- `app/shared/schemas/.../{1..5}.json` — schema history with migrations.

## Explicitly unverified (not claimed)

Physical Android/iOS devices, emulators/simulators, window-size and
foldable matrix, TalkBack/VoiceOver, large text on device, real camera
and GPS behavior, 100-photo render under 10 s, PDF readability in
independent readers, signed release install, Swift/Xcode compile and
link, Baseline Profiles, cold start, shutter latency, ANR rates.
"All devices" is not a test result.

## Device-pass instructions (macOS + hardware)

1. `make verify-ios-macos` (Xcode build + framework export).
2. Install release on one mid-range Android + one iPhone; block
   network; run the acceptance matrix scenarios 1–11; photograph
   every Eligibility/Review/PDF/Verify screen.
3. Tamper copies (one byte in original/PDF/manifest/label) and confirm
   rejection with missing-vs-modified labels.
4. Record device, OS, build SHA, timings and verifier outputs here;
   file defects before beta. No beta without this section filled.
