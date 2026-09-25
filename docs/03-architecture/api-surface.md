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
- `GET /inspections/{id}/evidences/download-url?evidence_id=` — signed 5min URL (S3)
- `POST /rooms/{id}/findings` (S4) — { evidence_id, preset, note? }
- `GET /rooms/{id}/findings` (S4) — list
- `GET /inspections/{id}/coverage` (S4) — { total_percent, rooms[] }

Preset enum: `stain | scratch | crack | broken | moisture | hole | other`.

## Seal, report, share, verify

- `POST /inspections/{id}/seal` (S5) — payment stub, returns { manifest_sha256, signature, report_id, verify_code }; 404 missing, 409 sealed/changed, 422 empty
- `GET /reports/{id}.pdf` — signed URL to PDF
- `POST /reports/{id}/shares` — { channel: whatsapp|email|link }
- `GET /v/{code}` — public, no auth: { authentic, sealed_at, evidence_count, report_sha256 }

## Occurrences (V1.1, schema-ready)

- `POST /properties/{id}/occurrences` — { title, body? } + evidence upload reuse.
