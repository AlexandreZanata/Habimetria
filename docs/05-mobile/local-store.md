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

## Verification

- `LocalDatabaseTest` reopens a file-backed DB (force-stop analogue)
  and asserts Inspection, Room, Evidence and Finding survival.
- `make verify-mobile` runs it on JVM; Android/iOS compile the same
  sources via KSP per target.
