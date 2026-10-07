# Acceptance Report — Local Validation (L8, Linux Pass)

First executable pass of [the acceptance matrix](local_validation_acceptance.md).
Build: `f8a06d8` on `codex/local-validation-roadmap`, 2026-09-30 (code HEAD).
Consolidation refresh: docs status updated 2026-10-01 (no code change); validation below was executed against `f8a06d8`.
Re-measurement: 2026-10-01 on `a7fdb25` (merge of the above, no code change):
`make verify-all` green, `:shared:jvmTest` 109/109 across 35 suites,
`PerfProbe` best-of-3 `canonicalManifest+sha(100ev)` 1 ms /
`paginateModel(100photos)` 0 ms (JVM host-relative),
release APK `composeApp-release-unsigned.apk` **8.5 MB**,
independent verifier `accepted: true` (2 evidences, issuer `UNKNOWN`).
Re-measurement: 2026-10-01 on `c24e084` (`chore/release-hardening`:
CI mobile fix, env-parity gate, PG18 volume fix):
`make verify-all` green, `:shared:jvmTest` 109/109 across 35 suites,
`make test-integration` passed against real PostgreSQL 18.6-alpine
(dedicated container via `TEST_DATABASE_URL`; skip verified when unset),
independent verifier `accepted: true` (2 evidences, issuer `UNKNOWN`),
release APK `composeApp-release-unsigned.apk` **8.5 MB** vs 15 MB budget,
`docker compose config` renders with healthy `postgres`/`minio` deps and
the API `/healthz` gate. PerfProbe best-of-3 not re-run (prior 1 ms / 0 ms
stands as the last measured value).
Scope since `a4603ab` adds V1.1 occurrences, V1.2 exit, V1.3 compare, V1.4 inspector, V1.5 visual diff, and optional V-sync server mirror.
Host: Linux x86_64, JDK 21 Temurin, Gradle 8.14.3, Android SDK
(platforms 34/35/36/36.1), Go 1.25.0, Kotlin 2.3.10, AGP 8.13.2,
Room 2.8.5, Compose Multiplatform 1.11.1.

## Executed checks (all green)

- `make verify`: Go vet + `go test ./...` — `cmd/verify` (5),
  `internal/httpapi`, `internal/service` pass.
- `make verify-mobile`: 109/109 JVM tests pass (35 suites: 26 `vault.local` plus `vault.nav`, `vault.sync`, `QueueTest`, `RoomCoverageTest`, `VaultApiTest` incl. exit/comparison/occurrence),
  `:shared:compileKotlinIosSimulatorArm64`,
  `:composeApp:assembleDebug` green.
- `make verify-all`: green.
- Independent cross-check: `go run ./cmd/verify --format json
  testdata/fixture-package` → `accepted: true`, evidences 2 checked,
  issuer `UNKNOWN` (fixture key unpinned, as designed).
- `make bench`: `canonicalManifest+sha(100ev)` 1 ms,
  `paginateModel(100photos)` 0 ms on 2026-10-01 re-measurement at `a7fdb25`
  (earlier Linux pass: 2 ms / <1 ms; JVM host-relative, not device claims).
- `make bench-apk`: release R8 APK **8.5 MB unsigned** vs 15 MB budget
  (re-measured 2026-10-01 at `a7fdb25`; same size as the `a4603ab` reading).

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
- Evolution and mirror: `OccurrenceTest`, `ExitInspectionTest`,
  `CompareTest`, `AiInspectorTest`, `AiDiffTest`, `ServerMirrorTest`
  (LOCAL_ONLY skip, explicit failure, no `VaultApi` from screens),
  `DbMirrorStoreTest` (first-write-wins links); API `occurrence`,
  `comparison`, and exit-kind guard covered by Go tests.

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
- `app/shared/schemas/vault.local.db.VaultDatabase/{1..11}.json` — schema history with migrations (v11 adds `mirror_links`).

## Explicitly unverified (not claimed)

Physical Android/iOS devices, emulators/simulators, window-size and
foldable matrix, TalkBack/VoiceOver, large text on device, real camera
and GPS behavior, 100-photo render under 10 s, PDF readability in
independent readers, signed release install, Swift/Xcode compile and
link, Baseline Profiles, cold start, shutter latency, ANR rates.
"All devices" is not a test result.

## U12 offline acceptance pass (Linux, 2026-10-01)

