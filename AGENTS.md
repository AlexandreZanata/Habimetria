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
- Hash + timestamp are server-side only. Never trust device clock.
- SEALED is terminal. No `SEALED -> DRAFT`. Use addendum/occurrence.

## 5. Mobile rules (KMP)

- Layout in `docs/05-mobile/kmp-layout.md`.
- Offline-first: capture → local queue → background upload → server ack → ✓.
- Never show "saved" before server confirmation. States: `queued | uploading | confirmed | failed`.
- Minimal mandatory text. Photo + preset + optional note.

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
