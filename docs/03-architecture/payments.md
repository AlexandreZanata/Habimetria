# Payments — Monthly Infrastructure Subscriptions

Target authority: [service monetization](../02-product/service_monetization.md).
Execution: [commercial roadmap](../06-roadmap/commercial/README.md).

## Current implementation — C01

`SealService` accepts only InspectionID and has no payment dependency. API
wiring does not construct or contact a payment provider. The HTTP endpoint
still accepts the deprecated optional string `payment_proof` for old clients;
its value is ignored and grants no hosted rights. Normal ownership checks,
empty-inspection validation, signing and terminal SEALED behavior remain.

Legacy PaymentPort/Stripe/Asaas providers, their environment fields and the
default per-seal price have been removed. The obsolete payment-error-to-402
mapping is also removed. No payment provider is configured by this API.
There is no Play Billing, StoreKit, monthly catalog or entitlement ledger yet.

Existing clients may send payment_proof temporarily for compatibility; it
cannot grant subscription rights. Removing this JSON compatibility requires
a separate client-contract migration, not an unannounced rejection.
External providers are not substitutes for mobile-store digital purchases.

## Deployment migration from the legacy seam

1. Remove `PAYMENTS_MODE`, `PAYMENTS_PROVIDER`, `STRIPE_SECRET_KEY`,
   `STRIPE_API_BASE`, `ASAAS_API_KEY`, `ASAAS_API_BASE`, `SEAL_PRICE_CENTS`
   and `SEAL_CURRENCY` from deployment configuration. They are no longer read;
   leaving stale values cannot enable a paywall or provider calls.
2. Set `APP_ENV=production` for every shared/beta/production API, independent
   of whether any paid feature is enabled. Only `development` and `production`
   are accepted; an absent setting defaults to local development.
3. Configure a random HMAC_KEY of at least 32 bytes. Production and advertised
   non-loopback or HTTPS verify/panel origins reject empty, short or development
   signing keys before connecting/listening. Local HTTP loopback development
   remains bootable. HTTPS panel protection is retained independently of billing.
4. Preserve an existing secure HMAC_KEY when migrating; do not casually rotate
   it and invalidate historical verification. A legacy undersized key needs
   an explicit compatible key-rotation/preservation procedure before deployment;
   do not rewrite historical Manifests to satisfy the new configuration.

No database migration, re-seal, Report rewrite or client payment is needed.
`backend/internal/service/seal_integration_test.go` checks real stored bytes,
HMAC, server/device time separation, terminality and database failure context.
Run coverage with TEST_DATABASE_URL pointing at disposable PostgreSQL 18.6;
skipped integration is not coverage proof.

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
