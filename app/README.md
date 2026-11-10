# KMP App — Habimetria (M1 scaffold)

Compose Multiplatform + shared logic. Photo-first, offline queue (M2).

## Modules

```text
app/
  shared/      # pure Kotlin: models, VaultApi (Ktor), queue, coverage
  composeApp/  # CMP UI (Home, Room) + Android app + iOS entry
  iosApp/      # Swift host (project assembly in M2, macOS runner)
```

The shared architectural illustration is documented in
[Home artwork](../docs/05-mobile/home_artwork.md).

UI chrome lives in `composeApp/.../vault/ui/`:
`VaultShell` (single inset boundary + header),
`VaultChrome` (destination bar/rail and shell wiring).
`HomeScreen` prioritizes new/resume via the hero to `Route.NewInspection`;
property management lives in `vault/ui/property/`: `NewInspectionScreen`
(dedicated address form), `PropertyListScreen` (search, aggregated
overviews, detail entry), `PropertyDetailScreen` (bounded inspection
history with DRAFT/SEALED filter), `PropertyDataForm` (editable data
section), `PropertyForm` (address form, creation block),
`HomeHeroBanner` (brand signature + architectural elevation), `PropertyCards`
(photo-led Home cards and compact management rows),
`HomePropertyStates` (shared loading/error/empty), `PropertyActions`
(creation helpers), `PropertyStrings` (feature copy).
Journey destinations read bulk overviews from `LocalOverviewRepository`
(`shared/.../vault/local/LocalOverviews.kt`, queries in `ListingDaos`):
`vault/ui/inspection/` (`InspectionListScreen` with Em andamento/Selada
filters; `InspectionRows` options menu per state plus `InspectionDeleteHost`
confirmation; DRAFT resumes, SEALED opens read-only review) and
`vault/ui/report/` (`ReportListScreen` with finalized READY reports
plus explicit pending "Gerar PDF" entries; DRAFT never offers
generation, pending never claims an existing report; `ReportRows`
with red delete plus `ReportDeleteHost` confirmation).
`vault/ui/settings/` (`SettingsScreen`: device-local sections; reports
live here as a detail route under Ajustes; `FindingProblemsScreen`
toggles built-ins and manages customs, `SettingsStrings` owns the copy).
Finding display labels live in `shared/.../vault/findings/FindingLabels.kt`
(`label()` per `AppLocale`); stored codes never change.
Generated-report removal lives in `shared/.../vault/local/ReportGenerator.kt`
(`deleteReport` removes row, PDF and receipt; sealed data regenerates).
Inspection removal lives in `shared/.../vault/local/InspectionRemoval.kt`
(`InspectionRemovalRepository` via `LocalSession.inspectionRemoval`,
transactional deletes in `vault/local/db/InspectionRemovalDao.kt`): DRAFT
and SEALED delete with confirmation; comparisons block instead of
corrupting the other side; tenancies, properties and occurrences survive.
The capture journey (`LocalRoomsScreen`, `LocalRoomScreen`,
`LocalReviewScreen`, `LocalSealScreen`, `LocalPdfScreen`) shares
`vault/ui/journey/`: `JourneyShortcuts` (Cômodos/Revisão entries),
`RoomSetup` (labeled input, text-only empty state),
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

## Local PDF implementation

`shared/.../vault/local/PdfContract.kt` builds the Report model;
`PdfTypes.kt` defines drawing ports and geometry; `PdfPagination.kt` places
photos without stretching. `ExifOrientation.kt` defines EXIF transforms,
covered by `ExifOrientationTest.kt` and aspect checks in `PdfContractTest.kt`.
Native adapters use `AndroidImageDecode.kt` for bounded normalized pixels,
`IosScaler.kt` for orientation-aware derivatives even at small sizes, and
`JvmImageOrientation.kt` for the independent test codec. Original bytes stay
unchanged. `PdfDims.kt` returns oriented display dimensions per platform.

`ManifestSnapshot.kt` reads signed inputs and coverage without live-row reads.
`ReportSources.kt` hashes originals before decoding and bypasses preview caches.
`ReportArtifacts.kt` atomically publishes/verifies exact PDF/receipt bytes;
`SignerMaterial.kt` retains historical public keys without assigning trust.
The package responsibilities are `PackageFormat.kt` (identity/verdict),
`PackageEnvelope.kt` (wire payloads), `PackageExporter.kt` (verified staging)
and `PackageVerifier.kt` (independent checks). `PdfImageFixture.kt` supplies
synthetic JPEG bytes; `ReportSnapshotTest.kt` covers live edits, cached-image
substitution, same-size tampering, missing receipts and key rotation.

`PdfTextLayout.kt` measures and splits complete text using native font widths;
`PdfTextLayoutTest.kt` covers long notes, Unicode and actual page rectangles.
`ReportClaims.kt` prints device capture time, absent/stale/approximate location
and separate EXIF claims per photo. Android drawing is split across
`AndroidPlanRenderer.kt`, `AndroidPdfFonts.kt` and `AndroidPdfDrawing.kt`;
iOS drawing uses `IosPlanRenderer.kt`, `IosPdfText.kt` and `IosPdfImages.kt`.
Both render only measured positions and fail on decoding/QR errors.

- [Local feature repositories and database definitions](shared/src/commonMain/kotlin/vault/local/README.md).

Report review uses `ReportDetailsSheet.kt`; Property detail uses
`InspectionProfileDialog.kt` and `PropertyInspectionRow.kt` for profile
selection and history. `ReportProfilesTest.kt` checks frozen professional
metadata, direct Property ownership and version history.

`PackageZip.kt` and `PackageZipImport.kt` provide bounded offline archives;
`PackageZipTest.kt` checks portable verification, corruption and traversal.

`InspectionCompletionRoutes.kt` and `LocalReportRoutes.kt` split the route
composition. `LocalPdfScreen.kt` verifies READY bytes before sharing and
lets users select previous versions or export the portable package.

System document selection uses `LocalFilePicker.kt`, `AndroidFilePicker.kt`
and `IosFilePicker.kt`; `PackageVerdictBlock.kt` separates the verification
results from the import screen. `ResponsibilityDocument.kt` preserves the
required professional ART/RRT PDF and freezes its digest in Manifest v4.

`IosImageDecode.kt` provides bounded ImageIO thumbnails with all EXIF
transforms for both preview generation and native PDF drawing.

[JVM migration probes](shared/src/jvmTest/kotlin/vault/local/README.md)
verify preservation of populated v15 databases and historical signed bytes.

[Native Android PDF probes](composeApp/src/androidInstrumentedTest/kotlin/vault/pdf/README.md)
exercise eight orientations, long notes, rental and technical PDFs, and
portable package validation using isolated synthetic data.

`PhotoDisplay.kt` freezes encoded/display dimensions at seal time. Report
review offers standard or enlarged-photo presentation; `ResponsibilityDocumentTest.kt`
checks attachment immutability, metadata freezing and independent rejection.

[Native PDF artifact checks](tools/README.md) verify embedded synthetic
images, dimensions, complete notes and physical page/index references.

[Report authorship UI](composeApp/src/commonMain/kotlin/vault/ui/README.md)
provides optional identification, applicability and explicit Share declarations.
[Java certificate capability](shared/src/jvmAndAndroidMain/README.md) is shared
by Android/JVM; `shared/src/iosMain/kotlin/vault/local/PersonalCertificateSigner.kt`
reports this optional holder-signing capability unavailable on iOS.
Current delivery runs code tests/Android compilation only. User-owned manual
validation after all phases is documented in `docs/06-roadmap/report_authorship_execution.md`.
