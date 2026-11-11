# Commercial Configuration — C06 Contract

Authority: [operations](commercial_operations.md) and
[free local / monthly hosted](../02-product/service_monetization.md).
This is configuration/activation infrastructure, not an implemented store
adapter, purchase endpoint, offer publication or verified entitlement.
No publisher identity, product, price or secret is seeded.

## Disabled by default

Google Play, App Store and new purchase exposure default to false independently.
All three switches accept only `true`, `false` or the zero-value empty/off state.
Unknown switch values fail configuration validation; they never silently enable.
Inactive provider identifiers/credentials are not required or consumed at startup.
No commercial setting belongs in LocalSession or the free on-device journey.

`COMMERCE_ENV` is `sandbox` or `production` when any store is activated.
Production commerce requires APP_ENV=production. Hardened staging may use
APP_ENV=production with sandbox commerce: APP_ENV governs server security,
not the provider's transaction namespace. Future verified proofs must match
the configured provider/app/commerce namespace exactly; no automatic fallback.

Provider enablement and new sales are separate. New sales require an enabled
provider, but disabling new sales must leave delivery, restore, reconciliation
and notifications available for that enabled provider. Required account controls,
public authorized recipients and local features do not use the sales switch.
Disabling a provider instead is a separate incident action with delivery impact.

## Environment fields

- `COMMERCE_ENV`: empty while disabled; explicit sandbox/production on activation.
- `COMMERCE_NEW_PURCHASES_ENABLED`: false by default; a new-sale control only.
- `COMMERCE_SECRETS_DIR`: absolute normalized mounted-secret root, default
  `/run/secrets/habimetria`. Root-only `/` is not a credential directory.
- `GOOGLE_PLAY_ENABLED`, `GOOGLE_PLAY_PACKAGE_NAME`, `GOOGLE_PLAY_SECRET_REF`:
  activation flag, exact configured Android application ID and credential reference.
- `APP_STORE_ENABLED`, `APP_STORE_BUNDLE_ID`, `APP_STORE_APP_ID`,
  `APP_STORE_ISSUER_ID`, `APP_STORE_KEY_ID`, `APP_STORE_SECRET_REF`: activation
  flag, bundle ID, positive numeric App Store app ID, issuer UUID, key identifier
  and signing-key reference. App Store app ID is distinct from the bundle ID.

Enabled providers require structurally valid identifiers and a credential
reference of the exact form `<environment>/<google_play|app_store>/<filename>`.
The filename is bounded ASCII letters/digits/dot/underscore/hyphen; no traversal,
absolute path, URL, inline credential, extra component or other namespace/store.
Config errors identify the field, never echo its supplied value or secret data.
Only the selected enabled provider requires its identifiers. Android currently
builds as `vault.app`; this is not proof of publisher-console ownership. The iOS
host/app identity still needs its owner-controlled C28 qualification.

## Secrets and provider truth

References identify read-only mounted files; environment values never contain
private keys or service-account JSON. Shape validation does not read secrets,
contact providers or validate publisher permissions. Actual activation checks
the referenced file separately; provider verification remains C24/C26/C30.
Secret namespaces are a deployment policy, not a claim that credential bytes
prove whether a transaction was a test. Do not include PII in reference names.

Google test transactions are identified by verified provider data, not a made-up
sandbox Publisher API endpoint. Package identity and license-test setup must
match the real app; test tracks alone do not guarantee free test transactions.
See [Google billing tests](https://developer.android.com/google/play/billing/test)
and [SubscriptionPurchaseV2](https://developers.google.com/android-publisher/api-ref/rest/v3/purchases.subscriptionsv2).
Apple uses distinct Server API environments and app/key context; see
[environment](https://developer.apple.com/documentation/appstoreserverapi/environment)
and [JWT configuration](https://developer.apple.com/documentation/appstoreserverapi/generating-json-web-tokens-for-api-requests).
Sources checked 2026-11-11; C06 does not perform any native/store test purchase.

## Implementation references

- [commerce.go](../../backend/internal/config/commerce.go): configuration fields,
  explicit provider flags and environment loading.
- [commerce_validation.go](../../backend/internal/config/commerce_validation.go):
  conditional activation validation and namespace/reference bounds.
- [commerce_test.go](../../backend/internal/config/commerce_test.go): activation,
  inactive startup, mixing and redacted validation regressions.
- [commerce_env_test.go](../../backend/internal/config/commerce_env_test.go):
  environment/example parity and override coverage.

Operational exposure/rollback checks are the second C06 sub-slice. These
settings do not add unclassified HTTP registrations or authorize private data.
