# Cost Accounting and Admin-Defined Monthly Prices

Binding direction: [free local / monthly hosted](../02-product/service_monetization.md).
This contract specifies implementation, not configured prices or known costs.

## Meter consumption, not private content

Meter at verified server operations: stored GiB-month (byte-hours integrated),
storage API requests, database/compute share, egress GiB, restore traffic,
queue/observability, transactional email, remote-provider units and support.
Keep exact units and UTC billing windows. GiB and decimal GB are not synonyms.
Separate originals, derivatives, retained versions and operational replicas.
Retries deduplicate by operation key. Persist reservations separately from
completed consumption. Infrastructure invoices may have minimums and nonlinear
tiers; reconciliation records the difference from attributed consumption.

Never charge for local photo counts, offline PDF pages, local verification,
local assistant runs or local disk usage. No telemetry upload is necessary
for free local operation. Do not collect photos/GPS/CPF for costing.

## CostRate inputs

Version each supplier/currency/unit/effective period, fixed monthly amount,
variable rate/tier, region, FX source/date, applicable tax handling and evidence
reference. Access to invoices/rates is finance-scoped. Rates have explicit
unknown/estimated/verified status. Missing inputs block a “ready to publish”
recommendation rather than becoming zero cost.

Account monthly cost includes:

1. Attributed variable consumption times applicable supplier rates.
2. Allocated shared fixed infrastructure, email, privacy/security controls,
   required free-to-access account operations and recipient verification.
3. Operational backup redundancy, recovery/retention reserve, abuse/refund
   reserve and the chosen support allocation.

Record the allocation basis and eligible population. Default plan: allocate
shared service cost over monthly paid-active account-days by service cohort;
also show total business overhead separately. Never divide by all installs
or double-count costs already included in supplier invoices/rates.
Before sales, use labeled capacity forecasts and expected paid accounts.
Zero eligible accounts yields “forecast required”, never divide-by-zero.

## Reproducible CostSnapshot

Use closed monthly windows plus a configurable rolling history. Record
window boundaries, rate versions, FX, eligible population, usage completeness,
allocation policy and source revision. Show cohort mean, median and high-use
percentile with sample size. Show an account's attributable estimate and
cohort allocation separately. Sparse/partial windows are estimates.

The admin can compare expected plan capacity with actual average customer
cost and stress cases. Do not derive a person's legal/credit risk from the
collection. A high estimate suggests a capacity tier; it does not authorize
silently moving that customer or changing their next charge.

## Recommended price

Solve for the smallest store-supported monthly price P such that expected
net proceeds after store charges, applicable taxes, currency adjustments and
refund reserve cover modeled cost plus admin target contribution margin.

For the restricted case where all fractions use the same gross base:

`P_min = (C + fixed_transaction_fee) / (1 - store_fraction - tax_fraction - refund_fraction - target_margin)`

Reject a nonpositive denominator. Otherwise use a documented net-proceeds
function with the real order of tax/commission calculation and piecewise
fees; do not apply the simplified formula to incompatible bases. C includes
the attribution policy above without double-counting a reserve. Use decimal
arithmetic, explicit currency and deterministic rounding. Rates are admin
inputs validated against actual contracts, not hardcoded universal 15%/30%.

Round to an allowed store price point, then recompute effective net proceeds,
expected margin and capacity stress exposure for each store/territory.
The same logical plan can have different store-localized prices if disclosed.
No numeric retail amount, margin or quota is approved by this document.

## PriceProposal lifecycle

`draft -> reviewed -> publishing -> verified -> active`, or `rejected/failed`.
Keep recommendation, admin-selected amount, reason for deviation, monthly
period, quota/plan version, storefronts, effective date, affected cohorts,
consent/notice requirements and rollback strategy. Draft edits have versions.

1. Finance/admin creates rates, snapshot and price simulation.
2. Authorized admin reviews monthly offer/capacity and confirms publication.
   This is the business approval, not repeated permission prompts to an agent.
3. Store adapter submits supported catalog changes or provides an exact
   manual console checklist where API support is insufficient.
4. Read back provider state and product availability. Failed/partial publishing
   never marks both stores active. Preserve receipts of publication attempts.
5. App displays store-returned price, period and eligibility. Server verifies
   identifiers and grants; a displayed database price is not a charge authority.
6. Existing subscriber changes use store-supported cohort/consent/notice
   processes; do not retroactively bill measured overage.

An admin can retire new sales without revoking valid paid access. Rollback
cannot erase a store contract, notification already issued or paid ledger.
Default rollout preserves existing cohorts until a reviewed migration exists.

## Guardrails and proof

- No daily automatic repricing of individuals, hidden overage or silent
  quota reduction during a paid period.
- At capacity: explain usage, preserve bytes and local work; offer a clearly
  priced voluntary monthly upgrade. Reserve recovery/export capacity.
- Test empty population, incomplete month, provider outage, FX change,
  nonlinear rates, duplicates, high-use cohort and all-invalid price points.
- Test recommendation reproducibility, audit/reason, cross-store partial
  publication and unchanged existing subscriber prices.
- Publish only after actual store prices and accountant-reviewed assumptions
  are recorded. Recheck [store rules](../06-roadmap/commercial/store_compliance.md).
