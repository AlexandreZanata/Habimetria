# Phase 6 — Complete Administration and Production Operation

Depends on C01-C40. Read [admin](../../04-backend/commercial_admin.md),
[operations](../../04-backend/commercial_operations.md),
[pricing](../../04-backend/cost_pricing.md) and [runbook](agent_runbook.md).
Every slice is independently verified, committed and pushed.

## C41 — Complete commercial admin workspaces

Real overview/customer/catalog/subscription/usage/event/configuration screens,
scoped search/pagination, role matrix, reasons and audit. Reuse existing panel
assets/components; no private collection through finance/support privileges.
Test each capability combination, reauthentication and responsive/keyboard UI.
Commit: `feat(admin): complete commercial operations workspaces`.

## C42 — Reconcile finance and customer costs

Official store report import, duplicate detection, currency/period handling,
gross/net/payout separation and differences linked to verified ledger.
Show per-account/cohort cost and price publication results. Export safe CSV.
Test refunds, FX, late adjustments and invoice attribution without double count.
Commit: `feat(finance): reconcile store proceeds and service costs`.

## C43 — Deliver support and transactional communication

Ticket/protocol/assignment/history/outcome plus owned user help screen.
Real mail templates for security, quota, subscription/retention and incidents;
delivery/failure/retry tracking and marketing separation. Distinguish store
refund request from completed reversal. Test no impersonation/private leakage.
Commit: `feat(support): deliver customer cases and service notices`.

## C44 — Execute privacy export, retention and deletion

Owner/legal-configured durations, bases, backup rules and legal holds are
external prerequisites. Implement jobs for C15 intake, bounded safe exports,
account/session shutdown, cloud disposal and minimal justified purchase claims.
Test expired plan, PII-bearing old Manifest, hold expiry and deletion replay
after operational restore. Never report deletion before it actually completes.
Commit: `feat(privacy): execute retention and account rights workflows`.

## C45 — Monitor service and commercial integrity

PII-minimized logs/metrics, actual subscription/cost/usage definitions, store
event/ack lag, sync/restore/email health and actionable alerts/runbooks.
Exclude sandbox/grants from paid KPIs. Inject connector failures and prove
an operator can detect undelivered paid service without reading customer photos.
Commit: `infra(observability): monitor paid service delivery`.

## C46 — Rehearse infrastructure recovery

Production-equivalent DB/object/key backup configuration and timed isolated
restore; verify receipts/hash inventory and replay deletion/holds before access.
Record RPO/RTO evidence, recovery cost and rollback constraints. Owner gate
remains manual; code/config presence alone is not a successful drill.
Commit: `infra(recovery): qualify commercial backup restoration`.

## C47 — Review security and supply chain

Target auth/MFA/IDOR/store events/replay/upload/price publication and public
share paths. Add vulnerability/secret/license inventory checks and release
artifact provenance. Resolve critical findings and rerun tests; independently
review when qualified reviewer access is available. No invented approval.
Commit: `fix(security): close commercial release findings`.

## C48 — Qualify capacity, margins and configuration

Measure representative load and high-use cost, set explicit service/alert
budgets and verify price proposals with configured allowances/rates. Confirm
tax/fee assumptions with responsible operators. Reject missing required inputs
at sale activation; ensure current users/local work survive disabled sales.
Commit: `infra(commerce): qualify capacity and monthly service margins`.

Exit: the business can support, account for, monitor and recover the paid
service. Fiscal/privacy acceptance and actual rates/price/retention are
recorded external gates, not silently filled defaults.
