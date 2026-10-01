# API Surface — MVP REST

Base: `/api/v1`. Auth: magic-link JWT (Bearer). Public: `/v/{code}`.

## Ops (S1, implemented)

- `GET /healthz` — always 200: { status, database, uptime }
- `GET /readyz` — 200 when DB pings, else 503
- `GET /api/v1/ping` — pong

## Properties & tenancies

- `POST /properties` — { address, complement? }
- `POST /properties/{id}/tenancies` — { type: entry }
- `POST /tenancies/{id}/inspections` — creates DRAFT

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
- Post-seal only by convention: occurrences never mutate inspections.
