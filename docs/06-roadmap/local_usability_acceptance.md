# Local Usability — Acceptance Gates

Status: planned; no scenario below has been executed for this feature.
Scope: [U01–U12](local_usability_plan.md), supplementing the
[existing local acceptance matrix](local_validation_acceptance.md).

## Navigation and comprehension

1. From capture, review, seal, Report, verify, Occurrence and comparison,
   reach each global destination with one tap; return to the same draft
   without lost notes, photos or Room progress.
2. Switch global destinations 50 times; no duplicate-root stack growth.
   Re-selecting a destination opens its root. Back follows the detail
   hierarchy without replaying the tab-switch sequence.
3. Open/resume directly, rotate/resize, force-stop/restart and change
   Property. Restore stable IDs and current states, never stale edit routes.
4. During capture commit, seal and PDF rendering, attempt tab changes and
   rapid taps. Preserve committed state; no duplicate writes or unexpected
   navigation after late completion. Native share/camera cancellation is safe.
5. List DRAFT/SEALED Inspections and finalized/pending Reports; errors are
   visible and retryable rather than falsely represented as an empty list.

## Property removal and integrity

1. Delete an empty unlinked Property; cancel first, then confirm. Relaunch:
   deletion persists and unrelated Properties/progress remain unchanged.
2. Add a Tenancy between dialog open and confirm: permanent delete is
   rejected by transactional eligibility, preserving all related data.
3. Archive a Property with DRAFT and SEALED Inspections, Findings, Report,
   Occurrence, comparison and optional mirror references. It leaves active
   pickers while remaining discoverable through archived history.
4. Restore after process death: original IDs, drafts, originals, finalized
   Reports and links remain identical. Resume an archived draft; require
   restoration before creating another Inspection.
5. Inject write failure, double taps and interruption after DB commit but
   before navigation refresh. No false success, dangling restored routes
   or file cleanup. Verify migration from v13 preserves all legacy rows.
6. Compare Manifest/Report/original hashes and independently verify a
   sealed package before/after archive/restore. SEALED remains terminal.

## Appearance, accessibility and windows

- Light mode: inspect every destination, header, bar/rail, card, dialog,
  sheet, focus/error state and keyboard transition. White/green is the
  default light palette; no beige/yellow surface overlays remain.
- Dark mode: baseline comparison confirms independent role definitions.
- Verify normal text at 4.5:1 and required non-text indicators at 3:1.
  Check selection by label/shape/semantics, not green alone.
- At default and 200% font scale, every action is readable/reachable,
  contextual titles retain usable Property identification and forms scroll.
- Check all targets at 48 dp or larger; label icons/menus, announce
  save/removal/error results and preserve sensible dialog focus return.
- Execute TalkBack and VoiceOver journeys; selected destinations are
  announced, headings/order are logical and there are no duplicate labels.
- Compact phone, medium/expanded tablet/foldable, portrait/landscape,
  split-screen, cutout, gesture/three-button navigation and keyboard open:
  no double insets, overlaps, hidden actions or lost input.

## Local performance and independence

Use synthetic fixtures with 100 Properties, 200 Inspections and one
100-photo Inspection. Record release-build results on two mid-range
devices, identifying OS, hardware, build and warm/cold conditions.

- Preserve existing budgets: cold start <1.5 s, shutter-to-preview <300 ms,
  capture-to-durable-queue <100 ms, 60 fps thumbnail grids and 100-photo
  PDF <10 s. Record APK/AAB size against the established 15 MB budget.
- New target: warm destination switch to usable cached content p95 <200 ms
  across at least 50 switches. Measure complete UI response, not click dispatch.
- Lists use bounded queries/lazy rendering and thumbnails; navigation
  never decodes full originals, computes hashes or runs filesystem I/O on
  the UI thread. Storage failures show actionable errors.
- Run with API stopped and app HTTP blocked: create/resume/capture/review/
  seal/PDF/share/verify, archive/restore and empty deletion all operate locally.
  Airplane mode alone does not establish zero app network requests.

## Usability and release evidence

Observe five first-time users with no navigation coaching: resume a draft,
switch Property, reach review, find/share a Report, and remove an empty
Property or archive one with history. Record per-task completion, taps,
time, help requests and confusion between deletion and archive.
Target at least four of five unaided completions for each task and zero
unintended deletion or misunderstanding that archive deletes photos.
This small qualitative gate identifies problems; it is not a population claim.

Attach commit/build, test logs, measured contrast/performance, screenshots,
screen-reader recordings and independent verification output to the release
report. `make verify` checks Go only; mobile code needs `make verify-mobile`
and applicable macOS/device checks from beta readiness. Unexecuted scenarios
stay unverified. No release with data loss, false confirmation, inaccessible
primary actions or unresolved critical offline regression.
