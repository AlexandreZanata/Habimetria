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
