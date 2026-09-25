# Docs Index — Source of Truth (English only)

All product, architecture, and execution docs. Small files, one topic each.

## 01 Vision

- `01-vision/product-thesis.md` — Evidence Vault thesis, Capture → Seal → Share → Compare

## 02 Product

- `02-product/capture-flow.md` — entry/exit guided flow
- `02-product/ux-principles.md` — radical simplicity rules
- `02-product/paywall.md` — sell protection, not PDF

## 03 Architecture

- `03-architecture/system-overview.md` — PWA/KMP + API + Postgres + Storage + PDF worker
- `03-architecture/domain-model.md` — User/Property/Tenancy/Inspection/Room/Evidence/Finding/Manifest/Report/Occurrence/Share
- `03-architecture/evidence-protocol.md` — hash + timestamp + manifest + seal + verify
- `03-architecture/api-surface.md` — REST surface for MVP
- `03-architecture/storage-media.md` — originals, previews, signed URLs, video limits

## 04 Backend

- `04-backend/go-layout.md` — Go project layout
- `04-backend/database.md` — tables, states, invariants

## 05 Mobile

- `05-mobile/kmp-layout.md` — Kotlin Multiplatform module layout
- `05-mobile/offline-upload.md` — queue, IndexedDB/DataStore, retry

## 06 Roadmap

- `06-roadmap/mvp-01.md` — frozen MVP 0.1 checklist
- `06-roadmap/evolution.md` — V1.1 → V1.5 without ERP creep

## 07 AI Ops

- `07-ai-ops/agent-workflow.md` — how agents build in slices
- `07-ai-ops/conventions.md` — naming, size limits, DoD
