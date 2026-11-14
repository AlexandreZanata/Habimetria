# Mobile Subscription Experience — Planned Android and iOS Delivery

Authority: [monetization](../02-product/service_monetization.md),
[lifecycle](../03-architecture/subscription_lifecycle.md) and
[account lifecycle](../03-architecture/account_lifecycle.md).
This extends the existing UI; it is not permission to rebuild local storage.

## Free journey

Startup, Property, Inspection, Rooms, capture, findings, review, local seal,
PDF, local archive, assistant, comparison, declarations and OS sharing all
work without signup, API, purchase or network. There is no paid local tier.
Do not place subscription middleware around local repositories, AppContainer
startup, finalized Report access or local export/import. No artificial
commercial photo/Room/PDF cap or watermark. Technical bounds remain honest.

Add cloud entry points without changing that flow: backup/sync status in
Settings/account and an explicit action for hosted library/share publication.
Explain “Salvo neste dispositivo” versus server acknowledgment precisely.

## Connected screens

1. Account: registration/verification, login, recovery, current account,
   sessions/devices, profile, logout, deletion and support.
2. Monthly service offer: hosted benefits, exact allowances, selected monthly
   localized price, renewal, trial terms only if configured, privacy/terms,
   restore and continue-local action. No hardcoded retail amount.
3. Native checkout: store sheet; returning from it does not itself unlock.
4. Delivery state: pending approval/payment, verifying, delivered or retryable
   failure. Explain recovery before suggesting another purchase.
5. My subscription: purchase origin, validity/end, renewal status, plan usage,
   recovery/export terms and native manage/cancel/restore affordances.
6. Backup/sync: selected account/collections, bytes, transfer policy,
   queued/uploading/confirmed/failed, quota and error recovery.
7. Restore: purchases separate from snapshot bytes, staging/progress,
   conflicts, integrity results, disk limits and new signing identity.

Required account/privacy/restore/manage controls remain reachable when the
plan expires. Do not imply uninstalling the app cancels a subscription.
No external checkout link/QR/fallback in store builds without a separate
storefront-specific policy decision.

## Platform adapters

Shared feature code owns models, use cases, UI state and entitlement display.
Android adapter owns BillingClient connection, supported library version,
ProductDetails/offer selection, native purchase result and purchase queries.
iOS adapter owns StoreKit 2 products, purchase, Transaction updates/current
entitlements, finish, user-triggered AppStore.sync and management/refund UI.
Inject both through platform interfaces; UI never calls VaultApi directly.
Do not expose a common fake success adapter in production builds.

Persist pending deliveries before navigation/process loss; retry via durable
application work. Protect credentials in platform storage. Cancel/rebind
owner-scoped sync when login changes. App lifecycle and billing reconnection
must not trigger repeated checkout sheets or duplicate grants.
Calling AppStore.sync that prompts authentication requires explicit user
action; ordinary transaction observation does not require that prompt.

## Visual and accessibility contract

Retain Manrope, H monogram, pine `#123D35`, ivory `#F4F3EE` and existing
design-system tokens. Header title/icon keep consistent size and position;
context belongs in the body. Bottom navigation retains the established
active/inactive contrast. Use existing professional wide dialogs, close
controls, aligned forms, loading skeletons and touch targets.
Cloud/payment errors need actionable text, not raw API exceptions.
Never use color alone for pending, expired, failed or confirmed status.

Cover TalkBack/VoiceOver, large fonts, keyboard/safe area, dark mode, rotation,
small screens and offline behavior. Native store sheets are platform-owned;
do not redraw them to mimic an approved transaction.

## iOS release prerequisite

`app/iosApp/README.md` currently documents Swift host sources without an
assembled Xcode project. Deliver a reproducible project/scheme, Info.plist,
capabilities, framework embedding, signing/archive and privacy resources.
Linux shared compilation is not an iOS host build or a StoreKit/device pass.
macOS/iPhone, TestFlight and store review remain recorded manual gates.

## Acceptance

The [test matrix](../06-roadmap/commercial/acceptance.md) covers both clients.
The free journey must pass with billing disabled, account absent, expired
subscription, server outage and restored legacy collection. Payment errors
cannot lose draft work, rewrite sealed bytes or block local PDF/share.
