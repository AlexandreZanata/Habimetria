# Payments — Monthly Infrastructure Subscriptions

Target authority: [service monetization](../02-product/service_monetization.md).
Execution: [commercial roadmap](../06-roadmap/commercial/README.md).

## Current implementation — C01.a

`SealService` accepts only InspectionID and has no payment dependency. API
wiring does not construct or contact a payment provider. The HTTP endpoint
still accepts the deprecated optional string `payment_proof` for old clients;
its value is ignored and grants no hosted rights. Normal ownership checks,
empty-inspection validation, signing and terminal SEALED behavior remain.

Legacy `payment*.go`, environment fields and their old signing-config check
remain temporarily until C01.b retires them and replaces the signing check
independently of billing. Do not activate or reuse these unused providers.
There is no Play Billing, StoreKit, monthly catalog or entitlement ledger yet.

Do not activate the legacy payment gate as a subscription implementation.
Sealing is already decoupled. Existing clients may send payment_proof
temporarily for compatibility; it cannot grant subscription rights.
C01.b removes obsolete configuration with a documented migration.
External providers are not substitutes for mobile-store digital purchases.

## Target boundary

- Local use cases have no account or payment dependency.
- Authenticated hosted use cases enforce ownership, entitlement and quota
  at the server boundary; navigation visibility is not authorization.
- Google Play Billing and StoreKit initiate monthly digital-service purchases.
- The backend verifies store facts and maintains a durable commercial ledger.
- The backend is commercial authority; SQLite remains local collection authority.
- Subscription events never redefine domain Inspection states or trust claims.
- Admin price publication follows store tiers, consent and renewal rules.
- Required account, billing and privacy operations remain available when unpaid.

Store-specific purchase and event adapters stay outside domain services.
Keep `handler -> service -> store`; no store API call from SQL or mobile UI.
See [commercial domain](commercial_domain.md),
[lifecycle](subscription_lifecycle.md),
[API](../04-backend/subscription_api.md) and
[storage](../04-backend/subscription_storage.md).

## Payment acceptance

No client boolean, supplied price, uploaded screenshot, success callback,
webhook type alone or admin-edited status proves a paid transaction.
An admin can issue a separate auditable, expiring operational grant where
authorized; it must never fabricate a store transaction or paid revenue.
No runtime development override can grant production paid entitlements.

## External checkout

The approved mobile baseline uses native store purchases. Web checkout,
external purchase links and alternative-billing programs require a separate
market-specific policy decision before implementation. Do not introduce
them through a fallback when store products are unavailable.
