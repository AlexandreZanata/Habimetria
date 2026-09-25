# Mobile Phases — M1 to M5 (MVP = photos + evidence + PDF)

Backend S1–S7 is done; mobile consumes it. Auth/users/billing stay future.

## M1 — Scaffold + design system

- KMP template (composeApp, shared, iosApp host), CI builds Android APK
  + shared-for-iOS (full iOS assembly in M2, needs macOS runner).
- 3-color theme, system fonts, Home + Room placeholder screens.
- VaultApi (Ktor, CIO/Darwin) with MockEngine tests; feature flags file.
- DoD: release AAB < 12 MB baseline, cold start measured.

## M2 — Guided capture (core value)

- Camera (CameraX MINIMIZE_LATENCY / AVFoundation host in M3),
  room checklist, local queue (atomic JSON via okio; Room if it outgrows),
  WorkManager background upload vs S3 endpoints.
- Coverage ring UI fed by `GET /coverage`. Offline-first states.
- DoD: shutter→preview < 300 ms on mid-range device, airplane-mode
  capture with later auto-sync.

## M3 — Findings + review + seal

- Finding sheet (preset chips + note), review grid, seal call (S5).
- Seal ack screen with verify code. No login, no paywall (mode off).
- DoD: full entry flow on 4G with 2 s interruptions, zero lost photos.

## M4 — On-device PDF + share + verify

- Two-pass PDF (`pdf-ondevice.md`), QR, share sheet, in-app `/v/` view.
- `report_sha256` local-vs-server check before "saved".
- DoD: 100-photo PDF < 10 s render, < 10 MB file.

## M5 — Perf pass + beta

- Baseline Profiles, R8 audit, size audit, startup trace, 60 fps audit.
- Closed beta (100 users), crash-free > 99.5%, ANR < 0.5%.
- DoD: all budgets in `app-performance.md` green on 2 mid-range devices.

## Explicit non-goals

Accounts, passwords, magic links, payments UI, agency views, AI,
video-first, iOS < 14, tablets layout.
