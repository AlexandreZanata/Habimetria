# Release Gates and Required Business Inputs

Status: UNVERIFIED unless actual dated evidence is linked in
[progress](progress.md). Documentation and mocked tests do not close a gate.
These are dependencies, not repeated approvals for ordinary repo changes.

## Owner-controlled configuration

| Gate | Required concrete input/evidence | First dependency |
|---|---|---|
| G01 Publisher | Legal holder/PF-PJ, console identity, domain/contact, D-U-N-S where required, team authority | Live provider setup |
| G02 Store commerce | Paid agreements, bank/tax setup, app IDs, monthly products, sandbox/license users and API credentials | Store sandbox/price publication |
| G03 Cost basis | Supplier contracts/rates/FX, fixed allocations, support/retention cost, paid-account forecasts | Cost recommendation |
| G04 Monthly offer | Admin-selected prices, margin, exact quotas/units, tiers, validity and storefront scope | New sale activation |
| G05 Retention/privacy | Per-class durations/bases, recovery window, notices, holds, deletion/backup semantics and processor terms | Hosted enrollment |
| G06 Fiscal/consumer | Accountant/legal operating procedure, terms, invoice/refund/consumer support duties | Production sale |
| G07 Production | TLS/private storage/secret rotation, deployment, capacity, alert/on-call targets, RPO/RTO and restore drill | Hosted production |
| G08 Native proof | Actual Android/iPhone/macOS build, capture/PDF/accessibility/store lifecycle | Store candidate acceptance |
| G09 User/PDF acceptance | Existing user sessions, independent visual PDF/package review, beta criteria | Launch approval |
| G10 Store submission | Privacy forms/SDK reasons/metadata/reviewer access and actual store review outcome | Store publication |
| G11 Launch authority | Exact reviewed release/build/prices, rollback, support coverage and owner go-ahead | Production rollout |
| G12 Extended scope | Organization ownership/seat billing, remote providers, allowance, transfer/trust conditions and optional monthly offers | Corresponding phase-8 activation |

No price, quota, legal retention duration or SLA is silently invented.
Admin UI can prepare concrete reviewable configuration; missing values leave
the dependent hosted offer unavailable, while free local use remains complete.
Real store credentials never appear in progress/evidence or committed files.

## What code can establish

Correct privilege enforcement, transaction/byte integrity, immutable history,
cost arithmetic, idempotency, configuration validation, migrations, unit and
integration failure recovery, build output and documented test observations.

## What code cannot establish alone

Legal basis or fiscal opinion, universal professional validity, current
council competence, production store-account verification, accepted store
agreements, successful app review, independent native device/user approval,
or an operational restoration that was never performed.

## Activation order

Development fixtures -> isolated integration -> configured store sandbox ->
closed beta -> owner-approved staged store release -> monitored expansion.
Use configured feature exposure to prevent incomplete sale paths. Keep
delivery/restore/privacy services available when new sales are stopped.
Do not erase subscriber contracts or evidence on rollback.

An unavailable gate does not stop independent slices. Record exactly which
dependent validation/activation is blocked and continue safe code work.
The implementing agent must not collect real payment merely to claim a pass.
