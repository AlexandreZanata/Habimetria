# KMP App — Rental Evidence Vault (M1 scaffold)

Compose Multiplatform + shared logic. Photo-first, offline queue (M2).

## Modules

```text
app/
  shared/      # pure Kotlin: models, VaultApi (Ktor), queue, coverage
  composeApp/  # CMP UI (Home, Room) + Android app + iOS entry
  iosApp/      # Swift host (project assembly in M2, macOS runner)
```

## Local verification (Linux)

```bash
./gradlew :shared:jvmTest                        # common tests on JVM
./gradlew :shared:compileKotlinIosSimulatorArm64 # shared Kotlin for iOS
./gradlew :composeApp:assembleDebug              # Android APK (~12.7 MB debug)
```

iOS linking needs Xcode (macOS). CI validates shared-for-iOS + Android.

## M1 budgets

AAB download < 15 MB · debug APK 12.7 MB · cold start measured in M5.

See `docs/06-roadmap/mobile-phases.md` and `docs/05-mobile/`.
