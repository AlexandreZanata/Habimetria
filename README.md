# Rental Evidence Vault — MVP

> Not an inspection form. A digital safe for rental evidence.
> Capture → Seal → Share → Compare.

Tenant-first product: prove months later **"it was already like this when I moved in."**

## Ultra-fast MVP thesis

- **Mobile:** Kotlin Multiplatform (Android + iOS), Compose Multiplatform, photo-first, guided capture, offline queue.
- **Backend:** Go + PostgreSQL 18.6-alpine + S3-compatible object storage.
- **Trust core:** SHA-256 per file + server timestamp + evidence manifest + sealed report hash + public verification page + QR in PDF.
- **Docs:** 100% English, small normalized files, AI-agent buildable and maintainable.

## Repo layout

```text
docs/       # English source of truth (vision → architecture → roadmap → ai-ops)
backend/    # Go API + worker + migrations
app/        # Kotlin Multiplatform app
infra/      # docker-compose, postgres:18.6-alpine, minio, env templates
```

## Quick start (day 0)

```bash
cp infra/.env.example infra/.env
docker compose -f infra/docker-compose.yml up --build
# API: http://localhost:8080/healthz
# Verify page API: http://localhost:8080/v/ABC123
```

## MVP 0.1 scope (freeze)

1. Property + rooms
2. Guided photo capture
3. Findings (photo + preset + optional note)
4. Original upload + SHA-256 + server timestamp
5. Seal inspection (DRAFT → SEALED, no rollback)
6. Paywall at seal: "Protect this tenancy for R$29,90"
7. PDF + QR + public `/v/{code}` verification
8. Share link + access log

Out of MVP: accounts with passwords, agency dashboard, AI diff, video-first, edit-after-seal.

See `docs/06-roadmap/mvp-01.md` and `docs/README.md`.

## Next delivery scope (in progress)

[Local validation roadmap](docs/06-roadmap/local_validation_plan.md):
professional Material 3 UI, manually named rooms, real Android/iOS photo
capture with location claims, complete on-device persistence/seal/PDF,
offline integrity verification and a reproducible device acceptance matrix.
L0–L8 are implemented with a green Linux pass (see
`docs/06-roadmap/acceptance-report.md`); UI reaches the API only through
the optional server mirror. Physical-device, simulator and macOS/Xcode
gates remain explicitly unverified.
