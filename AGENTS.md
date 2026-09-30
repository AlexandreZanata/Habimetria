# AGENTS.md — AI-Agent Operating System

This repo is built and maintained 100% by AI agents. Humans review. Agents execute.

## 1. Source of truth

- `docs/` is normative, 100% English.
- Never invent domain terms. Use `docs/03-architecture/domain-model.md`.
- Never invent flow. Use `docs/02-product/`.

## 2. File rules (strict)

- One concept per file. Max ~200 lines per file. Split before growing.
- No `utils.go`, `helpers.kt`, `misc/`, god-files, giant migrations.
- Naming: `snake_case.md` for docs, Go standard layout, Kotlin feature-first.
- Every new file must be referenced from its folder `README.md` or parent doc.

## 3. Change protocol

1. Read relevant `docs/` file first.
2. Small diff. Consistent style. No TODOs, stubs, placeholders.
3. Update docs if behavior changes.
4. Run `make verify` (lint + typecheck + tests). Fix before done.
5. Commit small + push to GitHub after every change. Message: `type(scope): verb + object`.
   Never finish a slice with uncommitted or unpushed changes.

Types: `feat, fix, docs, chore, db, api, app, infra`.

## 4. Backend rules (Go)

- Layout in `docs/04-backend/go-layout.md`. Respect package boundaries:
  `handler -> service -> store`. No DB in handlers. No HTTP in store.
- Errors: explicit, wrapped with context. Never swallow.
- Server hash + timestamp are server-side only. Never trust device clock.
  Local SHA-256 is an integrity measurement; local time is a device claim.
  Never write local values into `received_at_server` or certified `sealed_at`.
- SEALED is terminal in every mode. No `SEALED -> DRAFT`. Use addendum/occurrence.

## 5. Mobile rules (KMP)

- Layout in `docs/05-mobile/kmp-layout.md`. Local authority in
  `docs/03-architecture/local_validation_architecture.md`.
- Local-first: capture → durable storage → review → seal → PDF → share.
  UI never calls `VaultApi` directly; API sync stays optional.
- "Salvo neste dispositivo" means local durability only.
  Server states `queued | uploading | confirmed | failed` mean server sync.
  Never show server-confirmed before server ack.
  Never fabricate confirmation offline.
- Minimal mandatory text. Photo + preset + optional note.
- Manually named Rooms; coverage from distinct shot categories, not counts.

## 6. Database rules

- PostgreSQL 18.6-alpine only. Migrations in `backend/migrations/*.sql`, one version per file, reversible where possible.
- Schema source: `docs/04-backend/database.md`.
- Keep originals immutable. Derived `preview.webp` / `thumbnail.webp` only.

## 7. Definition of Done

- [ ] Code follows layout + conventions
- [ ] `make verify` green
- [ ] Docs updated
- [ ] No secrets committed
- [ ] Committed + pushed to GitHub
- [ ] Small PR, English description
