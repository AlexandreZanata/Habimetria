# Subscription Lifecycle — Planned Verified Projection

Read [commercial domain](commercial_domain.md),
[monetization](../02-product/service_monetization.md) and
[store sources](../06-roadmap/commercial/store_compliance.md).
This is a target contract, not a statement of implemented billing.

## Normalize without losing provider facts

| Normalized observation | Hosted access |
|---|---|
| pending | No new purchase entitlement; preserve another valid source |
| active | Enabled through verified validity and applicable quota |
| grace | Enabled through store-confirmed grace validity |
| hold / paused | Disabled for that source when the store says benefit ends |
| expired | Disabled for that source |
| revoked | End source at verified revocation instant |
| verification_unavailable | No invented new grant; retain last verified rights only within their actual validity |

Cancellation is a renewal flag/reason, not automatically expiry. Keep
`auto_renew`, `valid_until`, `grace_until`, `revoked_at`, `provider_state`,
`observed_at_server` and verification source separately. A refund is an event
whose entitlement effect follows verified provider facts; do not assume every
partial financial adjustment immediately revokes all access.

## New purchase sequence

1. Authenticated User requests store-product eligibility and opaque account
   binding. Explain why the connected service needs an account.
2. Native client presents the store's product, localized monthly price and
   purchase sheet; it sends returned proof to the backend.
3. Backend authenticates User, checks app/environment/product and verifies
   proof using the provider's current supported verification interface.
4. Atomically claim the transaction/chain for the expected User, persist
   verified facts, project Subscription and Entitlement and enqueue delivery
   acknowledgment. Concurrent claims have exactly one allowed owner.
5. A durable worker acknowledges Google initial purchases after grant;
   finish StoreKit transactions only after durable verified delivery.
6. Client refreshes effective rights; failure resumes/reconciles without
   asking the customer to purchase the same service again.

Google: pending is not paid; the three-day acknowledgment window starts at
PURCHASED. Renewals do not need a new initial-purchase acknowledgment.
Apple: verify signed transaction/renewal data; keep transaction updates and
unfinished delivery recoverable. StoreKit pending/Ask to Buy is not a grant.

## Store events

Authenticate transport/payload before granting anything. Persist accepted
events before responding success. Deduplicate by store/environment/event ID
and transaction identity, preserving delivery attempts separately.
Use a durable worker with leases, bounded retry/backoff, dead-letter visibility
and audit-controlled replay. Fetch authoritative state when required.
Do not order subscriptions merely by webhook arrival or trust event names
without verified context. Handle stale events, overlap, linked Google tokens,
Apple transaction chains, refund/revocation and missing notification recovery.
Reconcile on purchase, restore, scheduled jobs and suspicious divergence.

## Cross-platform use and ownership

One User can use an eligible hosted plan on Android, iOS and web. Display
its purchase origin and manage through that store. Before selling, check
known active rights and warn about duplicate subscriptions. Independent
purchases outside the app can still occur; detect and guide their resolution.
Never silently transfer an already claimed chain to a newly signed-in User.
Offer authenticated recovery/support with evidence, audit and no impersonation.

Apple appAccountToken is an opaque UUID; Google uses obfuscated account
identifiers. Do not put email, CPF or Report IDs in these provider identifiers.
Historical/deleted account purchase claims need minimal lawful tombstones
and a defined recovery path, not unlimited raw receipt retention.

## Offline and service failure

Local features never consult entitlement. Hosted access cannot be performed
offline; queued work reports its true state. A protected cached entitlement
can explain last-known access, validity and freshness. It cannot extend store
expiry or turn unacknowledged uploads into confirmed backups.
Bound retry and clock-skew behavior; refresh before new hosted transfers.
An API outage never removes local files, prevents local PDF generation or
changes a seal. Purchased-but-undelivered states have visible recovery.

## Expiration, plans and price changes

Enforce the contracted quota version and actual store-effective plan date.
No prorating computed solely by the app. Use provider-supported upgrade,
downgrade, replacement and price-change mechanisms. Existing subscribers do
not move merely because an admin saves a PriceProposal.
After expiry, prohibit new hosted consumption except the published recovery,
account-management and legal-rights paths. Continue local operation and apply
[retention](commercial_privacy.md) without mutating historical artifacts.
