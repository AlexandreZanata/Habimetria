# Local Store — SQLite Schema and Files (L1)

Normative for the on-device core. Code: `vault.local.db` in `:shared`.

## Database

- Room KMP, `VaultDatabase`, schema v1, entities:
  `properties → tenancies → inspections → rooms → evidences → findings`.
- Schema JSON exports to `app/shared/schemas/` and is committed.
  Version bumps require explicit `Migration` SQL; destructive
  migration is forbidden (photos must survive upgrades).
- v1 covers the capture core only. Manifest/Report tables arrive
  with L5 as migration v2.
- Device time and GPS stay device claims. No `received_at_server`,
  no certified `sealed_at` columns exist locally.

## Files

- Originals: `<filesDir>/vault/originals/<evidenceId>.bin`, written to
  `.tmp` then atomically renamed. Name never derives from photo counts.
- Database file: `<filesDir>/vault/vault.db` (Android `filesDir`,
  iOS `NSDocumentDirectory/vault`, JVM temp `vault`).
- Derivatives (`preview.webp`, `thumbnail.webp`) are recreatable;
  only originals plus DB rows are authoritative.

## Rules

- `DRAFT | SEALED` only; `SEALED` terminal. Writes to a sealed
  inspection abort except through Occurrence/Addendum (L5).
- Room labels preserve Unicode; `roomKey` is internal and stable.
  `naCategories` records explicitly non-applicable shot categories.
- Startup reconciles DB rows against original files; missing files
  and corrupt rows surface as errors, never silent empty lists.
- Existing `queue.json` records migrate into `evidences` without
  discarding photos (L1.3); the old queue file is then retired.

## Session and sync isolation

- `LocalSession` bundles database, repository, coverage, originals,
  navigation progress and the sync policy. Open via `openLocalSession`;
  `startup()` migrates any legacy queue then reconciles files vs rows.
- `SyncMode.LOCAL_ONLY` (default) disables every network worker through
  the single `uploadsEnabled` gate (`UploadWorker.scheduleIfAllowed`).
  `WITH_SERVER_SYNC` keeps the optional API integration.
- UI indicators stay split: `storedLocally` means durable on-device;
  `SyncIndicator (QUEUED | UPLOADING | CONFIRMED | FAILED)` means server
  sync only. `LOCAL_ONLY` never shows a server state.
- `NavigationStore` (`vault/progress.json`, atomic write) restores the
  last inspection/room after process death, outside domain tables.
- Per-screen `LocalSession` injection completes with the L2/L3 rewiring;
  `AppContainer` already carries `syncMode` and the optional session.

## Verification

- `LocalDatabaseTest` reopens a file-backed DB (force-stop analogue)
  and asserts Inspection, Room, Evidence and Finding survival.
- `make verify-mobile` runs it on JVM; Android/iOS compile the same
  sources via KSP per target.
