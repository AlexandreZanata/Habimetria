# Evidence Protocol — Hash + Time + Manifest + Seal

SHA-256 alone is not enough. We bind file → time → inspection.

## On upload (server)

1. Receive bytes, stream SHA-256.
2. Set `received_at_server = now()` (never trust device clock).
3. Persist Evidence row with `original_sha256`, `inspection_id`, `room_id`, `sequence`.
4. Store original immutable at `originals/{inspection_id}/{evidence_id}.bin`.
5. Return `evidence_id` + server timestamp as ack.

Conceptual evidence:

```json
{
  "evidence_id": "ev_98412",
  "inspection_id": "ins_5543",
  "room": "kitchen",
  "created_at_server": "2026-09-25T13:42:18Z",
  "original_sha256": "3e8af91...",
  "mime": "image/jpeg",
  "size": 4838291,
  "sequence": 17
}
```

## On seal

1. Freeze inspection (`DRAFT → SEALED`).
2. Build `manifest.json` sorted by `sequence` (all evidence hashes).
3. `manifest_sha256 = SHA256(manifest.json)`.
4. Sign manifest (HMAC/ed25519 server key, MVP: HMAC).
5. Generate Report with `report_sha256`, public `verify_code`.
6. PDF embeds: report ID, sealed_at, count, report hash, QR → `/v/{code}`.

## Verify

`GET /v/{code}` returns: authentic?, sealed_at, count, report SHA-256, manifest SHA-256. No public photos by default.
