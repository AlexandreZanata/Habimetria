# Local Validation Roadmap — Android and iOS

Status: implemented on `codex/local-validation-roadmap` (Linux pass 2026-10-01, HEAD `f8a06d8`, including V1.1-V1.5 and V-sync). Requested on 2026-09-30.
Physical-device, emulator/simulator, and macOS/Xcode gates remain explicitly unverified per the acceptance report.
This is the next delivery scope after the existing M1–M5 work. It expands
the earlier template-room and phone-only scope; it does not claim those
features or device checks are complete.

## Outcome

Deliver a professional Kotlin Multiplatform app using Google Material 3,
with the entire Capture → Seal → Share journey available on the device.
Create Property, Tenancy, Inspection, manually named Room, Evidence and
Finding; review, seal, render and verify a real Report without an API,
Docker, internet, account, payment, online map or remote image download.
GPS availability remains subject to device hardware and permissions.

See [local architecture](../03-architecture/local_validation_architecture.md)
and [acceptance gates](local_validation_acceptance.md). This roadmap is
the planning authority for this requested scope. Before implementation,
L0 must reconcile the existing normative documents and AGENTS.md rules.
Existing server guarantees must retain their meaning.

## Observed starting point

- `composeApp` already imports Material 3; `Theme.kt` defines a basic palette.
- `AppContainer` and the screens call `VaultApi` directly. Home creates
  remote records and automatically runs `ensureTemplate`.
- `CaptureStore` persists a JSON upload queue, not the complete Inspection.
  Corrupt JSON becomes an empty list; file names depend on photo counts.
- Android has CameraX capture. iOS injects `UnavailableCapture`, displays
  camera text and has Swift sources but no assembled Xcode project.
- The upload path does not carry capture-time or location claims.
- Seal and PDF require API calls. PDF is saved for sharing only after upload.
- Both PDF renderers lack proper continuation pages and can omit images;
  the Swift bridge also needs a real compilation pass.
- Screens use fixed padding without a shared safe-area policy. Android
  targets SDK 36. The existing coverage rule counts photos, not shot kinds.
- `make verify` runs Go vet/tests only. No mobile completion is established
  by that command. These findings are source review, not device test results.

## L0 — Contracts and reproducible builds

Deliver: document local authority, device-claim labeling, terminal SEALED,
manual Room input and offline sharing. Update product flow, domain fields,
evidence protocol, mobile layout, PDF contract and AGENTS.md together.
Keep the existing API path as a separate optional integration.
Pin compatible Kotlin, Compose, Android and Xcode versions after a build
audit; do not copy version promises from the old roadmap unchecked.
Assemble the iOS host and Kotlin framework export on macOS/Xcode.

Slices: scope/contract alignment; Android/shared build baseline;
iOS host/framework assembly; unified verification entry points.
Gate: reproducible Android build and runnable iOS host; mobile checks
are explicit, including macOS checks unavailable on Linux.

## L1 — Durable local application core

Deliver: shared feature repositories/use cases backed by transactional
SQLite, with Room KMP as the first compatibility candidate. Persist all
domain records, resume navigation and recover unfinished operations.
Keep immutable originals in durable app storage, with unique IDs,
atomic writes, explicit corruption handling and reconciled file/DB state.
Migrate existing queue records without discarding photos.
Inject local repositories into screens; disable network workers in local
mode. Storage completion and server confirmation are separate indicators.

Slices: persistence schema/migrations; repositories and use cases;
crash recovery; local container and worker isolation.
Gate: force-stop/relaunch preserves the Inspection and Evidence; the
whole data flow starts with the API stopped and networking blocked.

## L2 — Material 3 design system and adaptive shell

Deliver: a restrained professional identity using Material color roles,
typography, shapes, icons, surfaces and motion. Keep the current green
brand cue, with light/dark themes, pt-BR UI and accessible contrast.
Create shared Scaffold, app bars, forms, sheets, progress and error
components; move navigation and loading state out of screen-local memory.
Apply system/gesture/keyboard insets once at the correct boundary.
Camera backgrounds may extend edge-to-edge; controls stay in safe areas.
Support compact, medium and expanded windows, portrait/landscape,
tablet, foldable and multi-window layouts; verify iOS host interop.

Slices: tokens/components; navigation/safe-area shell; adaptive screens.
Gate: no hidden controls or doubled padding across the device matrix;
TalkBack/VoiceOver, large text and keyboard flows remain usable.

## L3 — Manually named rooms and places

Deliver: users type a Room label such as “Suíte principal” or “Garagem”.
Remove automatic templates from the primary flow. IDs and internal keys
are generated independently of the label; preserve Unicode and accents.
Allow creation, reordering and renaming in DRAFT; require an explicit
decision before deleting a Room with Evidence. Freeze labels when sealed.
Keep Finding presets: free Room names do not replace issue classification.
Use the existing guided shot categories consistently, including ceiling;
calculate coverage from distinct applicable categories, not photo count.

