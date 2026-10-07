# Local PDF — Implementation and Executed Evidence

Evidence date: 2026-10-02. Status: implemented local core with Android
emulator proof; physical-device and iOS runtime release qualification pending.
References: [delivery plan](local_pdf_plan.md),
[acceptance gates](local_pdf_acceptance.md),
[implemented contract](../05-mobile/pdf-ondevice.md),
[Brazilian legal scope](../03-architecture/brazil_report_legal_requirements.md).

## Delivered behavior

- PDF01: reviewed rental/professional details, direct Property ownership for
  technical Inspection, SQLite v16 migration and canonical Manifest v3.
  Frozen author, observations, dimensions and attachment digests are used
  instead of later live edits. Legacy sealed bytes remain unchanged.
- PDF02: EXIF 1–8 rotation/mirror normalization, bounded decoding and contain
  geometry; originals are unchanged. Portrait/landscape proportions survive.
- PDF03: native font measurement, complete notes and Unicode, physical index,
  correct final numbering, coverage gaps and verified-original photos.
- PDF04: both reviewed presentations, required professional declarations and
  ART/RRT PDF attachment, author acknowledgment and readable integrity appendix.
- PDF05: explicit device time/offset, missing/denied/disabled GPS, accuracy,
  fix age/provider, nullable simulation signal and separate EXIF coordinates.
- PDF06: original/attachment validation before and after rendering, detached
  installation signatures, immutable READY/history and retained public keys.
  Bounded ZIPs are reimported before publication; native document pickers,
  Kotlin and Go verification separate integrity from issuer trust.
- PDF07: official legal applicability documented; local completeness checks
  implemented. External identity/signature and professional qualification are
  not claimed complete; remaining gates are listed below.
- PDF08: JVM, native Android emulator and independent artifact proof executed.
  Physical hardware/iOS/print/memory qualification remains pending.

The numbering maps delivery scope, not an assertion that every acceptance
gate has passed. Optional external signing and professional qualification
remain separate work. No signature/QR/GPS proves the scene or civil identity.

## Published review slices

