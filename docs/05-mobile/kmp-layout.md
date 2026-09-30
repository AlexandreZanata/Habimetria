# KMP Layout — Feature-first, Shared Logic

```text
app/
  composeApp/src/commonMain/kotlin/vault/
    capture/CaptureQueue.kt      # queue model
    capture/Uploader.kt          # background upload interface
    inspection/InspectionState.kt
    rooms/RoomChecklist.kt       # coverage %
    findings/FindingPreset.kt    # enum, no free text required
    share/ShareLink.kt
  composeApp/src/androidMain/    # camera, WorkManager
  composeApp/src/iosMain/        # camera, BGTask
  README.md
```

Rules: all business rules in `commonMain`. Platform code only for camera, storage, workers. UI: Compose Multiplatform, one question per screen.

## Local-first (L0 authority)

- Shared repositories/use cases own validation, coverage, snapshot,
  canonical serialization, verification and the PDF layout contract.
- UI never calls `VaultApi` directly. API repositories stay optional
  and explicitly configured; local mode disables network workers.
- Persistence is transactional SQLite plus durable private files.
  Storage completion and server confirmation are separate indicators.
- Checklist coverage uses distinct applicable categories
  (overview/walls/openings/floor/ceiling), never photo count.
