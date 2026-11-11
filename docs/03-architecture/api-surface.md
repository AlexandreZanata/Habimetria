# API Surface — MVP REST

The base is `/api/v1`. The [web panel](../04-backend/web_panel.md) uses
same-origin sessions. Owned private API resources require the titleholder's
session. Legacy unclaimed resources are served only when the panel is
disabled. `/v/{code}` and explicit Share links retain public access.

This documents the current baseline, including the legacy payment seam.
The [commercial roadmap](../06-roadmap/commercial/README.md) replaces that
seam with monthly hosted authorization. Planned new routes are in
[subscription API](../04-backend/subscription_api.md); they do not exist yet.

## Ops (S1, implemented)

- `GET /healthz` — always 200: { status, database, uptime }
- `GET /readyz` — 200 when DB pings, else 503
- `GET /api/v1/ping` — pong

## Properties & tenancies

- `POST /properties` — { address, complement? }
- `POST /properties/{id}/tenancies` — { type: entry }
- `POST /tenancies/{id}/inspections` — creates DRAFT
- `POST /tenancies/{id}/inspections` with { kind: exit } (V1.2/S9) — requires a SEALED entry, one exit per tenancy; else 409

## Rooms & evidence

- `POST /inspections/{id}/rooms` — { key, label }
- `POST /inspections/{id}/rooms:ensure-template` — default 5 rooms, idempotent
- `GET /inspections/{id}/rooms` — ordered list
- `POST /inspections/{id}/evidences` (S3, multipart: file + type + room_id? + captured_at_device? + latitude? + longitude?) — 201 { evidence_id, sha256, received_at_server, sequence }
- `GET /inspections/{id}/evidences` — sealed set ordered by sequence (PDF source)
- `GET /inspections/{id}/evidences/download-url?evidence_id=` — signed 5min URL (S3)
- `POST /rooms/{id}/findings` (S4) — { evidence_id, preset, note? }
- `GET /rooms/{id}/findings` (S4) — list
- `GET /inspections/{id}/coverage` (S4) — { total_percent, rooms[] }

Preset enum: `stain | scratch | crack | broken | moisture | hole | other`.

## Seal, report, share, verify

- `POST /inspections/{id}/seal` (S5, S6-basic) — returns { manifest_sha256, signature, report_id, verify_code }; paywall off by default, live mode enforces Stripe/Asaas proof (402); 404 missing, 409 sealed/changed, 422 empty
- `GET /reports/{id}` (S7) — report + QR-ready verify_url
- `POST /reports/{id}/pdf` (S7) — two-pass final PDF attach, returns report_sha256
- `GET /reports/{id}/download-url` (S7) — signed 5min PDF URL
- `GET /v/{code}` (S7) — public, no auth: { authentic, sealed_at, evidence_count, manifest_sha256, signature_valid, report_sha256 }
- `POST /reports/{id}/shares` (S8) — { channel } → { share, url }; logs created
- `GET /reports/{id}/shares` (S8) — timeline with created/accessed/downloaded counts
- `GET /s/{token}` (S8) — public recipient view + fresh PDF URL; logs accessed
- `GET /s/{token}/pdf` (S8) — 302 to signed PDF; logs downloaded

## Occurrences (V1.1, S9)

- `POST /properties/{id}/occurrences` — { title, body? } → 201; 404 unknown property.
- `GET /properties/{id}/occurrences` — timeline, newest first; 404 unknown property.
- `POST /occurrences/{id}/evidences` — { evidence_id } → 201; evidence must belong to the occurrence's property (400), re-link is 409.
- `GET /occurrences/{id}/evidences` — photo proof, oldest received first; 404 unknown occurrence.
- Post-seal only by convention: occurrences never mutate inspections.

## Comparisons (V1.3, S9)

- `POST /tenancies/{id}/comparisons` — { entry_room_id, exit_room_id, shot_category, entry_evidence_id?, exit_evidence_id?, tag } → 200 upsert; retags overwrite, history stays on device.
- `GET /tenancies/{id}/comparisons` — tags, oldest first; 404 unknown tenancy.
- Closed vocabularies: tag `no_change|changed|wear`, category `overview|walls|openings|floor|ceiling`.
- Rooms must sit on the tenancy's entry/exit inspections (400); tags describe visual state only, never liability.
