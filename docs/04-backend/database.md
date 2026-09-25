# Database — PostgreSQL 18.6-alpine

Single source: `backend/migrations/0001_init.sql`. One version per file.

## Core tables

- `users(id, email CITEXT UNIQUE, created_at)`
- `properties(id, owner_user_id, address, created_at)`
- `tenancies(id, property_id, type, status, cap_photos, cap_videos, cap_bytes)`
- `inspections(id, tenancy_id, kind(entry|exit), status(DRAFT|SEALED), sealed_at, manifest_sha256, report_id)`
- `rooms(id, inspection_id, key, label, sort)`
- `evidences(id, inspection_id, room_id, type(photo|video|text|doc), mime, size, storage_key, sha256, captured_at_device, received_at_server, sequence)`
- `findings(id, room_id, evidence_id, preset, note)`
- `reports(id, inspection_id, verify_code UNIQUE, report_sha256, pdf_key, created_at)`
- `occurrences(id, property_id, title, body, created_at)` + `occurrence_evidences`
- `shares(id, report_id, channel, token UNIQUE, created_at)` + `access_logs(share_id, event, at, ip_hash)`

## Invariants (enforce in service + CHECK)

- `SEALED` requires `sealed_at NOT NULL`, `manifest_sha256 NOT NULL`.
- No update/delete on `evidences` when parent inspection `SEALED`.
- `sequence` unique per inspection. Manifest ordered by it.
- `verify_code`: `nanoid(10)`, unique index.
