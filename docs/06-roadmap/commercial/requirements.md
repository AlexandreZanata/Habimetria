# Requirements Traceability — 72 Administrative Study IDs

Status: planning only. A mapped slice is not implemented evidence.
All obligations follow the owner's 2026-11-11 override: every local feature
free, monthly hosted services only. Old study examples of annual/per-use
pricing or paid local branding/templates are superseded. Business inputs
and external approvals are tracked in [release gates](release_gates.md).
Read the phase documents for scope, commit and dependency details.

| ID | Binding obligation | Slices | Required evidence |
|---|---|---|---|
| IAM-01 | One account for connected app/web; anonymous local use | C09-C13 | Same owner, no local login dependency |
| IAM-02 | Verified mail and customer-owned recovery | C09-C11 | Expiry/replay/abuse and real delivery |
| IAM-03 | Revocable devices and rotating sessions | C12-C13, C16 | Refresh reuse and two-device revoke |
| IAM-04 | Staff MFA and protected recovery | C14 | Privileged action and recovery denial cases |
| IAM-05 | Account deletion inside app and public web path | C15, C44 | Real disposition/status independent of plan |
| IAM-06 | Explicit local ownership enrollment and purchase binding | C16, C26, C30, C33 | Wrong-account claim/upload rejected |
| ADM-01 | Real scoped operational overview | C41, C45 | Defined metrics and no demo counters |
| ADM-02 | Minimal customer/support workspace | C41, C43 | Purchase assistance without private content |
| ADM-03 | Fine-grained staff commercial powers | C07, C41 | Support/finance privilege tests |
| ADM-04 | Protected reasoned audit | C07, C41 | Actor/server time/before-after without secrets |
| ADM-05 | Connector/job health and safe replay | C19-C20, C41, C45 | Durable retry without duplicate effect |
| ADM-06 | Versioned safe settings/content/rollout | C06, C41, C55 | Reviewed configuration with reversible exposure |
| CAT-01 | Monthly catalog and store mappings | C05, C24 | App/environment/product/plan consistency |
| CAT-02 | Store-authoritative price and availability | C23-C24, C32 | Publication/readback and native price display |
| CAT-03 | Finite hosted allowances and measured consumption | C21, C35, C48 | Concurrent quota and no local commercial cap |
| CAT-04 | Configured official offers and separate grants | C18, C63 | Eligibility/expiry, no fabricated revenue |
| CAT-05 | Monthly tier/price changes by store rules | C24, C32, C49 | Effective date, legacy cohort and consent |
| CAT-06 | Immutable plan versions and compatibility | C05, C17-C18 | Old purchased rights preserved |
| GGL-01 | Native Play monthly purchase | C25 | Real product and native checkout |
| GGL-02 | Backend verification and unique owner claim | C26 | Forgery/replay/two-owner race |
| GGL-03 | Pending and durable acknowledgment | C26 | No grant pending; recover delivery/ack |
| GGL-04 | Authenticated RTDN and authoritative refresh | C19, C27 | Duplicate/stale/missing events |
| GGL-05 | Complete Google subscription lifecycle | C27 | Grace/hold/pause/cancel/expiry/recovery |
| GGL-06 | Manage/restore/refund/revoke correctness | C27, C32, C49 | Native controls and verified changes |
| APL-01 | Native StoreKit monthly purchase | C28-C29 | Host/device plus sandbox product |
| APL-02 | JWS/context/account ownership verification | C30 | Wrong issuer/app/environment/owner denied |
| APL-03 | Durable grant before transaction finish | C29-C30 | App death and retry exactly once |
| APL-04 | Authenticated V2 notifications and recovery | C19, C31 | History/state recovery, no invented event |
| APL-05 | Restore/current rights/native management | C31-C32 | Clean install and user-triggered sync prompt |
| APL-06 | Apple renewal/refund/revocation semantics | C31, C49 | Request differs from final refund |
| ENT-01 | Append-only paid facts and source history | C17 | Namespace/chain uniqueness and history |
| ENT-02 | Common effective hosted-rights engine | C18 | App/web equivalent rights and isolation |
| ENT-03 | Same account across supported platforms | C18, C32 | Android/iOS/web, origin-aware management |
| ENT-04 | Detect known double subscription/account conflict | C26-C27, C30-C32 | No silent transfer or duplicate grant |
| ENT-05 | Honest offline freshness and failure recovery | C03, C32, C50 | Free local path; no fabricated cloud confirmation |
| ENT-06 | Expiry preserves local rights and defined recovery | C32, C40, C44 | No local loss or sealed mutation |
| SYN-01 | My subscription and usage | C32 | Correct dates/origin/status/allowances |
| SYN-02 | Private owner-scoped hosted library | C04, C40 | IDOR and real PDF access tests |
| SYN-03 | Selected durable backup with consent/ACK | C33, C35-C36 | Interrupted upload and completeness proof |
| SYN-04 | Real clean-device restore | C34, C38 | Exact bytes/references/public key material |
| SYN-05 | Revision conflicts, tombstones and versions | C34, C37 | No resurrection/SEALED overwrite |
| SYN-06 | Controlled hosted sharing | C39 | Paid publisher; authorized unbilled recipient |
| PRI-01 | Processing inventory, purpose and minimization | C15, C44, C53 | Real data/SDK/provider records |
| PRI-02 | Accurate public policies and contract versions | C43-C44, C53 | Live privacy/terms/deletion/support URLs |
| PRI-03 | Actual rights/retention/holds/disposal | C44, C46 | Export, erasure and no restore resurrection |
| PRI-04 | Private collection separate from commerce/support | C04, C07, C41 | Explicit Report access still required |
| PRI-05 | Immutable originals/Reports and terminal seal | C01, C03, C34-C38 | Byte hashes and state regression |
| PRI-06 | Honest trust/professional/privacy claims | C50, C53, C61 | Separate legal/professional/manual acceptance |
| SEC-01 | Authorization in all routes/jobs/exports | C02, C04, C47 | Cross-customer/organization adversarial tests |
| SEC-02 | TLS/private storage/secret protection and rotation | C06, C12, C46-C47 | No embedded secrets; recovery/rotation proof |
| SEC-03 | Store event authenticity and bounded intake | C19, C27, C31 | Forged auth/signature/replay rejected |
| SEC-04 | Abuse limits and risk response | C09-C12, C35, C47 | Rate limits without customer lockout abuse |
| SEC-05 | CI, dependencies, secrets and release provenance | C08, C47, C55 | Exact-head checks and signed artifact lineage |
| SEC-06 | Independent targeted security assessment | C47, C54 | Findings/resolutions/retest; external gate honest |
| OPS-01 | Reproducible isolated deployment/migrations | C06, C08, C55 | Production-equivalent readiness/rollback |
| OPS-02 | Durable jobs and reconciliation | C19-C20, C27, C31 | Lease loss/restart and no lost purchase |
| OPS-03 | Actual service/commercial monitoring | C41, C45 | Injected failure produces actionable alert |
| OPS-04 | Timed operational restore | C46 | DB+objects+keys with RPO/RTO evidence |
| OPS-05 | Incident/privacy response | C45-C47, C53 | Runbook/tabletop and responsible operator |
| OPS-06 | Cost/capacity model and budgets | C21-C23, C48 | Average/high-use load and margin proof |
| FIN-01 | Official sales/adjustments/payout reconciliation | C42, C62 | Idempotent imports and explained differences |
| FIN-02 | Fiscal/banking obligations established | C48, C53 | Accountant and store account evidence |
| FIN-03 | Store-specific refund/dispute support | C27, C31, C43 | Request/status versus real store outcome |
| FIN-04 | Customer service with protocol and ownership | C43 | Response flow without credentials/impersonation |
| FIN-05 | Transactional mail/security/retention notices | C09, C43-C44 | Delivery/failure/audit and marketing separation |
| FIN-06 | Defined commercial metrics excluding tests/grants | C42, C45, C62 | Reproducible revenue/churn/cohort measures |
| PUB-01 | Legal publisher identity, banking and agreements | C06, C53, C55 | Owner-completed console prerequisites |
| PUB-02 | Approved monthly products and territories | C24, C53, C55 | Real product status and mappings |
| PUB-03 | Accurate privacy/permissions/SDK declarations | C28, C51-C53 | Forms match collection and native behavior |
| PUB-04 | Supported Android target/Billing and secure AAB | C25, C51 | Current policy/build/signing/device artifacts |
| PUB-05 | Native iOS host/archive/TestFlight qualification | C28-C31, C52 | macOS/iPhone proof, no Linux substitution |
| PUB-06 | Reviewer access, beta and store submission | C53-C56 | Applicable tester/review/owner acceptance |

## Scope extensions tracked by phase 8

C57-C59 extend account/ownership/catalog/entitlement/security requirements to
organizations and monthly team capacity. C60-C61 add separately selected
remote AI/trust providers under the same privacy/metering/integrity contracts.
C62-C63 automate finance and optional monthly offers. C64 rechecks every ID.
Do not mark these delivered because the individual product passed.
