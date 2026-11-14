# System Overview — Local Authority, Optional Hosted Services

The KMP app performs capture, durable local storage, review, seal, PDF,
share and verification without a server. This entire path is free.
SQLite/private files and platform signing keys support local authority;
see [local validation](local_validation_architecture.md).

The Go API uses PostgreSQL 18.6-alpine, private S3-compatible/local object
storage and an optional authenticated web panel. Existing API mirror and
upload seams do not yet form full account-aware backup/synchronization.

## Planned commercial extension

- Google Play Billing / StoreKit clients initiate purchases.
- Verified store events feed server-side Subscription/Entitlement records.
- Paid hosted capabilities check account ownership, entitlement and quota.
- Admin cost estimates guide store-published monthly prices.
- Original bytes, finalized Reports and SEALED inspections stay immutable.
- Cloud processing never impersonates certified device time or identity.

See [payments](payments.md),
[service monetization](../02-product/service_monetization.md) and the
[commercial roadmap](../06-roadmap/commercial/README.md).

## Boundaries

The API is stateless outside durable database/jobs/object storage. Local
hashes measure integrity; remote receipts acknowledge only actual server
observations. Private object access is authorized and short-lived.
Derivatives get separate identities. Public verification is minimized.
Authentication, privacy controls and purchase recovery are not paywalls.
