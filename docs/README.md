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
- `03-architecture/client-processing.md` — app does the work, server keeps trust
- `03-architecture/payments.md` — Stripe + Asaas seam, off by default in MVP
- `03-architecture/local_validation_architecture.md` — planned local authority, immutable snapshots and offline verification

## 04 Backend

- `04-backend/go-layout.md` — Go project layout
- `04-backend/database.md` — tables, states, invariants

## 05 Mobile

- `05-mobile/kmp-layout.md` — Kotlin Multiplatform module layout
- `05-mobile/offline-upload.md` — queue, IndexedDB/DataStore, retry
- `05-mobile/app-performance.md` — budgets, 2026 stack, size/speed discipline
- `05-mobile/capture-ux.md` — photo-first flow, zero friction rules
- `05-mobile/pdf-ondevice.md` — two-pass seal PDF, QR, budgets

## 06 Roadmap

- `06-roadmap/mvp-01.md` — frozen MVP 0.1 checklist
- `06-roadmap/mobile-phases.md` — M1→M5 app plan (MVP = photos + evidence + PDF)
- `06-roadmap/evolution.md` — V1.1 → V1.5 without ERP creep
- `06-roadmap/local_validation_plan.md` — next phases: Material 3, manual rooms, local capture/seal/PDF on Android and iOS
- `06-roadmap/local_validation_acceptance.md` — planned device matrix, failure cases and functional-proof gates

## 07 AI Ops

- `07-ai-ops/agent-workflow.md` — how agents build in slices
- `07-ai-ops/conventions.md` — naming, size limits, DoD
