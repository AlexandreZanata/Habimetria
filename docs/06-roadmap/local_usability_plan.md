# Local Usability — Execution Plan and Task Backlog

Status: ready for implementation; every task below is unstarted.
Source inspection: `ad42d25`, 2026-10-01. This is planning, not an app release.

Contracts: [navigation UX](../02-product/local_navigation_ux.md),
[Property removal](../03-architecture/local_property_removal.md),
[acceptance gates](local_usability_acceptance.md).
Continue the [local journey](local_validation_plan.md) and
[beta runbook](beta_readiness.md); these tasks do not replace those gates.

## Observed baseline

- `VaultShell` already has a title/Back header, but Home passes an empty
  title. No global navigation bar or dedicated Property/Report routes exist.
- `VaultNavigator.navigate` pushes routes. Previous fixes restore the Back
  trail but do not supply direct destination switching.
- `HomeScreen` combines address creation, Property cards and draft resume;
  its list-loading catch currently converts failures into an empty list.
- Property menus support new Inspection and rename, with no removal.
  `PropertyDao` supports insert/update/get/list; local schema is v13.
- Light theme `Paper` (`#F7F4EC`) and `PaperSoft` (`#ECE7DA`) are beige;
  shared constants are also used by dark roles.
- Navigation, SQLite reopen/migration and offline journey tests exist.
  Current source files over the size limit must be split while touched.

## Delivery sequence

Deliver U01 first for the requested color correction. U02–U06 remove the
repeated-Back problem; U07–U09 add safe Property removal; U10–U12 qualify
the complete result. IDs below are reusable work-item titles, not estimates
of calendar dates. Size is relative: S = focused change, M = feature slice,
L = device/performance validation. Accessibility applies to every slice.

### U01 — Replace beige light tokens with white and green

Priority P0; size S; dependencies: none.
Scope: `Theme.kt`, `design-system.md`; isolate light/dark constants and
set every relevant light surface/container role explicitly.
Accept: white Home/header/dialog backgrounds, green primary/selection,
neutral supporting surfaces and no beige light overlays. Dark snapshots
retain their baseline. Validate contrast and inspect both themes.

### U02 — Implement global navigation and restoration policy

Priority P0; size M; dependencies: none.
Scope: shared `vault.nav`, `NavigationStore`, `NavigatorTest` and App wiring.
Add typed destination/detail routes, root selection without duplicate
stack growth, ID-based resume and status/existence checks after restart.
Accept: 50 tab switches leave bounded stacks; starting another Inspection
never restores another Property's detail; SEALED never opens edit routes.
Test direct entry, restart, device Back parity and removed/archived targets.

### U03 — Add shared contextual header and adaptive navigation

Priority P0; size M; dependencies: U01, U02.
Scope: `VaultShell`, small shared header/bar components and `Strings`.
Compact bar/medium-expanded rail have four labeled destinations. Header
identifies page/Property/Room and provides one hierarchical Back control.
Accept: one tap reaches each global destination; Insets are consumed once,
all targets are at least 48 dp and selected state is accessible.

### U04 — Separate Home and Property management screens

Priority P0; size M; dependencies: U02, U03.
Scope: split `HomeScreen` into feature files, add Property list/detail and
existing create/rename form components; document them in `app/README.md`.
Use aggregated/bounded local queries rather than per-card repository loops.
Accept: Home prioritizes new/resume; `Imóveis` owns management and history;
loading/error/empty states differ, retry works and filters/scroll restore.

### U05 — Add Inspection and Report destinations

Priority P0; size M; dependencies: U02, U03, U04.
Scope: small local list screens/queries, read-only review and PDF entry.
Expose all local DRAFT/SEALED Inspections and finalized Reports; pending
PDF generation is clearly distinguished from an existing Report.
Accept: resume/review/open/share/verify work offline; no route bypasses seal
eligibility, displays fabricated success or mutates finalized artifacts.

### U06 — Preserve work while navigating the capture journey

Priority P0; size M; dependencies: U03, U05.
Scope: capture, Finding input, review/seal/PDF screens and session state.
Add `Cômodos`/`Revisão` shortcuts; replace duplicated Back controls;
preserve input/progress and prevent late async results from changing tabs.
Accept: tab switching and restart preserve committed photos/notes; busy
writes cannot double-submit; long PDF work is recoverable and navigable.

### U07 — Implement Property visibility and removal use cases

Priority P0; size M; dependencies: none.
Scope: shared local repository/DB; explicit migration/schema export,
eligibility queries and transactional delete/archive/restore.
Accept: only empty unlinked Properties delete; populated Properties archive;
legacy data survives and signed payloads remain identical.
Test reopen, races, write failure, idempotence and optional mirror links.

### U08 — Add removal confirmation and durable UI feedback

Priority P0; size S; dependencies: U04, U07.
Scope: Property action menu, dialogs, `Strings`, list refresh/error states.
Accept: distinct `Excluir`/`Arquivar` wording explains impact; Cancel is
safe; success follows commit; failure retains data/context and allows retry.
Test focus return and double activation with assistive technology.

### U09 — Add archived history, restore and stale-route recovery

Priority P0; size M; dependencies: U02, U05, U08.
Scope: Property/Inspection/Report filters, restore callback and startup.
Accept: archive removes active picker entries but keeps history accessible;
restore retains IDs; new Inspection creation requires restoration;
unrelated progress is unchanged and stale restoration falls back safely.

### U10 — Qualify accessibility and adaptive layouts

Priority P0 release gate; size M; dependencies: U01–U09.
Scope: all changed UI, TalkBack/VoiceOver, font scale, keyboard/focus,
gesture/three-button navigation, compact/medium/expanded windows.
Accept: all [accessibility scenarios](local_usability_acceptance.md) pass
on the stated devices; retain logs and screenshots, fix blocking defects.

### U11 — Measure and correct local performance regressions

Priority P0 release gate; size M; dependencies: U04–U09.
Scope: query batching/paging, lazy lists, thumbnails, background I/O and
device traces against [existing budgets](../05-mobile/app-performance.md).
Accept: targets pass on measured release builds; tab switching never
decodes originals or reopens the DB, and metrics include workload/device.
No new analytics SDK, network requirement or unmeasured speed claim.

### U12 — Run full offline acceptance and publish evidence

Priority P0 release gate; size L; dependencies: U10, U11.
Scope: regression suite, physical Android/iOS pass and focused usability
sessions with five first-time users, using synthetic Property data.
Accept: [acceptance gates](local_usability_acceptance.md) pass with actual
artifacts; unresolved data loss, false trust or inaccessible action blocks
release. List untested environments explicitly and update beta readiness.

## Execution discipline

Each task is a small PR, with English description and the repository's
commit convention; split M tasks further if the diff mixes concepts.
Read normative docs first, keep files about 200 lines or fewer, reference
new files from README/parent docs, and update contracts with behavior.
Run `make verify` on every slice; mobile implementation also requires
`make verify-mobile`, with macOS/Xcode and physical-device gates recorded
separately. Commit and push each completed slice. A merged task is not
evidence that the final device/usability/performance gates passed.
