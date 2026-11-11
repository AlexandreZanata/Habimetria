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
    local/                       # offline core: vault, seal, PDF, verify
    local/db/MirrorLinks.kt      # server-mirror link entity + DAO
    sync/ServerMirror.kt         # optional API mirror (occurrences, tags, exit)
    sync/DbMirrorStore.kt        # durable mirror links, survives relaunch
    nav/                         # routes + navigator, no screen-local memory
  composeApp/src/androidMain/    # camera, WorkManager
  composeApp/src/iosMain/        # camera, BGTask
  README.md
```

Rules: all business rules in `commonMain`. Platform code only for camera, storage, workers. UI: Compose Multiplatform, one question per screen.
Screens reach the API only through `ServerMirror` via `AppContainer`; never call `VaultApi` directly.

Planned account, native store and hosted-service features use injected
use cases/adapters through AppContainer; see
[subscription experience](subscription_experience.md). All local use cases
remain free and independent of those adapters.

## Local-first (L0 authority)

- Shared repositories/use cases own validation, coverage, snapshot,
  canonical serialization, verification and the PDF layout contract.
- UI never calls `VaultApi` directly. API repositories stay optional
  and explicitly configured; local mode disables network workers.
- Persistence is transactional SQLite plus durable private files.
  Storage completion and server confirmation are separate indicators.
- Checklist coverage uses distinct applicable categories
  (overview/walls/openings/floor/ceiling), never photo count.
