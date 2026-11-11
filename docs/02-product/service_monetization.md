# Monetization Contract — Free Local, Monthly Hosted Services

Decision date: 2026-11-11. Approved product direction from the owner.
This supersedes the earlier per-seal/per-Tenancy proposal in
[paywall](paywall.md). Contract approved; implementation is planned, not delivered.
Execute the [commercial roadmap](../06-roadmap/commercial/README.md).

## Binding boundary

- Every capability executed entirely on the user's device is 100% free.
  No subscription, signup, purchase receipt, artificial commercial photo cap,
  watermark, export restriction or cloud enrollment may gate local work.
- Customer-facing services that consume Habimetria-hosted infrastructure
  require a monthly subscription with explicit allowances and a published price.
- The admin determines that price using measured/estimated cost and margin,
  then publishes it through the store catalog. Cost changes do not authorize
  arbitrary customer charges. See [cost pricing](../04-backend/cost_pricing.md).
- Identity, purchase restoration/management, account recovery, privacy requests
  and cancellation remain accessible without an active subscription. They are
  supporting obligations, not a free hosted-collection plan. Their costs are
  allocated to the service's operating model.

## Capability classification

| Capability | Commercial rule |
|---|---|
| Local Property, Tenancy, Inspection and Room management | Free |
| Capture, GPS/device claims, presets, notes and coverage | Free |
| Local review, seal, hashes, signatures and verification | Free |
| On-device PDF, templates, branding, receipts and ZIP export/import | Free |
| Local comparison, Occurrence, declarations and on-device assistant | Free |
| User-supplied certificate signing performed locally | Free app operation; external certificate acquisition is separate |
| OS share sheet or user-managed file backup | Free; no Habimetria hosting involved |
| Private cloud backup, restore and cross-device synchronization | Monthly hosted service |
| Hosted web library, server search and cloud collaboration | Monthly hosted service |
| Hosted Share links, access logs and remote verification publication | Monthly hosted service for the publisher |
| Remote AI, hosted processing or third-party trust service | Monthly service with separately disclosed allowance/provider conditions |
| Required account/billing/privacy controls | Accessible without payment |

Technical safety limits apply to both paid and free paths: available disk,
bounded decoding, supported formats and anti-corruption validation. They
must not be disguised subscription quotas. Local templates and professional
fields cannot become paid merely because they are useful to professionals.
Third-party storage paid directly by the user does not create an app paywall.

## Subscription offer

Initial implementation: individual monthly hosted-service plans, with the
same logical benefits across Android, iOS and web for the same User.
The catalog can hold multiple explicit capacity tiers, priced by the admin.
Launch only tiers whose costs, allowances and store mappings are configured.
There is no approved retail price, storage quantity, trial or annual plan.
Annual billing, consumable credits, lifetime purchases and per-seal charges
are outside this approved implementation baseline.

Team subscriptions and remote services follow later roadmap phases. All
their hosted costs must fit a monthly offer; their local equivalents remain
free. A store purchase does not automatically grant access to another User's
Property, Evidence or Report.

## User journey

1. Install and complete the entire local capture-to-share journey for free.
2. Choose a hosted capability. Explain its benefit, data transfer and price.
3. Create/sign in to the account needed for that connected service.
4. Purchase through Google Play Billing or StoreKit in the store build.
5. Verify the transaction, deliver entitlement durably, then enable service.
6. Ask which local collection to upload; never enroll it silently.
7. Offer restore, manage/cancel, usage, export and account deletion controls.

If the product later sells a purely local purchase, this contract must first
change with owner authorization; this plan must not implement one.

## Expiration and preservation

- Cancellation normally stops renewal; paid access continues for the
  store-confirmed period. Refund/revocation can end entitlement sooner.
- Local data and every local capability remain free after any billing event.
- Hosted writes require current entitlement and quota. Recovery/export and
  legally required access use the documented retention policy, not a paywall.
- Existing hosted bytes are preserved/disposed under that policy, with notice
  and legal holds where applicable. No promise of indefinite free hosting.
- Recipients do not buy a plan merely to inspect an authorized shared Report
  or verification result. The publisher funds the contracted hosted period.
  Public pages remain privacy-minimized and are not general file hosting.
- Billing never changes a Manifest, finalized PDF, signature or SEALED state.

## Acceptance invariants

Airplane-mode, never-registered and expired-account fixtures must all finish
local capture, review, seal, PDF, export, import and verification. Removing
the billing/network adapter must leave these operations available.
Admin price changes must not alter existing store contracts silently.
No local state may be labeled server-confirmed before server acknowledgment.