Slices: Room CRUD; user flow; checklist/coverage consistency.
Gate: arbitrary names work offline and appear unchanged in review/PDF;
repeated overview photos cannot produce false checklist completion.

## L4 — Real capture and per-photo geolocation

Deliver: lifecycle-safe CameraX and AVFoundation capture/preview,
orientation handling and explicit camera/permission errors.
Preserve the exact captured original. Produce thumbnail/preview copies
without changing it; bind metadata to each Evidence at capture time.
Use foreground Android location APIs and iOS Core Location behind a
shared interface. Store coordinates, accuracy, fix age, permission and
availability; distinguish precise, approximate, denied and unavailable.
Do not delay or lose a photo waiting for GPS. Missing/stale coordinates
must stay explicit; never substitute another photo's location or `(0,0)`.
Extract EXIF as a separate device claim and reconcile discrepancies.

Slices: Android capture reliability; iOS camera; location adapters;
Evidence metadata/derivatives and permission UX.
Gate: real captures on both platforms survive relaunch; GPS denial,
approximation, no fix and airplane mode work without fabricated data.

## L5 — Review and immutable local seal

Deliver: review includes all local Evidence, Findings, Room names,
checklist gaps and location status. Show incomplete coverage honestly.
Validate readable originals and complete hashing before finalization;
atomically freeze a versioned Manifest and set Inspection to SEALED.
Bind original hashes and all reported metadata, including Findings and
Room labels, to the signed snapshot. Use a protected installation key.
Keep device time labeled as such and server fields absent in local mode.
Changes after seal use Occurrence/Addendum, never reopen the Inspection.

Slices: review eligibility; canonical Manifest/signing; seal recovery.
Gate: alteration invalidates verification; double-tap/crash cannot create
different sealed snapshots; rendering failures keep SEALED terminal.

## L6 — Enterprise PDF generated entirely on-device

Deliver: cover, Property/Inspection identification, Room summary,
paginated photos with stable Evidence references, Findings, coordinates
and accuracy, methodology, completeness and verification appendix.
Use readable typography, consistent margins, wrapping, page numbers,
headers/footers and full digests in the appendix. Preserve image aspect
ratios and orientation; every sealed Evidence must be represented.
Use a shared layout contract with Android PdfDocument and native iOS
rendering. Stream bounded image decoding on a background dispatcher.
Show progress/cancellation and recover rendering jobs after interruption.
Save the final bytes locally before sharing; hash them once finalized.
Sign a detached receipt binding Report, Manifest and exact PDF hash.

Slices: complete pagination; layout/branding; platform parity;
render recovery and local preview/share with receipt.
Gate: 100-photo Report contains all photos and Findings, opens in
independent readers and can be generated/shared with the API stopped.

## L7 — Offline verification and issuer provenance

Deliver: in-app import/verification of the PDF plus signed receipt;
an optional evidence package adds Manifest, signature and originals.
The QR identifies the Report/Manifest and opens local verification.
Scanning it alone never certifies the PDF bytes or the original photos.
Show PDF integrity, Manifest integrity, Evidence integrity, signature
validity and issuer trust separately; distinguish missing from modified.
Create an independent offline verifier using the same published format.
Implement key fingerprint comparison and optional publisher-authorized
installation certificates, following the architecture's trust rules.

Slices: package format/export; app verifier/QR import;
independent verifier; issuer trust and key-lifecycle checks.
Gate: accept valid artifacts and reject tampering, substituted keys and
missing required files; an unknown signer cannot become a trusted issuer.

## L8 — Functional proof, resilience and release validation

Deliver: execute the acceptance matrix on physical Android/iOS devices,
emulators/simulators and supported window sizes. Record actual results,
build revision, environment, screenshots, sample PDF/package and verifier
output. Add meaningful automated regression tests for data loss,
pagination, canonical signing, tampering and network independence.
Measure existing performance budgets; fix critical defects before beta.

Slices: automated journeys; physical-device coverage;
performance/failure recovery; reproducible release evidence.
Gate: the complete local journey passes, with no unresolved data-loss,
missing-Evidence, false-trust or safe-area defects. Untested environments
remain explicitly unverified; “all devices” is not a test result.

## Execution order and completion

L0 → L1 → L2 → L3 → L4 → L5 → L6 → L7 → L8.
Use small PRs per slice, English descriptions, relevant doc updates,
`make verify`, required mobile checks, commit and push after each slice.
No completion percentage or delivery date is inferred from source alone.
Accounts, payments, remote trust timestamps, server sync and public web
verification are later extensions, outside the required local journey.
