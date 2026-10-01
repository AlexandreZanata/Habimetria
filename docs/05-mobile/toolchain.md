# Toolchain — Pinned After L0 Build Audit

Audit date: 2026-09-30. Host: Linux, JDK 21 Temurin, Gradle 8.14.3.
Android SDK platforms 34/35/36/36.1 present; build-tools up to 37.0.0.

## Pinned (source: `app/gradle/libs.versions.toml`)

- Kotlin `2.3.10`, Compose Multiplatform `1.11.1`, AGP `8.13.2`.
- `compileSdk 36`, `targetSdk 36`, `minSdk 26`, Java 17.
- CameraX `1.6.0`, Ktor `3.0.3`, coroutines `1.10.2`,
  serialization `1.8.1`, okio `3.9.0`, work `2.9.0`, lifecycle `2.8.7`.
- Room `2.8.5`, sqlite-bundled `2.6.2`, KSP `2.3.10` (L1 local core).

## Verified on Linux

- `:shared:jvmTest` — BUILD SUCCESSFUL.
- `:shared:compileKotlinIosSimulatorArm64` — BUILD SUCCESSFUL
  (Kotlin side only; linking needs macOS/Xcode).
- `:composeApp:assembleDebug` — BUILD SUCCESSFUL,
  debug APK ~18 MB (not a release-size claim).

## Not verified on Linux

- Xcode/Swift toolchain: absent (`xcodebuild`, `swift` missing).
  iOS host assembly, Swift compilation, framework export and
  simulator/device tests require macOS. See `app/iosApp/README.md`.
- Release R8/AAB size, Baseline Profiles and cold-start numbers
  are not re-established by this audit. Previous M5 numbers stay
  historical until re-measured on device.

## Policy

- Do not copy version promises from older roadmaps unchecked.
- Bump pins only with a fresh audit entry here plus green checks.
