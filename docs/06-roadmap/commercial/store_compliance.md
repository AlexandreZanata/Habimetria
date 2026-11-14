# Store Compliance — Official Sources and Recheck Gates

Checked 2026-11-11. Target market: Brazil initially. This checklist and
implementation do not guarantee store approval. Recheck on every submission.
Policy exceptions and external-billing programs vary by storefront; baseline
uses native store purchases for monthly digital services.

## Commerce and price

- Apple [review guidelines](https://developer.apple.com/app-store/review/guidelines/):
  IAP, subscriptions, account-dependent functionality and review access.
  Local use remains free. Connected paid value is hosted service.
- Google [payments policy](https://support.google.com/googleplay/android-developer/answer/10281818?hl=en):
  Play Billing baseline for in-app digital-service sales.
- Apple [subscription pricing](https://developer.apple.com/help/app-store-connect/manage-subscriptions/manage-pricing-for-auto-renewable-subscriptions/)
  and Google [price changes](https://developer.android.com/google/play/billing/price-changes):
  catalog publication, existing subscribers, notices and conditional consent.
  Admin cost models never override these processes.
- Apple [subscriptions](https://developer.apple.com/app-store/subscriptions/):
  sustained value, offers and subscription-group planning.
- Apple [purchase UI](https://developer.apple.com/design/human-interface-guidelines/apple-in-app-purchase):
  clear billing/renewal price and native purchase/refund affordances.

## Verification and lifecycle

- Google [security](https://developer.android.com/google/play/billing/security):
  backend verification, unique tokens, account binding and replay prevention.
- Google [integration](https://developer.android.com/google/play/billing/integrate):
  pending transactions and acknowledgment clock after PURCHASED.
- Google [subscription lifecycle](https://developer.android.com/google/play/billing/lifecycle/subscriptions):
  authoritative state, grace/hold/pause/expiry and linked purchase chains.
- Google [RTDN](https://developer.android.com/google/play/billing/rtdn-reference):
  authenticated delivery, deduplication and authoritative state lookup.
- Google [server integration](https://developer.android.com/google/play/billing/backend):
  catalog, lifecycle and financial report integration.
- Apple [Server API](https://developer.apple.com/documentation/appstoreserverapi),
  [notifications V2](https://developer.apple.com/documentation/appstoreservernotifications/receiving-app-store-server-notifications),
  [appAccountToken](https://developer.apple.com/documentation/storekit/product/purchaseoption/appaccounttoken(_:)):
  verified signed data, notification recovery and opaque account association.
- Apple [current entitlements](https://developer.apple.com/documentation/storekit/transaction/currententitlements)
  and [AppStore.sync](https://developer.apple.com/documentation/storekit/appstore/sync()):
  restoration and explicit-user-action authentication prompts.

## Privacy, identity and deletion

- Apple [account deletion](https://developer.apple.com/help/app-review/guideline-reference/5-1-1-account-deletion)
  and Google [account deletion](https://support.google.com/googleplay/android-developer/answer/13327111?hl=en):
  clear in-app deletion plus required external Google web path, associated
  data and disclosed justified retention. Subscription management is separate.
- Google [Data safety](https://support.google.com/googleplay/android-developer/answer/10787469?hl=en),
  Apple [App Privacy](https://developer.apple.com/app-store/app-privacy-details/)
  and [SDK requirements](https://developer.apple.com/support/third-party-SDK-requirements/):
  accurate app/SDK disclosures, manifests and required API reasons as applicable.
- Apple guideline 4.8 must be reassessed if third-party primary login is added.
  Email/password alone does not make social login a requirement.
- LGPD/consumer/professional requirements remain separate; use
  [Brazil applicability](../../03-architecture/brazil_report_legal_requirements.md)
  and [privacy contract](../../03-architecture/commercial_privacy.md).

## Publisher and release prerequisites

- Apple [enrollment](https://developer.apple.com/help/account/membership/program-enrollment/)
  and Google [registration](https://support.google.com/googleplay/android-developer/answer/6112435?hl=en):
  legal holder, identity verification, organization/D-U-N-S where applicable,
  paid agreements, banking/tax and team access. Owner completes external gates.
- Apple [Small Business](https://developer.apple.com/app-store/small-business-program/)
  and Google [fees](https://support.google.com/paymentscenter/answer/112622?hl=en):
  effective contract/region/program inputs to costing, never universal rates.
- Google [target API](https://support.google.com/googleplay/android-developer/answer/11926878?hl=en)
  and [Billing support](https://developer.android.com/google/play/billing/deprecation-faq?hl=en):
  current target 36; Billing 7 submission/extension deadlines have passed.
  Adopt a current supported release (at least 8 at this check).
- Apple [SDK requirement](https://developer.apple.com/news/upcoming-requirements/?id=04282026a)
  currently requires Xcode 26/iOS SDK 26 minimum for uploads; check
  [submission](https://developer.apple.com/app-store/submitting/) for the
  announced April 2027 transition. A macOS archive and real iPhone pass are required.
- Google [personal-account testing](https://support.google.com/googleplay/android-developer/answer/14151465?hl=en)
  can require 12 continuously opted-in testers for 14 days and production-access
  application. Account-specific applicability must be recorded.
- Google [billing tests](https://developer.android.com/google/play/billing/test)
  and Apple [sandbox](https://developer.apple.com/documentation/storekit/testing-in-app-purchases-with-sandbox):
  test purchases/renewal/refund/restoration with eligible sandbox/license users.
  Test-track purchases are not automatically free for every tester.

## Release record

Record source review date, storefront, developer account type, product IDs,
current SDK/library versions, price/consent policy, privacy forms, metadata,
reviewer access, test evidence and authorized publisher. Screenshots show
actual UI; no fictional purchases, privacy certifications or store approvals.
Native iOS/device/PDF/user-session approval remains user-owned under AGENTS.md.
