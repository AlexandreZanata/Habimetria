# Local PDF — Acceptance and Release Evidence

Status: acceptance specification; executed checks and unverified gates are
recorded separately in [execution evidence](local_pdf_execution.md).
References: [plan](local_pdf_plan.md),
[contract](../05-mobile/local_pdf_contract.md),
[legal scope](../03-architecture/brazil_report_legal_requirements.md).

## Orientation and image fidelity

- Synthetic asymmetric photos with readable UP/LEFT labels and corner markers:
  EXIF 1–8, portrait/landscape/square/panorama, JPEG and qualified HEIF inputs.
- Original fallback, normalized preview, missing preview and small images
  below the scaler threshold give the same display result on both platforms.
- Correct mirror/rotation, no double transform, no crop/stretch/upside-down
  photo; original hashes match before and after capture/review/PDF/export.
- Rapid device rotations and supported front/rear cameras retain shutter
  orientation. Unknown/malformed orientation follows a documented policy.
- Alter a preview while retaining its filename: regenerate/reject; never
  sign a Report showing substituted cached pixels as original Evidence.

## Completeness, page layout and reader interoperability

- Mixed fixture of 100 photos, multiple Rooms, long labels/Findings/notes,
  Unicode, missing/stale GPS, professional sections and ART/RRT attachments.
- Every expected Evidence ID/category, Finding and full note appears exactly
  as specified; each expected photo is visibly the correct image, not merely
  an extractable text reference. Manifest counts/sequence match the Report.
- Single photo, empty applicable sections, many portrait photos, panoramas,
  very long paragraph and many appendix digests exercise continuation pages.
- Actual page count equals `page/total` on every page; index points to the
  correct physical pages. No clipping, overlaps, missing glyphs or unreadable
  captions. QR works at realistic screen and A4 print sizes.
- Finish native contexts before consuming final bytes; reopen with independent
  PDF parsers/readers and render every page. Validate both Android and iOS
  outputs on desktop/mobile readers and A4 print. Logical content parity is
  required; cross-platform PDF byte equality is not promised.
- Native PDF output must not be labeled PDF/A/PAdES without corresponding
  conformance/signature validation. Missing/corrupt image decoding cannot
  produce READY, even if its file exists and the renderer can finish.

## Immutable inputs and adversarial verification

- After seal, change live Property address/author data/Room/Findings/coverage;
  the Report retains only its signed snapshot or rejects inconsistent inputs.
- Missing originals, changed original bytes, modified location/time/identity,
  substituted preview, reordered Evidence, duplicate IDs and forged attachments
  fail the relevant checks. Hash agreement alone does not prove truthful GPS.
- Change one PDF byte, transplant the QR, replace receipt/key/Manifest and
  re-sign with an unknown key. Each verdict remains explicit; unknown civil
  identity never becomes trusted merely from self-included public material.
- Validate current and historical packages in Kotlin and `backend/cmd/verify`.
  Test unsupported versions, malicious archive paths and oversized files.
- Key loss/rotation/reinstallation: existing Reports verify with preserved
  historical public material; new issuance has a distinct identity.
- READY cannot be overwritten. Changed rendering/PAdES signing gets a new
  Report ID and final receipt; earlier distributed Reports remain identifiable.

## Offline, location and crash recovery

- Generate, reopen, export and independently verify with API stopped and app
  HTTP instrumented/blocked. Fonts/maps/tiles/geocoding are unnecessary.
- GPS granted precise/approximate, denied, services disabled, no fix, old fix,
  negative/invalid accuracy, clock changes and detected/unknown simulation.
  Coordinates match the individual Evidence, not another photo or export time.
- Check Android monotonic age and iOS source-information behavior on supported
  physical devices; simulator results do not establish hardware trust.
- Kill during source validation, rendering, writer close, PDF rename, receipt
  write, DB commit and package export. Recover committed bytes/status without
  returning SEALED to DRAFT or allowing incomplete artifacts to verify.
- Low storage, allocation pressure, cancellation, repeated taps and restart
  retain originals and prior READY Reports; temporary output is reconciled.
- Measure time, PDF size and peak memory by device/build/quality/workload.
  Retain existing targets (<10 s and <10 MB for 100 photos) as unverified
  targets. Correctness/readable defects take precedence over compression.

## Both presentations and privacy

- Tenancy flow remains photo + preset + optional note; descriptive text cannot
  imply licensed diagnosis. Agreement/receipt/disagreement are separate from
  share/download. No uncollected signatory or witness is invented.
- Professional finalization validates required scope/author/ART/RRT inputs;
  missing inputs cannot yield a claimed complete regulated technical Report.
  Declared registrations never appear as live council-confirmed offline.
- Professional Inspection works directly under Property without a fictitious
  Tenancy after the reviewed domain extension. Existing entry/exit links and
  old sealed packages remain valid through migration/version changes.
- A professional approves technical conclusions; private Reports are not
  labeled judicial expert reports without the applicable qualified workflow.
- Review selected licensed standards, contract, municipality and recipient
  rules against an applicability checklist before compliance claims. Unreviewed
  clauses/locations prevent that claim, not basic descriptive local export.
- Covered deployments have data-purpose/basis/retention/privacy decisions,
  private file/key protection and controlled exports. Redacted copies are
  separately identified; legal retention does not mean indefinite storage.
- Optional ICP-Brasil/PAdES uses real authorized identity and validates under
  its selected ITI profile. ACT time is receipt-time evidence, not certified
  shutter time. Installation receipts are never mislabeled as government
  signatures or supported ITI formats without actual interoperability.

## Required proof per slice and before release

`make verify` (Go) is mandatory; relevant mobile changes also run
`./gradlew :shared:jvmTest :composeApp:assembleDebug` for this delivery.
Current user instruction defers all emulator/physical/native-PDF/visual
qualification to manual testing after all phases and excludes iOS checking.
These manual release criteria are not automated execution authorization.
For a separately authorized future iOS release, use `make verify-ios-macos`
on macOS/Xcode;
Linux Kotlin compilation does not validate the native runtime/Swift host.
Use semantic tests for corruption, omission, orientation and snapshot isolation,
plus visual/native-device checks. Documentation-only planning is not a mobile
release and needs no fabricated physical-device pass.

Record build/commit, OS/device, scenario, expected/observed result, quality
settings, measurement method, logs, rendered-page screenshots, synthetic
PDF/packages and independent verifier output. No real addresses, locations,
photos, signatory personal data or private keys enter committed fixtures.
Release requires no unresolved data loss, omitted/distorted Evidence,
incorrect numbering or false integrity/identity/time/location statement.
Every unexecuted check remains explicitly unverified.
