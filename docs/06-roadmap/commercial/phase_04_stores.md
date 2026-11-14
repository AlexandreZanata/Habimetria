# Phase 4 — Google Play and App Store Purchases

Depends on C01-C24. Read [store compliance](store_compliance.md),
[mobile](../../05-mobile/subscription_experience.md) and [runbook](agent_runbook.md).
Developer accounts/products/credentials are manual dependencies. Continue
code/tests independently; never mark sandbox/device gates passed without runs.
Every slice commits/pushes and checks backend/mobile CI as applicable.

## C25 — Integrate Android native monthly checkout

Supported Billing Library, product/offer details, lifecycle reconnection,
opaque account binding, purchase query and pending-delivery persistence.
Shared interfaces keep billing outside local use cases. Test pending,
cancel/decline, disconnected service, app death and unavailable product.
Commit: `app(billing): integrate Google Play monthly purchases`.

## C26 — Verify and acknowledge Google purchases

Real Developer API adapter checks app/product/environment/token ownership;
atomically persist grant/outbox, then acknowledge initial delivery. Handle
pending-to-purchased clock and retry before provider deadline. Test forged
token, repeated token, two-account claim and backend outage after payment.
Commit: `feat(billing): verify and acknowledge Google purchases`.

## C27 — Reconcile Google lifecycle and RTDN

Authenticate Pub/Sub transport/topic context; query authoritative subscription
state; handle linked tokens, renewal, grace, hold, pause, cancel, expiry,
refund/revocation and missing event recovery. Add native manage/restore hooks.
Test out-of-order/duplicate events and protected event replay on real DB.
Commit: `feat(billing): reconcile Google subscription lifecycle`.

## C28 — Assemble reproducible native iOS host

Build versioned Xcode project/schemes, framework embedding, Info.plist,
privacy resources/capabilities and signing/archive instructions. Correct
the existing macOS verification command to reference the actual project.
Run macOS build/native local capture/PDF where available; Linux is not proof.
Commit: `app(ios): assemble the native Habimetria host`.

## C29 — Integrate StoreKit monthly checkout

Real StoreKit 2 bridge, products, purchase options/appAccountToken, pending,
transaction updates and unfinished delivery. Shared UI uses store-localized
monthly price and preserves offline local work. Test native lifecycle in
StoreKit testing and sandbox; include app death before durable finish.
Commit: `app(billing): integrate StoreKit monthly purchases`.

## C30 — Verify Apple transactions and ownership

Verify JWS signature/certificate chain/context, product, environment, account
binding and transaction/original chain via supported provider interfaces.
Persist verified delivery before StoreKit finish. Secure API credentials.
Test unknown issuer, wrong app/environment, duplicate chain and owner conflict.
Commit: `feat(billing): verify Apple purchase chains`.

## C31 — Process Apple V2 events and recovery

Authenticated signed notifications, durable inbox, current state/history
reconciliation, renewal/grace/expiry/refund/revocation and price-change facts.
Add currentEntitlements observation, user-triggered restore and native
management/refund request where supported. No automatic AppStore.sync prompt.
Commit: `feat(billing): reconcile Apple subscription events`.

## C32 — Complete shared subscription experience

Plan selection, verified delivery states, My subscription, usage, restore,
origin-aware cancellation/help, known duplicate-plan warning and voluntary
capacity changes. Add price/renewal/privacy disclosures and continue-local.
Test same User across Android/iOS/web, wrong store account, free/expired modes,
screen accessibility and real monthly-price/cohort readback.
Commit: `app(subscription): complete hosted plan management`.

Exit: both native sandboxes prove purchase-to-grant-to-restore. No fake
success in production, external-payment fallback or local paywall. Public
sale remains gated on hosted delivery, privacy/operations and release proof.
