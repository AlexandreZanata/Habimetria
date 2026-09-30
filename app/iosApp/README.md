# iOS host app (L0 status)

Swift sources embedding the shared Compose UI. There is no assembled
`.xcodeproj` in this repo yet; Linux CI checks only the shared Kotlin
side for iOS targets (`compileKotlinIosSimulatorArm64`).

## Files

- `iOSApp.swift` — app entry.
- `ContentView.swift` — `MainViewController` bridge from `:composeApp`.
- `SwiftPdfRenderer.swift` — `PlatformPdf` in Swift (PDFKit render,
  CoreImage QR, share sheet). Same pages as Android. Full pagination
  and local-receipt parity land in L6.

## macOS assembly (required, not verified on Linux)

1. `./gradlew :shared:linkDebugFrameworkIosSimulatorArm64`
   (plus `IosArm64` for device) to export the Kotlin framework.
2. Create/open the Xcode project hosting `iosApp/` sources,
   link the exported `shared.framework`, set the iOS minimum
   from `docs/05-mobile/toolchain.md` once pinned there.
3. `xcodebuild -scheme iosApp -sdk iphonesimulator build`
   then run simulator/device tests. Record Xcode version,
   iOS version, device and output in the L8 acceptance report.
4. Add `Info.plist` usage keys (required for capture/location):
   `NSCameraUsageDescription` and `NSLocationWhenInUseUsageDescription`.
   Without them the OS denies silently at runtime.
4. Swift does not compile on Linux; `xcodebuild`/`swift` absent
   here is expected, not a failure.
