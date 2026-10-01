# Capture UX — Photos First, Zero Friction

## Rules

1. No signup before value. `Protect tenancy → address → camera`.
2. One question per screen. Camera view is the home screen.
3. Shutter is the biggest element. Review happens after, never before save.
4. Every photo shows queue truth: `✓ confirmed | ↑ sending | ! waiting`.
5. Findings: `photo + preset + optional note`. No mandatory text.
6. Coverage bar per room; nudge text for missing shots (no AI).

## Local capture (current)

- No embedded preview: `Fotografar` opens the full-screen system
  camera; shots return with the selected category bound.
- Every photo is deletable individually (row, findings, files).
- The rules above still hold, except "Camera view is the home screen"
  and "Camera pre-warmed on Home", which described the old embedded
  CameraX preview.

## Screen flow (M2–M4)

```text
Home (property + coverage ring)
 → Room (checklist: overview/walls/openings/floor + shutter)
 → Finding sheet (preset chips + optional note)
 → Review (grid + coverage % + missing nudges)
 → Seal (server ack → verify code + QR)
 → PDF (render → share via WhatsApp/Email/link)
 → Verify view (in-app browser of /v/{code})
```

## Speed details

- Camera pre-warmed on Home. ZSL/`MINIMIZE_LATENCY`, flash off default
  (flash kills latency; CameraX discussion group evidence).
- JPEG quality 80–85, max edge 2048px for upload originals.
  (Trade-off recorded: pixel-peeping originals deferred post-MVP.)
- EXIF kept locally for device claims (time, GPS); upload sends
  `captured_at_device`, `latitude`, `longitude` as fields.
- Haptics on capture; shutter sound per regional API (CameraX 1.6+).
