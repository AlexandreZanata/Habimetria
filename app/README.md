# KMP App — Rental Evidence Vault (M1 scaffold)

Compose Multiplatform + shared logic. Photo-first, offline queue (M2).

## Modules

```text
app/
  shared/      # pure Kotlin: models, VaultApi (Ktor), queue, coverage
  composeApp/  # CMP UI (Home, Room) + Android app + iOS entry
  iosApp/      # Swift host (project assembly in M2, macOS runner)
```

UI chrome lives in `composeApp/.../vault/ui/`:
`VaultShell` (single inset boundary + header),
`VaultChrome` (destination bar/rail, header subtitle, shell wiring).
`HomeScreen` prioritizes new/resume; property management lives in
`vault/ui/property/`: `PropertyListScreen` (search, aggregated
overviews, detail entry), `PropertyDetailScreen` (bounded inspection
history with DRAFT/SEALED filter), `PropertyForm` (address form,
creation block, rename dialog), `PropertyCards` (hero + cards),
`HomePropertyStates` (shared loading/error/empty), `PropertyActions`
(creation helpers), `PropertyStrings` (feature copy).
Journey destinations read bulk overviews from `LocalOverviewRepository`
(`shared/.../vault/local/LocalOverviews.kt`, queries in `ListingDaos`):
`vault/ui/inspection/` (`InspectionListScreen` with Em andamento/Selada
filters; DRAFT resumes, SEALED opens read-only review) and
`vault/ui/report/` (`ReportListScreen` with finalized READY reports
plus explicit pending "Gerar PDF" entries; DRAFT never offers
generation, pending never claims an existing report).
The capture journey (`LocalRoomsScreen`, `LocalRoomScreen`,
`LocalReviewScreen`, `LocalSealScreen`, `LocalPdfScreen`) shares
`vault/ui/journey/`: `JourneyShortcuts` (Cômodos/Revisão entries),
room rows/dialogs, capture controls, photo viewer and finding input.
Journey screens keep one header Back control (no bottom Voltar),
saveable input (category, finding note/preset) and busy-guarded
writes; committed photos/notes live in durable storage.
Property removal use cases live in `shared/.../vault/local/PropertyRemoval.kt`
(`PropertyRemovalRepository` via `LocalSession.propertyRemoval`, transactional
queries in `vault/local/db/PropertyRemovalDao.kt`): empty unlinked Properties
delete, populated ones archive/restore with sealed hashes unchanged.
The Imóveis list confirms through `PropertyRemovalHost` (fresh eligibility
wording in `PropertyRemovalDialog`, busy-guarded commit, live-region
feedback) with labeled menus in `PropertyCards`; navigation refs revalidate
in `PropertyActions`. Archived history filters through `PropertyVisibility`
(Ativos/Arquivados, badge, restore keeping stable IDs); Home hides
archived, creation requires restore, Vistorias/Relatórios label archived
rows via overview flags, and startup revalidates stale trails through
shared `vault.nav.validatedTrail`.

## Accessibility (U10)

Every action is a labeled button with no time limit: `ScreenTitle` announces
as a heading, errors/success/loading announce via polite live regions
(`ErrorText`, `RemovalFeedback`, `ProgressStatus`), decorative glyphs stay
hidden from assistive tech, and touch targets keep the Material 3 48dp
minimum (no overrides). No operation auto-cancels on inactivity; the only
delays are background upload backoffs (server sync, off in LOCAL_ONLY).
Light and dark schemes are independent theme roles with no hardcoded
screen colors; status always pairs text/shape with color. Device
TalkBack/switch/keyboard pass stays gated on hardware (U10 accept).

## Performance budgets (U11)

Release cold start ≤ 2 s · evidence write ≤ 500 ms/photo · PDF export ≤ 30 s,
all gated on the reference device + release build. Linux runs relative
measurements only, never absolute budgets. No fixture data is committed.

- `shared/.../vault/local/PerformanceFixture.kt` builds the deterministic
  200-inspection rows-only fixture in temp dirs (photo bytes excluded;
  the byte path is measured separately on the identical code path).
- `shared/.../vault/local/BulkPerformanceTest.kt` proves the bulk paths
  (200 overviews labeled, reused coverage check over 75 inspections at 100,
  3 MB evidence write within 500 ms) and prints timings for run-over-run
  comparison; `-Dvault.fixture.dir` adds a JSON summary for the verifier.
- `baselineprofile/.../VaultStartupBenchmark.kt` measures release cold
  start on device (median `timeToInitialDisplayMs` from the report):
  `./gradlew :composeApp:assembleRelease` then
  `:baselineprofile:connectedAndroidTest` with the benchmark class filter.
  The baselineprofile module applies the Kotlin Android plugin so these
  sources actually compile (previously unwired).

## Local verification (Linux)

```bash
./gradlew :shared:jvmTest                        # common tests on JVM
./gradlew :shared:compileKotlinIosSimulatorArm64 # shared Kotlin for iOS
./gradlew :composeApp:assembleDebug              # Android APK
```

iOS linking needs Xcode (macOS). CI validates shared-for-iOS + Android.
Pinned versions and L0 audit: `docs/05-mobile/toolchain.md`.

## M1 budgets

AAB download < 15 MB · debug APK 12.7 MB · cold start measured in M5.

See `docs/06-roadmap/mobile-phases.md` and `docs/05-mobile/`.
