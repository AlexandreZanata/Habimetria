# Client Processing — App Does the Work, Server Keeps the Trust

Decision: the app does most processing (CPU, rendering, UX). The server
keeps the trust anchors plus billing. Device data is a labeled claim,
never a trust anchor.

## Why not 100% client-side trust

Client timestamps, hashes, and PDFs can be forged (device clock is
user-controlled). If the server blindly signs them, the seal is theater
and the "prove it months later" thesis dies. Hence the split below.

## App owns (heavy, offline-capable)

- Photo compression + `preview.webp` / `thumbnail.webp` derivation
- EXIF extraction: capture time, GPS → sent as claims
- Coverage checklist UI + offline queue + retry
- PDF rendering (layout, photos, text) as draft
- Two-pass seal: embed server seal block + QR into final PDF, upload it

## Server owns (trust + money)

- SHA-256 over received bytes, `received_at_server` from DB clock
- Manifest build + HMAC/ed25519 signature + `verify_code`
- `report_sha256` over the final uploaded PDF
- Public `/v/{code}` verification, share access log
- Payment gate at seal, quota enforcement

## Claim labeling (honest UX)

Device fields (`captured_at_device`, `latitude`, `longitude`) are stored
as-is and shown as "reported by device". Server fields
(`received_at_server`, `sha256`, `sealed_at`) are shown as "certified".
Verify page only vouches for server fields.
