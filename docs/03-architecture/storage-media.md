# Storage & Media — Originals Immutable

## Buckets / prefixes

- `originals/{inspection_id}/{evidence_id}` — immutable, private.
- `derived/{evidence_id}/preview.webp` — max 1600px.
- `derived/{evidence_id}/thumbnail.webp` — 320px.
- `reports/{report_id}.pdf` — private, served via signed URL.

## Rules

- Never expose `bucket/key` publicly. Signed URLs only, 5 min TTL.
- Compute SHA-256 server-side during streaming upload.
- Keep `mime, size, original_filename`; serve with `Content-Disposition`.
- Photo-first. Video: max 30s, max 100 MB, only via finding flow.
- Tenancy caps enforced in API: 300 photos, 30 videos, 2 GB (tunable).

## Local dev

MinIO (S3-compatible) in docker-compose. Prod: R2 / S3 / GCS via S3 API. Same code path via `StoragePort`.