Build: `7dfee44` on `codex/local-usability-plan` (U01–U11 complete).
Host: Linux x86_64, JDK 21 Temurin, Gradle 8.14.3, Go 1.25.0,
Kotlin 2.3.10, AGP 8.13.2, Room 2.8.5. The suites below are the
automatable subset of the [usability acceptance gates](local_usability_acceptance.md);
device and five-user sessions stay explicitly unverified (see below).

- `make verify-all` green: Go vet + `go test ./...` pass;
  `:shared:jvmTest` **143/143 across 39 suites** (0 failures), including
  the U07–U09 removal trilogy (`PropertyRemovalTest` 5/5,
  `RestoreValidationTest` 5/5), the U11 bulk set (`BulkPerformanceTest`
  3/3: 200 overviews 4 ms, coverage over 75 inspections 21 ms, 3 MB
  evidence write 18 ms vs the 500 ms budget) and the navigation/recovery
  suites; `:shared:compileKotlinIosSimulatorArm64` and
  `:composeApp:assembleDebug` green.
- `make bench`: `PerfProbe` best-of-3 `canonicalManifest+sha(100ev)` 3 ms /
  `paginateModel(100photos)` 1 ms (JVM host-relative, not device claims).
- `make bench-apk`: release R8 APK
  `composeApp-release-unsigned.apk` **8.2 MB** vs the 15 MB budget.
- Independent verifier `accepted: true` on
  `backend/testdata/fixture-package/` (signature valid, issuer `UNKNOWN`).
- `make test-integration` **PASS against real PostgreSQL 18.6-alpine**
  (dedicated container via `TEST_DATABASE_URL`; skip verified when unset).

Gate mapping (usability acceptance): navigation/comprehension gates 1–5,
removal/integrity gates 1–6 (minus on-device relaunch/TalkBack items),
offline-independence (LOCAL_ONLY paths, API-stopped design) and the
bulk/coverage scale checks execute here. Appearance/a11y on-device items,
window-matrix items, release timings, camera/GPS behavior and the five
first-time-user sessions require hardware and stay open.

Residual risks: sealed-package tamper rejection is proven by unit tests,
not by tampered files on a device; accessibility beyond static semantics
(headings, live regions, 48 dp targets) needs TalkBack/VoiceOver runs;
cold start, shutter latency and 100-photo render need the reference
device; no data-loss/false-trust defect is known, none is claimed absent
on hardware.

## Explicitly unverified environments (U12)

Physical Android/iOS devices and emulators/simulators, macOS/Xcode
(`make verify-ios-macos`), window-size/foldable/split-screen/cutout
matrix, TalkBack/VoiceOver, 200% font scale on device, real camera and
GPS, release cold start/shutter/100-photo timings, ANR rates, signed
release install, Baseline Profiles collection, five-user usability
sessions. "All devices" is not a test result; no beta without the
[beta runbook](beta_readiness.md) matrix filled.

## Linux re-pass on the hardened branch (2026-10-07)

Base: `1ca3c17` on `codex/legal-report-hardening` (usability plan plus
PDF07 hardening: explicit external-signing intent, PAdES readiness,
contracted-ACT token binding, offline revocation policy, outline
consistency). Host: Linux. Code gates only; no device or user session
was run.

- `make verify`: Go vet + `go test ./...` pass.
- `:shared:jvmTest` (forced rerun): **194/194 across 65 suites**,
  0 failures (includes PAdES readiness, timestamp ABSENT/EXTERNAL,
  revocation UNKNOWN, external intent/preservation and outline cases).
- `:composeApp:assembleDebug` and `:composeApp:assembleDebugAndroidTest`:
  green as compilation only. Instrumented tests were not executed and no
  APK was installed. iOS checking excluded.
- U12 device runs, five-user sessions, PDF08 manual validation and the
  beta device matrix stay explicitly unverified and user-owned.

## Device-pass instructions (macOS + hardware)

Execute the [beta readiness runbook](beta_readiness.md), which consolidates
the steps below with the M5 beta exit criteria. Summary:

1. `make verify-ios-macos` (Xcode build + framework export).
2. Install release on one mid-range Android + one iPhone; block
   network; run the acceptance matrix scenarios 1–11; photograph
   every Eligibility/Review/PDF/Verify screen.
3. Tamper copies (one byte in original/PDF/manifest/label) and confirm
   rejection with missing-vs-modified labels.
4. Record device, OS, build SHA, timings and verifier outputs here;
   file defects before beta. No beta without this section filled.
