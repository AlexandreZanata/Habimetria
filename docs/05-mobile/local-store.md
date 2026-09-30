# Local Store — SQLite Schema and Files (L1/L4/L5)

Normative for the on-device core. Code: `vault.local.db` in `:shared`.

## Database

- Room KMP, `VaultDatabase`, schema v3, entities:
  `properties → tenancies → inspections → rooms → evidences → findings`
  plus `manifests` (unique per inspection).
- Schema JSON exports to `app/shared/schemas/` and is committed.
  Version bumps require explicit `Migration` SQL; destructive
  migration is forbidden (photos must survive upgrades).
- v1 capture core → v2 location/EXIF columns → v3 manifests.
  Report rows land with L6.
- Device time and GPS stay device claims. No `received_at_server`,
  no certified `sealed_at` columns exist locally.

## Seal (L5)

- Eligibility shows rooms/evidence/findings, per-room coverage gaps
  and unreadable originals. Gaps display honestly without blocking;
  empty inspections and unreadable originals block with reason codes.
- `LocalSealer` validates originals (hash recompute), freezes the
  canonical Manifest, signs with the installation key and commits
  manifest + terminal SEALED in one `@Transaction`. Retries and
  double taps return the same snapshot (identity = manifest hash).
- Signatures: ECDSA P-256/SHA-256, raw 64-byte R||S wire format,
  uncompressed 65-byte public keys, keyId = SHA-256 hex. Keystore /
  Secure Enclave when present, labeled software keys otherwise.
- `sealedAtDevice` is a device claim. Post-seal changes use
  Occurrence/Addendum (later phase), never reopening.

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

## Capture claims and derivatives (L4)

- Schema v2 adds shutter-time details to `evidences`: `fixAgeMs`,
  `provider`, `accuracyLevel`, `isMocked`, plus a separate EXIF claim
  (`exifLatitude`, `exifLongitude`). Migration v1→v2 is pure ADD COLUMN;
  `MigrationTest` replays a real v1 file through it.
- `LocationTracker` (shared interface; LocationManager on Android,
  Core Location on iOS) reads only the cached fix — capture never waits
  for GPS. Statuses: precise, approximate, denied, unavailable.
  Fixes older than 30 s stay labeled stale; absent stays absent.
- EXIF GPS is parsed from JPEG bytes in shared code (`ExifClaim`) and
  never overwrites the fix; gaps over 100 m surface in review.
- Derivatives (`derivatives/<id>_thumb.jpg`, `<id>_preview.jpg`) are
  produced at capture from the original, which is never recompressed
  or rotated by the process. Grids decode thumbs with original fallback.

## Verification

- `LocalDatabaseTest` reopens a file-backed DB (force-stop analogue)
  and asserts Inspection, Room, Evidence and Finding survival.
- `make verify-mobile` runs it on JVM; Android/iOS compile the same
  sources via KSP per target.