- Orientation: `7dd8065`, [PR #5](https://github.com/AlexandreZanata/mvp-aplicativo-pdf-prova-de-vistoria/pull/5).
- Frozen sources: `e866ecc`, [PR #6](https://github.com/AlexandreZanata/mvp-aplicativo-pdf-prova-de-vistoria/pull/6).
- Pagination: `61da8cf`, [PR #7](https://github.com/AlexandreZanata/mvp-aplicativo-pdf-prova-de-vistoria/pull/7).
- Profiles/history: `1e72816`, [PR #8](https://github.com/AlexandreZanata/mvp-aplicativo-pdf-prova-de-vistoria/pull/8).
- Independent verifier: `12b3ec9`, [PR #9](https://github.com/AlexandreZanata/mvp-aplicativo-pdf-prova-de-vistoria/pull/9).

Final integration and qualification evidence follow these dependent slices.
Review branches are drafts; no merge or production release is implied.

## Automated checks

`make verify`: Go vet/tests passed. Final mobile command passed:

```sh
cd app
./gradlew :shared:jvmTest :composeApp:assembleDebug \
  :composeApp:assembleDebugAndroidTest :composeApp:compileKotlinIosSimulatorArm64
```

JVM results: 169 tests, zero failures/errors/skips. Semantic cases include
snapshot isolation, changed/missing sources, substituted previews, immutable
READY/history, interrupted publication/retry, key rotation, package bounds,
professional completeness/attachment tampering and real populated v15→v16
SQLite migration preserving IDs, links and frozen signed bytes.
Linux Kotlin/iOS compilation passed; it is not a macOS/Xcode runtime pass.
Delivery debug APK: about 33 MiB; SHA-256
`9661e34d3411ee1b6cb50041efaf029d13374c3a673f309d6da791b5397bbd46`.
This final build restores explicit trust revocation in the verification UI;
the native PDF run below used the preceding APK with the same PDF pipeline.

Android native fixture sources are indexed in
[the probe README](../../app/composeApp/src/androidInstrumentedTest/kotlin/vault/pdf/README.md).
[The independent PDF fixture checker](../../app/tools/README.md) validates
text, image placements/decoded corner colors, proportions, index and bounds.

## Native Android run

Device: `emulator-5554`, Android SDK x86_64 emulator, Android 8.0/API 26.
Build: debug APK; SHA-256:
`8ed056a87e77b97cb16002a4794896a9b175f79d8f856b5ee395dbdc90a94246`.
Original images: synthetic 1200×800/800×1200 JPEGs with asymmetric colored
corners; all eight EXIF cases and mirrors. No real user data was inspected.
Instrumentation used an isolated `files/pdf-device-probe` directory and the
production Android PDF renderer, Keystore signer and ZIP pipeline.

- Rental: 100 photos, five Rooms, 114 A4 pages; 9,109,884 bytes; 6,258 ms.
- Professional: 20 photos, five Rooms, 29 A4 pages; 1,860,902 bytes; 1,727 ms.
- Maximum sampled process PSS: 465,474 KiB (about 455 MiB). Sampling ran at
  page-progress callbacks; it is not a continuous allocation profile.
- Timings span generation, verification, native PDF reopening and ZIP
  export/reimport. They exclude capture/sealing and are not render-only.
  This is one final debug/emulator run, not a hardware performance guarantee.
- Native EXIF dimensions/all four corners passed for cases 1–8.
- 100 varied QR identities decoded through normal ZXing detection using the
  exact production bitmap function; both renderers use correction level M.
- Native PdfRenderer reopened every page and checked actual A4 dimensions.
- Kotlin receipt/source verification and exported ZIP round trips passed.

The professional fixture declares synthetic author/registration/ART data
and attaches a synthetic PDF explicitly named `SYNTHETIC-PDF-NOT-AN-ART.pdf`.
It is neither a real professional report nor a registered ART example.
Rotated text inside asymmetric test images is intentional to exercise EXIF;
decoded corner positions establish the expected transformation.

## Independent artifact checks

- Poppler rendered all 143 pages without warnings. All seven contact sheets
  and representative full pages were visually inspected: no stretching,
  clipped content, numbering errors or misplaced images observed.
- pdfplumber/pypdf verified all 120 photo placements, decoded corner colors,
  oriented ratios, complete long Unicode notes, category labels, physical
  index references, page/total values and character/page bounds.
- Both embedded QR resources decoded independently and matched their
  Report/Manifest identities. Real screen-camera/A4 print scanning is pending.
- Go verification accepted both packages: valid signatures and all originals
  checked; issuer trust remained `UNKNOWN`. An included key is not a trusted
  civil identity. PDF, Manifest, receipt, photo, responsibility attachment and
  signer metadata byte-flip attacks were each independently rejected.

Final synthetic rental PDF SHA-256:
`3ade4a7abe45d653dca83717342ebee5b24c3f1e39d4ce342117b55b7eb3b1a4`.
Final synthetic professional PDF SHA-256:
`47896d5b7fb72fb2a05ba5908c3b5e02f15a3d9dd69d7776679a8318b288d6ee`.
PDFs/logs/verdicts are delivered as workspace artifacts, not committed media.

## Release gates still unverified

- Physical camera/device rotations, front/rear lenses, real sensor permission
  matrix, approximate/stale/mock readings, clock changes and HEIF variants.
  The phone OS rejected installation of the test APK; this restriction was
  respected. Synthetic emulator claims are not hardware GPS validation.
- iOS execution on macOS/Xcode, Swift host, native PDF/camera/location/picker
  behavior and reader parity. Core Location now requests foreground one-shot
  refreshes without making capture wait, but hardware behavior is unverified.
- Low-RAM devices, release-build memory/time/quality and large report budgets.
  The high sampled PSS requires qualification before claiming broad support.
- Actual airplane-mode/blocked-HTTP journey, real print and cross-reader checks.
  The native probe uses local-only production APIs; no simulated server ack.
- Process kills at each file/DB publication boundary, storage exhaustion,
  cancellation under load and attachment/package orphan recovery matrix.
- Licensed ABNT clause review, applicable service/municipality/recipient rules,
  professional attribution and council/ART/RRT authenticity validation.
- Deployment-specific LGPD purpose/basis, retention, backup/export controls
  and privacy decisions. Inspection removal cannot erase external shared copies.
- ICP-Brasil/PAdES, Gov.br or ACT timestamp integrations are not implemented.
  The installation receipt does not replace them or certify shutter time.
- Optional PDF bookmarks and a manual display-correction editor are not shipped.

The implemented guarantee is detection of byte changes against signed
references with a separately trusted key. It does not guarantee truthful
declarations, universal legal acceptance, ABNT conformity or civil identity.
