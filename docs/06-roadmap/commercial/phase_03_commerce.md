# Phase 3 — Ledger, Entitlements and Admin Pricing

Depends on C01-C16. Read [storage](../../04-backend/subscription_storage.md),
[lifecycle](../../03-architecture/subscription_lifecycle.md),
[pricing](../../04-backend/cost_pricing.md) and [runbook](agent_runbook.md).
Each slice includes meaningful tests, docs, verify, commit, push and CI.

## C17 — Persist verified commercial observations

Incremental transaction/chain/Subscription schema with ownership, namespace,
environment and immutable observation constraints. Add service/store APIs;
test duplicate proof, linked identities, two-owner races and migration replay.
Do not add an editable isPremium flag or fabricate paid rows for development.
Commit: `db(commerce): record purchase chains and observations`.

## C18 — Project effective hosted entitlements

Pure lifecycle/quota-version rules plus transactional projection and query API.
Add separate expiring grants with capability/reason/audit. Account controls
remain accessible. Test grace, overlapping sources, revocation, expiry,
account isolation and unchanged local operations.
Commit: `feat(commerce): project verified hosted entitlements`.

## C19 — Persist authenticated notification intake

Add event inbox/dedupe/attempt schema and bounded transport handlers wired only
to actual configured provider authentication. Invalid traffic cannot enqueue
trusted facts; accepted events are durable before response. Provider-specific
verification completes C27/C31 before exposing those endpoints publicly.
Test false auth, duplicate event and DB failure with non-production fixtures.
Commit: `feat(commerce): persist authenticated store event delivery`.

## C20 — Execute durable commerce jobs

Leases, bounded retry/backoff, outbox, dead-letter handling and replay audit.
Implement projection/refresh/ack job types only with real adapters as they
land; unconfigured features stay unexposed, not fake successful. Test process
death, lease loss, retry exhaustion and exactly-once business effect.
Commit: `feat(commerce): recover durable purchase delivery jobs`.

## C21 — Meter hosted usage and reserve quotas

Add unit-safe usage records, atomic reserve/complete/release and owner-scoped
usage endpoints. Cover retries, concurrency, reservations crossing plan expiry,
quota changes and over-capacity refusal. Never meter free local operations.
Commit: `feat(usage): meter hosted consumption and reservations`.

## C22 — Calculate reproducible customer cost

Version supplier/FX rates, allocation policy and CostSnapshots; support forecast
versus measured inputs, cohort mean/median/high-use and per-account estimates.
Test zero paid accounts, incomplete window, nonlinear invoice rate, units,
deduplication and fixed-cost double counting. Unknown costs never become zero.
Commit: `feat(pricing): calculate versioned monthly service costs`.

## C23 — Review admin monthly PriceProposals

Implement margin/net-proceeds simulation, valid store-price rounding,
admin-selected amount/reason, review and scheduled effective/cohort policy.
Test decimal math, incompatible tax bases, nonpositive denominator, missing
inputs and no silent individual repricing. Add an operational admin form.
Commit: `feat(pricing): review cost-informed monthly price proposals`.

## C24 — Publish and reconcile store catalog changes

Real store catalog adapters or exact console-assisted publication workflow,
safe dry-run preview, authorized submission, readback and partial-failure state.
Provide monthly product mapping to clients; existing subscriber changes need
their own reviewed cohort/consent flow. Test one-store failure and rollback.
Store credentials/publication evidence remain a manual gate when unavailable.
Commit: `feat(catalog): verify monthly price publication across stores`.

Exit: commerce service is testable and pricing reproducible. No real purchase
exposure without C25-C32 and release policy inputs. A failed store publication
is visible and does not become a successful app price or customer charge.
