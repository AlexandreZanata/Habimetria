# Mandatory Commit Breakdown — Keep Each Delivery Reviewable

The 64 phase IDs are ordered work packages. A package with multiple items
below MUST become ordered sub-slices `Cxx.a`, `Cxx.b`, etc., each with its own
meaningful tests/docs, verify, commit, push and CI. No package-sized mega commit.
Further split any item exceeding a coherent reviewable diff/~200-line file.
The parent is complete only after all sub-slices and applicable evidence pass.
Phase dependencies still apply; this list does not authorize fake interim UI.

| Package | Ordered independently deliverable sub-slices |
|---|---|
| C01 | Remove payment dependency; retire compatibility/config after callers migrate |
| C02 | Inventory/classification doc; enforce/test route registry |
| C03 | Free local fixture/core; PDF/package/assistant/comparison regression |
| C04 | Private route ownership; legacy/job/public-route compatibility |
| C05 | Catalog migration/constraints; service/query validation |
| C06 | Validated config/env parity; isolated exposure/rollback controls |
| C07 | Capability registry/guards; reasoned audit/concurrency |
| C08 | Disposable infrastructure; multi-owner fixtures/check commands |
| C09 | Registration/token schema; real mail/outbox delivery |
| C10 | Atomic verification; shared login/API/web integration |
| C11 | Recovery service/security; complete web/native integration |
| C12 | Session/rotation backend; Android protected storage; iOS protected storage |
| C13 | Signup/login/verification UI; recovery UI; profile/devices/logout UI |
| C14 | MFA enrollment/challenge; recovery/reauthentication guards |
| C15 | Rights request intake/status; public deletion page; in-app controls |
| C16 | Sensitive changes; account-switch/queue isolation |
| C17 | Ledger migration/constraints; service ownership/observations |
| C18 | Pure entitlement projection; durable query/guard; separate grants |
| C19 | Inbox/attempt persistence; bounded authenticated handler integration |
| C20 | Durable lease/retry engine; dead-letter/replay visibility |
| C21 | Usage idempotency/units; transactional quota reservations |
| C22 | Rates/invoice allocation; snapshots/cohorts; edge-case/net cost proof |
| C23 | Decimal price simulation; admin draft/review surface |
| C24 | Google catalog adapter; Apple catalog adapter/console workflow; publication/readback/cohort orchestration |
| C25 | Billing interface/native products; checkout/delivery state; lifecycle recovery |
| C26 | Provider proof/ownership; durable grant/ack worker |
| C27 | RTDN authentication/inbox; lifecycle/reconciliation; restore/manage/refund integration |
| C28 | Reproducible host/project; native resources/framework; archive/verification commands |
| C29 | Products/checkout bridge; updates/pending persistence; native failure tests |
| C30 | JWS/context verification; ownership/delivery persistence |
| C31 | V2 intake/lifecycle; history reconciliation; restore/manage/refund bridge |
| C32 | Monthly offer; pending/delivery recovery; subscription management; quota/upgrade; cross-platform proof |
| C33 | Enrollment/ownership backend; explicit account/data selection UI |
| C34 | Multi-Report schema migration; immutable snapshot service/compatibility |
| C35 | Transfer/reserve API; resumable client; verified completion/orphan recovery |
| C36 | Complete snapshot manifest/inventory; atomic finalize/list and missing-data proof |
| C37 | Change cursor/revisions; DRAFT conflict handling; tombstones/offline rejoin |
| C38 | Authorized restore staging/import; Android clean-device proof; iOS clean-device proof |
| C39 | Paid publisher/private links; recipient/verification redaction; expiry/revoke/access events |
| C40 | Hosted library/search; immutable document versions; recovery/export/subscription surfaces |
| C41 | Overview; customers; plans/catalog; purchases/subscriptions; usage; event/jobs; permissions/audit; safe configuration |
| C42 | Import schema/parser; ledger/payout differences; cost/price finance views; safe exports |
| C43 | Support cases/workflow; user help/history; transactional notices/delivery |
| C44 | Policy/holds; safe export; deletion/disposal; backup/deletion replay proof |
| C45 | Safe metrics/logs; operator dashboards/alerts/runbooks |
| C46 | Backup/key/restore configuration; timed isolated restore/integrity proof |
| C47 | Automated attack/dependency checks; one fix per finding; independent retest record |
| C48 | Representative load; high-use cost/margin proof; activation configuration guards |
| C49 | Google hostile sandbox matrix; Apple hostile sandbox matrix; price/cohort failure proof |
| C50 | Free/no-account/expired local matrix; private collection/restore immutability matrix |
| C51 | Android store artifact/signing; owner device/performance evidence |
| C52 | Native iOS archive; owner iPhone/TestFlight/accessibility evidence |
| C53 | Privacy/terms/retention; store forms/SDK inventory; real metadata/screenshots; reviewer access |
| C54 | Applicable tester/beta setup; user-owned acceptance/defect closure |
| C55 | Exact release/rollback preparation; owner-approved staged publication evidence |
| C56 | Store delivery/alerts reconciliation; owner launch acceptance record |
| C57 | Organization/membership schema; explicit ownership transitions |
| C58 | Invites/roles/revocation; team collection UI; concurrent collaboration proof |
| C59 | Reviewed monthly seat model; allowance/assignment service; native purchase/lifecycle/UI |
| C60 | Selected provider/privacy/metering; real remote jobs; explicit suggestion review/failure UI |
| C61 | Selected trust-provider/format decision; real issuance; independent verification/revocation; end-user disclosure |
| C62 | Scheduled official imports; cohort cost/recommendation automation |
| C63 | Configured store eligibility; native monthly offers; campaign/preferences and lifecycle proof |
| C64 | Traceability/evidence closure; full enabled-scope acceptance and operating handoff |

An evidence-only sub-slice records a real performed check or an honest external
block; it must not create a fictional pass just to generate a commit. Group
trivial evidence updates where appropriate, while preserving required review
and every implementation commit/push. All actual owner gates remain manual.
