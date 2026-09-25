# iOS host app (M1)

Swift sources embedding the shared Compose UI. Full Xcode project
assembly lands in M2 with a macOS runner; M1 validates the shared
Kotlin side for iOS targets (`compileKotlinIosSimulatorArm64`).

## Files

- `iOSApp.swift` — app entry.
- `ContentView.swift` — `MainViewController` bridge from `:composeApp`.
