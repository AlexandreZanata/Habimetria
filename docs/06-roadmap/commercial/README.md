# Commercial Execution Plan — Free Local, Paid Monthly Infrastructure

Status: implementation in progress; see [progress](progress.md).
Decision: owner, 2026-11-11. Baseline inspected: dev at `060d968`.
Always re-inspect HEAD; existing code and prior manual results can change.
The previous PDF requirements study is superseded wherever it proposed paid
local capabilities, annual billing or per-seal/consumable purchases.

## Approved outcome

Every on-device capability stays 100% free. Customer-facing hosted services
use admin-defined monthly subscriptions, cost-informed and published through
Google Play/App Store catalogs. No fixed retail price or quota is invented.
Required account/billing/privacy controls remain accessible when unpaid.
Local authority, SEALED terminality, private collection and existing visual identity
remain mandatory. This plan delivers a complete app/server/admin operation;
it does not claim store/legal/native-device approval by code alone.

## Read order

1. Repository AGENTS.md; [agent runbook](agent_runbook.md),
   [mandatory commit breakdown](commit_breakdown.md) and
   [implementation map](implementation_map.md).
2. [Product contract](../../02-product/service_monetization.md).
3. [Requirements](requirements.md) and [acceptance](acceptance.md).
4. [Commercial domain](../../03-architecture/commercial_domain.md),
   [lifecycle](../../03-architecture/subscription_lifecycle.md),
   [identity](../../03-architecture/account_lifecycle.md),
   [cloud](../../03-architecture/cloud_sync_contract.md),
   [privacy](../../03-architecture/commercial_privacy.md).
5. [API](../../04-backend/subscription_api.md),
   [storage](../../04-backend/subscription_storage.md),
   [cost pricing](../../04-backend/cost_pricing.md),
   [administration](../../04-backend/commercial_admin.md),
   [operations](../../04-backend/commercial_operations.md),
   [mobile experience](../../05-mobile/subscription_experience.md).
6. Current phase, [store compliance](store_compliance.md) and
   [release gates](release_gates.md); update [progress](progress.md).

## Ordered slices

| Phase | Slices | Deliverable | Gate |
|---|---|---|---|
| [1 — Foundation](phase_01_foundation.md) | C01-C08 | Free-local contract in code; account/cloud boundaries | Local regression and backend isolation |
| [2 — Identity](phase_02_identity.md) | C09-C16 | Account, recovery, mobile sessions, staff MFA and rights requests | Account lifecycle and no impersonation |
| [3 — Commerce](phase_03_commerce.md) | C17-C24 | Ledger, rights, jobs, usage/cost and price publication | Verified, idempotent commerce and reproducible pricing |
| [4 — Stores](phase_04_stores.md) | C25-C32 | Play Billing, StoreKit, verification and subscription UX | Both sandbox purchase/restore lifecycles |
| [5 — Cloud](phase_05_cloud.md) | C33-C40 | Enrolled backup, sync, restore, sharing and web library | Full collection round-trip and isolation |
| [6 — Operations](phase_06_operations.md) | C41-C48 | Admin, finance, support, privacy, resilience and capacity | Production-equivalent recovery/security/cost proof |
| [7 — Release](phase_07_release.md) | C49-C56 | Adversarial QA, devices, disclosures, beta and staged launch | All mandatory release gates recorded |
| [8 — Growth](phase_08_growth.md) | C57-C64 | Organizations, seats, remote services and commercial automation | Provider/scope decisions plus matching acceptance |

Each row is further split into eight ordered work packages in its phase file.
Use the mandatory commit breakdown so a multi-part package cannot become a
large single commit. Execute in order unless a dependency permits otherwise.
If a slice exceeds a reviewable diff or ~200 lines in one concept file,
split into `Cxx.a`, `Cxx.b` with separate meaningful tests/commit/push.
Never publish placeholder screens or fake production purchase adapters.

Phases 1-7 qualify the initial individual monthly product. Phase 8 completes
the extended scope; organizations/remote providers require the listed owner
decisions before activation, not fabricated integrations. Existing local
assistant/comparison/templates remain free throughout every phase.

## Definition of a delivered slice

Implemented contract + appropriate tests + `make verify` + applicable extra
checks + docs/progress evidence + small commit pushed to dev. A small English
PR from dev to main is reviewed after required CI; never commit to main.
No long-lived feature branches or stacked PRs. Reuse an existing dev->main
PR if one exists; otherwise create one, then wait for review/required checks.
Unrelated existing work is never reset, amended or included blindly.

No release with code-only success: store credentials/agreements, costs,
monthly price/quotas/retention, accountant/privacy sign-off, iOS host/device,
visual PDF and user sessions must have dated owner evidence. An unavailable
manual gate blocks activation of its dependent feature, not independent work.

## Repository baseline limitations

Current hosted surfaces and transport boundaries are inventoried in
[hosted entry points](../../03-architecture/hosted_entry_points.md).

- Panel has invitation/password identity, RBAC and Report-specific availability;
  no self-service mail/recovery/mobile session integration yet.
- At the inspected baseline SealService invoked PaymentPort. C01 removed
  that coupling and retired unused providers/configuration. This is not a
  subscription entitlement implementation.
- No native purchase adapters or commercial ledger found.
- Local collections are not automatically account-synchronized to the panel.
- Local multiple immutable Reports need server representation parity.
- iOS host sources exist without an assembled Xcode project; Linux compile
  does not establish native readiness.
- Enterprise test/scale/privacy plan contains planned work, not passed gates.

## Completion evidence

Use [requirements](requirements.md) to trace all 72 study IDs to slices and
[acceptance](acceptance.md) for end-to-end proof. Progress distinguishes
code checks, sandbox checks, device tests, external approvals and production
activation. Do not mark the complete program delivered while required gates
remain unverified. No implementation status advances merely by writing docs.
