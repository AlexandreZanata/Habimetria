# Commercial Domain — Planned Vocabulary and Boundaries

Status: implementation contract for the
[roadmap](../06-roadmap/commercial/README.md), not existing tables or routes.
[Inspection vocabulary](domain-model.md) remains authoritative.

## Records

- **ServicePlan**: versioned monthly hosted-service capabilities and allowances.
  Never contains a paid local capability. Individual and organization scope
  are explicit; the initial release is individual.
- **StoreProduct**: mapping from ServicePlan to store, app identifier, environment,
  product/base-plan/offer identifiers, storefront and billing period.
- **StoreTransaction**: verified transaction fact, provider identifiers,
  purchase/expiry/revocation instants, owner binding and verification provenance.
  Corrections append observations; do not edit history to pretend payment.
- **Subscription**: one provider subscription chain and its current verified
  commercial state, renewal setting and validity. Multiple chains can coexist
  during recovery; one chain cannot belong to two Users.
- **Entitlement**: effective right to a hosted capability, with subject,
  validity, allowance version and source Subscription or explicit grant.
- **OperationalGrant**: separate, expiring, reasoned administrative exception.
  Not a transaction, receipt or paid subscriber. Cannot sell local rights.
- **StoreEvent**: authenticated notification plus durable processing history.
- **UsageRecord**: attributable hosted consumption with unit and immutable
  deduplication identity; never stores photo content or signing secrets.
- **CostRate / CostSnapshot / PriceProposal**: versioned rate inputs,
  reproducible monthly cost estimates and admin-reviewed store price candidates.

These names are additions to the commercial bounded context. Never rename
Property, Inspection, Evidence, Manifest, Report, Share or Occurrence to fit it.

## Authority

Store facts determine commercial purchase validity. The entitlement service
projects those facts and explicit grants into effective hosted access.
Neither the store nor this service controls whether local work is allowed.
Server writes also require ownership/RBAC and quota; entitlement is necessary
but never sufficient authorization to another person's collection.

An installation key proves possession of that key. A login authenticates an
account. A transaction proves a store purchase. None independently proves
the truth of a photographed scene or regulated professional competence.

## Relationships and identity

- User is the initial entitlement subject; later an Organization can own
  hosted resources under [phase 8](../06-roadmap/commercial/phase_08_growth.md).
- StoreProduct points to an immutable ServicePlan version.
- Subscription references one User and one store chain; transactions and
  events retain provider history and environment.
- Entitlement can combine overlapping verified sources without summing the
  same purchase twice. Resolve overlapping capacity tiers by a documented
  policy; no accidental unlimited quota or duplicate charges.
- Store accounts and Habimetria accounts are different identities. Never
  bind a purchase using editable email, name or a screenshot alone.

## Proposed package ownership

Backend: `internal/service/commerce/` owns pure decision rules and orchestration;
`internal/store/postgres/` owns persistence; `internal/httpapi/` owns authenticated
JSON routes and panel handlers. Store/network adapters belong to a dedicated
`internal/billing/` package. Split by concept below approximately 200 lines.

KMP: `vault/account`, `vault/billing`, `vault/subscription` and `vault/sync` hold
shared models/use cases. Thin Android/iOS adapters bridge BillingClient and
StoreKit. Reuse existing AppContainer injection, local repositories and queues.
Create folder README references when those code folders are introduced.

## Required separations

Purchase state, upload state, local durability and Inspection state are four
different things. No `PREMIUM` Inspection state. No `confirmed` on offline data.
No API token, provider secret or private signing key in a Report package.
No admin action modifies a finalized PDF or changes SEALED back to DRAFT.
