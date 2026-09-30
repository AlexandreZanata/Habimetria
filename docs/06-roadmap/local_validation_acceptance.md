# Local Validation — Acceptance and Functional Proof

Execution gates for [the roadmap](local_validation_plan.md) and
[local architecture](../03-architecture/local_validation_architecture.md).
Status: planned scenarios; none are asserted passed by this document.

## Reproducible complete journey

Run the following on physical Android and iOS devices:

1. Install/start with the API stopped and all network access blocked.
2. Create Property, Tenancy and Inspection locally without signup/payment.
3. Type multiple Room names, including accents, spaces and long labels.
4. Capture several real photos per Room and add Findings with notes.
5. Review coordinates/accuracy where available, and explicit missing GPS
   elsewhere. Turn permissions off and continue successfully.
6. Force-stop and restart; resume the same Inspection without lost records.
7. Review coverage and every Evidence, then seal the immutable snapshot.
8. Generate the complete PDF locally with progress. Reopen it after restart.
9. Open/share through native system UI; cancellation preserves the Report.
   Local export requires no server; recipient transport may require network.
10. Export/import PDF plus receipt and the full evidence package; verify
    independently offline. Record actual issuer trust separately.
11. Attempt edits to SEALED content; create Occurrence/Addendum instead.

Airplane mode is a functional scenario, not proof of zero network requests.
Instrument/block HTTP and verify that API, map, font, image and verification
downloads are unnecessary. OS-level camera/location activity is separate
from app API traffic. Test fresh-install and resumed-session journeys.

## Persistence and failure recovery

- Kill during capture write, DB commit, hashing, seal, PDF render and export.
- Repeat/reenter capture rapidly; no filename collisions or duplicates.
- Exhaust disk space; retain committed originals and show actionable errors.
- Corrupt a DB/queue or remove a referenced file; never silently show empty
  data or a success badge. Reconcile recoverable partial writes explicitly.
- Migrate an existing queued-photo installation and a prior local schema.
- Retry seal after interruption; obtain the same snapshot, not a new one.
- Lose the signing key; still verify old reports with public material;
  new issuance clearly uses a different identity.
- Cancel PDF generation; retry without returning SEALED to DRAFT.

## Camera and location matrix

- Camera granted, denied, permanently denied and interrupted by backgrounding.
- Portrait/landscape images, EXIF rotations, front/rear camera if supported.
- Precise and approximate location; services disabled; no fresh fix.
- Airplane mode outdoors/indoors; stale fix; permission changed mid-session.
- Simulator/mock-location flags when available; real-device GPS accuracy.
- Imported media, if supported, has a different origin label from capture.
- Original bytes/hash unchanged after thumbnail and PDF creation.
- Locations belong to the correct Evidence; EXIF disagreement stays visible.
- Missing/approximate location never becomes “verified precise location”.

## Material design, safe areas and accessibility

Support classes, rather than claiming to test every device ever manufactured:

- Android API 26 baseline, target API 36 and the current supported stable
  release established in L0; gesture and three-button navigation.
- Small/large phones, cutouts, tablet, foldable, split-screen and resizing.
- iOS minimum established by the pinned stack, plus current supported iOS;
  iPhone without notch, with notch/Dynamic Island and iPad/multitasking.
- Portrait/landscape, light/dark, keyboard open/closed and large font scales.
- Sheets, camera controls, bottom actions, validation errors and share UI.
- TalkBack/VoiceOver focus order, labels, contrast and adequate touch targets.

Gate: no hidden/clipped action, unwanted double inset, unscrollable form,
inaccessible focused field or lost capture state after window changes.
Every future screen must reuse and validate the same inset policy.

## PDF completeness and visual quality

- Use a deterministic fixture containing 100 photos, several Rooms,
  long Findings, Unicode, varied image sizes/orientations and missing GPS.
- Count every expected Evidence reference and Finding in extracted PDF text;
  also inspect rendered pages for photos, clipping, overlap and readability.
- Check cover, summary, pagination, headers, footers, full hashes and QR.
- Open in independent Android/iOS/desktop PDF readers and print to A4.
- Compare logical contents across Android and iOS; bytes need not match.
- Remove an original before render; fail finalization or clearly identify
  an incomplete draft. Never silently omit it from a verified final Report.
- Verify QR at realistic print size; it must not encode a local server URL.

Performance targets retain the existing budgets pending real-device
measurement: cold start <1.5 s, shutter-to-preview <300 ms, 60 fps grids,
100-photo PDF <10 s and target <10 MB. Integrity/completeness take priority
over compression. Publish device, build type, workload and measured values;
do not claim a target as achieved until measured. Limit peak image memory.

## Integrity and issuer-trust adversarial cases

- Accept a valid snapshot, PDF receipt and complete evidence package.
- Change one byte of original/PDF, metadata, Room label, note or GPS value.
- Delete/reorder/duplicate Evidence or replace Manifest/signature/receipt.
- Substitute a self-generated key and re-sign; never retain trusted issuer.
- Scan the correct QR attached to a different PDF; PDF integrity must fail.
- Change device clock or simulate GPS; integrity can pass while device
  claims remain unverified. Never promote them to independently trusted facts.
- Test unknown/expired issuer certificates and stale offline trust data;
  device time alone cannot establish trustworthy certificate validity time.
- Test unsupported format versions, unsafe archive paths and oversized input.
- Verify shared fixtures in Kotlin and an independent offline implementation.

## Checks and proof artifacts

`make verify` currently covers Go only. L0 adds documented mobile gates:
shared JVM tests, Android lint/build, UI/device tests, iOS framework/Swift
build and simulator/device tests on macOS. Mobile checks must not be silently
skipped. Backend database integration also requires an explicit configured
PostgreSQL run; an unconfigured passing suite does not prove DB integration.
Run the applicable checks per slice, with full L8 acceptance before beta.

Record a reproducible acceptance report containing commit/build ID,
toolchain, OS/device, exact scenario, expected/observed result, test logs,
screenshots or recordings, sample PDF/package and independent verifier
output. Use synthetic fixtures for committed examples; exclude keys,
personal addresses and real-user location/photos from repository artifacts.
Distinguish automated, simulator and physical-device evidence. Release
requires no critical loss, omission, false-trust or safe-area defect;
anything not executed remains explicitly unverified.
