# App Performance — Budgets and Techniques (2026)

MVP obsession: extremely light, extremely fast. Numbers are binding.

## Budgets

| Metric | Budget |
|---|---|
| Cold start | < 1.5 s (Baseline Profiles) |
| Shutter → preview | < 300 ms |
| Capture → queued offline | < 100 ms |
| Photo upload (4G) | ≤ 1.5 MB/file after compression |
| Android download (AAB) | < 15 MB |
| iOS overhead vs SwiftUI | ~9 MB (CMP cost, JetBrains 2025 survey) |
| PDF render (100 photos) | < 10 s, background |
| Grid scroll | 60 fps, thumbnails only |

## Stack (pinned in `toolchain.md`, not promised here)

- See `toolchain.md` for audited Kotlin/Compose/AGP/SDK pins.
  Older "2026-current" version claims are not reused unchecked.
- CameraX 1.6+ (CameraPipe stack, `MINIMIZE_LATENCY`, ZSL where supported).
  Ref: developer.android.com/media/camera/camerax.
- Coil 3 (CMP image pipeline, memory cache 15–20%, disk for thumbs).
- Room KMP (shared SQLite) + DataStore (queue state).
- Ktor CIO (uploads) + kotlinx.serialization.
- zxing-core only (QR encode, no camera scan deps).
- Zero ML, zero analytics SDKs, zero push SDKs in MVP.

## Size discipline

- R8 full mode + optimized resource shrinking (AGP 9 default; reported
  25 MB → 13 MB cases). Ref: Android Performance Spotlight Week 2025.
- Baseline Profiles for capture→queue→upload paths (~30% faster).
- AAB + `resourceConfigurations` (pt-BR, en). Vector drawables only.
- No JPEG in APK beyond launcher; no bundled fonts (system fonts).

## Speed discipline

- Full-res capture → app cache; downscale + WebP async off the UI thread.
- Never block shutter on network, DB, or hashing. Queue first, ack later.
- Uploads in WorkManager (Android) / BGTask (iOS), exponential backoff,
  idempotency-key per evidence. Cap concurrency at 2 on metered links.
- Paginate every grid; thumbnails 320px; full photo loads on demand.
