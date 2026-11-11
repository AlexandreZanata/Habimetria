# Commercial Acceptance — Evidence, Not Assumed Success

Run against exact commit/builds with synthetic data. Record expected/observed,
toolchain, app/store environment, commands, artifacts and responsible operator.
Automatic, sandbox, device, legal and owner acceptance are separate results.
All scenarios are currently unverified for this commercial program.

## Original study scenarios

| ID | Scenario | Required result |
|---|---|---|
| Q01 | Approved purchase | Verified correct owner, unique durable grant |
| Q02 | Cancel/decline checkout | No invented charge or entitlement |
| Q03 | Pending approval/payment | Wait; grant only after verified paid state |
| Q04 | App dies after payment | Recover delivery without repurchase/duplicate |
| Q05 | Backend/provider unavailable | Retry/reconcile honestly; no fabricated success |
| Q06 | Forged/repeated/stale event | Reject forgery; final state correct once |
| Q07 | Renewal/payment failure | Correct grace/hold/recovery per provider |
| Q08 | Cancel renewal | Access through actual due period; clear end date |
| Q09 | Upgrade/downgrade/price | Store effective date, cohort and required consent |
| Q10 | Refund/revocation | Verified rights update; sealed bytes unchanged |
| Q11 | Reinstall/change device | Restore eligible purchase; bytes separately restored |
| Q12 | Android/iOS/web same User | Same logical rights; manage at purchase origin |
| Q13 | Wrong account/duplicate purchase | No stolen chain or duplicate delivery |
| Q14 | Offline/clock manipulation | All local free; no endless invented paid validity |
| Q15 | Two Users and staff | No cross-collection/purchase access |
| Q16 | Backup failure/quota/conflict | Resume safely, preserve originals and SEALED |
| Q17 | Delete account with subscription | Explain billing; execute truthful retention/rights |
| Q18 | Accessibility/native platforms | TalkBack/VoiceOver, fonts, keyboard, dark mode, real camera/PDF |

## Owner's free-local and cost-pricing rules

| ID | Scenario | Required result |
|---|---|---|
| L01 | Never registered, network blocked | Complete every supported on-device feature |
| L02 | Subscription expired/revoked | Same local capabilities, original/PDF access and exports |
| L03 | No billing adapter/account service | Local repositories/startup still work |
| L04 | Many local photos/Rooms/Reports | No commercial limits or watermark; honest device bounds |
| L05 | Templates/branding/comparison/assistant/certificate | Fully local operations free, including professional workflow |
| L06 | Toggle cloud or switch User | No silent transfer/deletion; truthful local/server states |
| P01 | No paid accounts or missing cost rates | Labeled forecast/unknown, no divide-by-zero/zero-cost fiction |
| P02 | Duplicate usage/mixed units/nonlinear invoice | Correct idempotent unit-safe allocation |
| P03 | FX/fee/tax bases/margin change | Reproducible exact-decimal net-proceeds model |
| P04 | Admin changes recommendation | Draft/reason/review only; no customer charge yet |
| P05 | Partial store publish/readback failure | No false active price; preserve existing cohorts |
| P06 | High-use customer/over quota | No surprise overage/automatic repricing; voluntary plan upgrade |

## Mandatory adversarial integration packs

- Real PostgreSQL 18.6 migration replay with old/new rows, concurrent purchase
  claims, job leases, primary-owner protection and quota reserve/complete races.
- Store namespace confusion, wrong product/app/environment, token/JWS forgery,
  detached transport auth, event arrival inversion, linked token replacement,
  ack/finish retry, delivery/DB crash and dead-letter replay.
- Account verification/reset expiry/replay, enumeration/rate abuse, mobile
  refresh reuse, CSRF, staff MFA recovery, revoked device/current role and IDOR.
- Backup corrupt/missing object, unsupported version, path/size bounds, disk
  full, orphan reconciliation, multiple immutable Reports and full restore.
- Deletion/holds/retention, deletion replay after infrastructure restore,
  public metadata redaction, explicit Report grant revocation and masked logs.
- Price authority, catalog retirement, plan quota version, migration/rollback,
  operational grants excluded from paid revenue and financial import drift.

## Command gates

Existing: `make verify`, `make secrets`, `make test-integration` against a
disposable configured DB, `make verify-mobile`, `make verify-ios-macos` on
macOS after the actual host exists, relevant coverage/fixture/benchmark checks.
Extend CI with the actual new suites as implemented. A skipped suite, empty
test or fake provider is not integration evidence. Store sandbox and native
manual tests complement tests; they do not replace production approval.

## Human and platform gates

Reuse [beta readiness](../beta_readiness.md),
[local acceptance](../local_validation_acceptance.md),
[PDF acceptance](../local_pdf_acceptance.md) and
[usability acceptance](../local_usability_acceptance.md).
Android and iPhone, visual PDF/independent readers, TalkBack/VoiceOver and
actual user sessions remain user-owned gates. Record observations separately
from approval. Current SDK/store rules are rechecked at submission.

## Exit decisions

No paid sale before all mandatory initial-release gates pass. No complete
program claim before enabled extended scope and C64 evidence pass. Critical
data loss, private-data exposure, forged entitlement, false confirmation,
local paywall or sealed mutation always blocks release. Other limitations
have documented effect/owner decision, never an invented pass.
