# Subscription Storage — Planned PostgreSQL Extensions

Target only. Allocate actual migration versions from repository HEAD when
executing; never assume `0006` is still free. PostgreSQL 18.6-alpine only.
Extend [database](database.md), [domain](../03-architecture/commercial_domain.md)
and the [lifecycle](../03-architecture/subscription_lifecycle.md).

## Incremental groups

1. Catalog: `service_plans`, immutable `service_plan_versions`, `store_products`.
   Explicit monthly period, capabilities, finite quota units, subject scope,
   status and store/app/environment/product/base-plan/offer mapping.
2. Ledger: `store_transactions`, `subscription_observations`, `subscriptions`,
   `entitlements`, `operational_grants`. Preserve provider state and effective
   times; grants record reason/issuer/expiry separately from paid facts.
3. Inbox/outbox: `store_events`, processing attempts and durable jobs for
   provider acknowledgment, refresh, notification and reconciliation.
4. Metering/pricing: `usage_records`, quota counters/reservations, `cost_rates`,
   `cost_snapshots`, `price_proposals`, store publication/readback observations.
5. Identity: mobile device/refresh sessions and rotation/reuse history;
   verification/recovery tokens, staff MFA and privacy requests. Extend the
   existing users/panel identity; do not make a second user database.
6. Cloud: account/device enrollment, collection/item revisions, immutable
   backup snapshots, upload sessions and durable change cursor/tombstones.
7. Operations: support cases, finance imports/reconciliation differences,
   retention jobs/holds and access/configuration audit extensions.
8. Growth: organization membership/invites/seat assignments and hosted-service
   allowances, after individual ownership and monthly billing are qualified.

Each numbered group may require several small migrations. Ship only the
columns/indexes needed by the current runnable slice; no giant future schema.

## Required constraints

- Unique transaction per `(store, environment, app_id, transaction_identity)`;
  unique claimed subscription chain in that same namespace.
- Google token identity and linked predecessors are constrained; Apple
  transaction ID/originalTransactionId are retained with their semantics.
  Never use Google order ID alone as an idempotency key.
- StoreEvent uniqueness includes environment and provider event ID; if an
  event lacks a stable ID, define and test a canonical verified dedupe key.
- Foreign keys enforce catalog/subject/source relationships. Enforce that a
  grant has an authorized issuer and a valid end after its start.
- Entitlement validity and quota versions are explicit. Never replace
  history with an unconstrained `users.is_premium` boolean.
- UsageRecord has a unique operation key and explicit unit. Counters update
  transactionally; quota reservations have state and expiry.
- Published plan versions and recorded transaction facts are immutable;
  append verification observations and superseding versions.
- Currency is an ISO code; monetary units have explicit exponent/currency
  and exact decimal/minor-unit storage. Never use binary float for charges.
- Every instant identifies provider-effective versus server-observed time.
  Never insert device claims into server-certified fields.
- Index entitlement lookup, chain ownership, due jobs, User usage windows,
  retention deadlines and stable pagination. Bound JSON/raw event sizes.

## Transactions and failures

Claim purchase + persist observation + project entitlement + enqueue provider
ack in one transaction. External HTTP is outside open DB transactions;
use durable jobs and re-check decisions on completion. Concurrent claims,
job lease loss and process death are explicit integration-test cases.
Quota reserve/complete/release uses an idempotent operation identity and
avoids charging a byte both for a retry and its completed upload.

Keep provider secrets in configured secret storage. Sensitive proof needed
for later verification must be encrypted/access-controlled; a one-way digest
alone cannot replace a token an API must later receive. Logs and ordinary
admin lists expose only safe transaction references.

## Preservation, migration and rollback

Do not retrofit billing into Manifest/PDF payloads or rewrite local schema
history. The server currently models fewer Report versions than local mode;
explicitly migrate server multiple-Report/snapshot representation before
offering complete backup/restore. Existing Reports retain their identities.

Account deletion uses approved retention/legal-hold semantics and minimized
purchase ownership tombstones. Never casually CASCADE-delete retained sealed
artifacts, nor retain personal data forever because it was sealed.
Cloud removal is a lifecycle event; sealed bytes are unchanged while retained.

Use expand/migrate/contract; replay all migrations on a disposable database
and test existing rows, concurrent requests and failure halfway through.
Document destructive irreversibility and backup/rollback procedure. Disable
new purchase exposure on rollback; retain paid ledger and recovery workers.
