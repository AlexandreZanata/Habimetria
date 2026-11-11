# Docs Index — Source of Truth (English only)

All product, architecture, and execution docs. Small files, one topic each.

## 01 Vision

- `01-vision/product-thesis.md` — Habimetria thesis, Capture → Seal → Share → Compare

## 02 Product

- `02-product/capture-flow.md` — entry/exit guided flow
- `02-product/ux-principles.md` — radical simplicity rules
- `02-product/paywall.md` — hosted-service offer; historical per-seal proposal superseded
- [02-product/service_monetization.md](02-product/service_monetization.md) — all local features free; admin-priced monthly hosted services
- `02-product/local_navigation_ux.md` — planned header, global navigation, white/green light theme and accessibility

## 03 Architecture

- `03-architecture/system-overview.md` — PWA/KMP + API + Postgres + Storage + PDF worker
- `03-architecture/domain-model.md` — User/Property/Tenancy/Inspection/Room/Evidence/Finding/Manifest/Report/Occurrence/Share
- `03-architecture/evidence-protocol.md` — hash + timestamp + manifest + seal + verify
- `03-architecture/api-surface.md` — REST surface for MVP
- `03-architecture/storage-media.md` — originals, previews, signed URLs, video limits
- `03-architecture/client-processing.md` — app does the work, server keeps trust
- `03-architecture/payments.md` — legacy payment seam and target store-subscription boundary
- `03-architecture/local_validation_architecture.md` — planned local authority, immutable snapshots and offline verification
- `03-architecture/offline-package.md` — L7 published package/verify format
- `03-architecture/local_property_removal.md` — planned empty deletion, reversible archive and preservation of sealed history
- `03-architecture/brazil_report_legal_requirements.md` — official Brazilian sources, tenancy/professional applicability and limits of legal claims
- [03-architecture/commercial_domain.md](03-architecture/commercial_domain.md) — planned commercial vocabulary and authority
- [03-architecture/hosted_entry_points.md](03-architecture/hosted_entry_points.md) — C02 API/panel/mobile/worker inventory and access/funding classifications
- [03-architecture/subscription_lifecycle.md](03-architecture/subscription_lifecycle.md) — verified purchase, entitlement and recovery states
- [03-architecture/account_lifecycle.md](03-architecture/account_lifecycle.md) — connected identity, sessions, recovery and deletion
- [03-architecture/cloud_sync_contract.md](03-architecture/cloud_sync_contract.md) — paid hosted snapshots, changes and clean-device restore
- [03-architecture/commercial_privacy.md](03-architecture/commercial_privacy.md) — commercial processing, retention and immutable evidence

## 04 Backend

- `04-backend/go-layout.md` — Go project layout
- `04-backend/database.md` — tables, states, invariants
- `04-backend/web_panel.md` — web setup, authorization and report access
- [04-backend/subscription_api.md](04-backend/subscription_api.md) — planned account, purchase and entitlement HTTP contracts
- [04-backend/subscription_storage.md](04-backend/subscription_storage.md) — incremental commercial schema and transaction constraints
- [04-backend/cost_pricing.md](04-backend/cost_pricing.md) — average customer cost, margin and admin store-price publication
- [04-backend/commercial_admin.md](04-backend/commercial_admin.md) — commercial screens, capabilities and user web controls
- [04-backend/commercial_operations.md](04-backend/commercial_operations.md) — finance, support, alerts and recovery runbooks

## 05 Mobile

- `05-mobile/kmp-layout.md` — Kotlin Multiplatform module layout
- `05-mobile/toolchain.md` — pinned toolchain after L0 build audit
- `05-mobile/offline-upload.md` — queue, IndexedDB/DataStore, retry
- `05-mobile/app-performance.md` — budgets, 2026 stack, size/speed discipline
- `05-mobile/capture-ux.md` — photo-first flow, zero friction rules
- `05-mobile/pdf-ondevice.md` — sealed local PDF, immutable receipts, QR and budgets
- `05-mobile/local_pdf_contract.md` — photo orientation, complete layout, per-photo claims and immutable Report integrity
- `05-mobile/report_authorship_contract.md` — optional identity, signed Share declarations, certificate-holder signatures and editorial presentation
- `05-mobile/local-store.md` — L1 SQLite schema, files, migration rules
- `05-mobile/brand-identity.md` — Habimetria symbol, wordmark, palette, typography, voice and applications
- [05-mobile/assets/README.md](05-mobile/assets/README.md) — source brand vectors
- `05-mobile/design-system.md` — L2 Material 3 tokens, Manrope, components, pt-BR
- [05-mobile/home_artwork.md](05-mobile/home_artwork.md) — generated Brazilian house art and Home presentation
- [05-mobile/subscription_experience.md](05-mobile/subscription_experience.md) — free local journey and native monthly-service UX

## 06 Roadmap

- `06-roadmap/mvp-01.md` — frozen MVP 0.1 checklist
- `06-roadmap/mobile-phases.md` — M1→M5 app plan (MVP = photos + evidence + PDF)
- `06-roadmap/evolution.md` — V1.1 → V1.5 without ERP creep
- `06-roadmap/local_validation_plan.md` — next phases: Material 3, manual rooms, local capture/seal/PDF on Android and iOS
- `06-roadmap/local_validation_acceptance.md` — planned device matrix, failure cases and functional-proof gates
- `06-roadmap/acceptance-report.md` — L8 Linux pass: results, budgets, defect log, unverified matrix
- `06-roadmap/beta_readiness.md` — device-pass runbook and beta exit criteria (consolidates M5 + acceptance gates)
- `06-roadmap/local_usability_plan.md` — U01–U12 tasks for navigation, Property removal and local usability
- `06-roadmap/local_usability_acceptance.md` — planned usability, accessibility, integrity and performance gates
- `06-roadmap/local_pdf_plan.md` — PDF-only delivery sequence for tenancy and architecture/engineering use
- `06-roadmap/report_authorship_execution.md` — code-only identity, declarations, certificate and editorial delivery; user-owned manual gates
- `06-roadmap/local_pdf_execution.md` — implementation results, native synthetic proof and remaining release gates
- `06-roadmap/local_pdf_acceptance.md` — planned orientation, completeness, offline integrity and professional-release proof
- `06-roadmap/enterprise_assurance_plan.md` — enterprise test/scale/LGPD plan for municipal use (planning only)
- [06-roadmap/commercial/README.md](06-roadmap/commercial/README.md) — complete 64-slice execution plan for free local and monthly hosted services; requirements, store sources, progress and release gates

## 07 AI Ops

- `07-ai-ops/agent-workflow.md` — how agents build in slices
- `07-ai-ops/conventions.md` — naming, size limits, DoD
