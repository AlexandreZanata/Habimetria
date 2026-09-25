# KMP App — Rental Evidence Vault

Compose Multiplatform, Android + iOS. Photo-first, offline queue.

## Modules (planned)

```text
composeApp/src/commonMain/kotlin/vault/
  capture/ inspection/ rooms/ findings/ share/
composeApp/src/androidMain/  # CameraX + WorkManager
composeApp/src/iosMain/      # AVFoundation + BGTask
```

Slices: S9 builds capture + queue + upload against `docs/05-mobile/offline-upload.md`.
See `docs/05-mobile/kmp-layout.md`.
