# Commercial Administration — Planned Screens and Powers

Extend the existing [web panel](web_panel.md); keep its Go templates,
Manrope, H monogram, pine/ivory palette and responsive forms/tables.
No demonstration records, localStorage account database or role selector.
Implementation stages: [roadmap](../06-roadmap/commercial/README.md).

## Screens

1. Overview: verified paid-active accounts, subscriptions by state, delivery
   failures, event lag, hosted usage/cost, open cases and service health.
   Each number has period, scope, definition and last refresh.
2. Customers: minimal identity, status, devices/sessions, service plan,
   safe purchase references, usage/cost estimate and support history.
3. Team/permissions: existing owner/admin/support/users with fine-grained
   capabilities. Add finance-scoped permissions without automatic collection access.
4. Plans/catalog: monthly capabilities, quota versions, store mappings,
   availability and retirement; prohibit paid local capability configuration.
5. Costs/pricing: rates, invoice attribution, cohort snapshots, simulations,
   reviewed PriceProposal, publication/readback and cohort migration status.
6. Purchases/subscriptions: provider state, verification, delivery/ack status,
   renewal, expiry, reversals and reconciliation differences.
7. Operations: authenticated event inbox, jobs/retries/dead letters, connector
   health, orphan/missing objects and storage capacity alarms.
8. Support: tickets/protocol, category, assigned operator, history, next step
   and response target. No customer impersonation or credential collection.
9. Finance: imported store reports, currency/period, refunds/adjustments,
   commissions, net proceeds, payouts and unresolved differences.
10. Privacy: rights requests, verified authority, export/deletion progress,
    retention/holds and strictly limited audit exports.
11. Activity: append-only commercial/identity/permission/configuration events;
    customer private content does not appear in staff activity.
12. Configuration: safe operational toggles, help/terms versions, maintenance,
    rollout, notification templates and secret-reference health only.

Implement scoped search, pagination, meaningful empty/error/loading states,
clear currency/unit labels, accessibility and safe CSV exports. No exported
formula injection; no full receipt or token in ordinary rows/CSV.

## Capability boundaries

Use existing roles plus capabilities such as `commerce.read`, `catalog.write`,
`pricing.review`, `pricing.publish`, `grants.write`, `finance.read`,
`refunds.request`, `events.replay`, `support.write`, `privacy.process` and
`operations.write`. Final names enter the permission registry with tests.
Capabilities do not imply `reports.read` or titleholder availability grants.

- Primary owner retains existing last-owner protection; manages staff/matrix.
- Admin gets only deliberately assigned commercial powers.
- Support may consult needed commercial facts and maintain cases, not edit
  prices, promote roles, reset activated customers or fabricate payment.
- Finance views reconciliation/costing and authorized store actions; not photos.
- Customer uses their own account/plan controls only.

Staff MFA and reauthentication protect price publication, privilege changes,
refund requests, grants, retention overrides and secret rotation. Record
reason, actor, target, before/after, server instant and operation identity.
Keep operational grants separate from paid revenue and store receipts.

## Store-controlled actions

Saving a price draft is not store publication. Requesting refund is not a
confirmed refund. Disabling an account is not store cancellation. Removing a
product from new sale is not terminating an existing paid period.
Label these distinctions and provide reconciliation/status rather than
editable “paid” or “refund completed” switches. Apple refund handling and
Google authorized refund/revoke operations have different capabilities.

If an action requires a console, prepare exact reviewed inputs and record
the authorized operator's result; never claim a simulated provider success.

## User web area

Account/profile/security, subscription origin and management, usage,
hosted library, explicit sharing/availability, export/restore and support.
Hosted library operations need a current hosted plan or published recovery
right. Account/security/privacy/cancellation do not.
Local-only items are labeled as on-device until explicitly enrolled/synced.
Recipient verification/shared access follows the publisher's contract and
authorization; never subscribe the recipient merely to open a shared Report.

## Acceptance

Two customers and each staff permission combination prove no cross-account
read/write, including attachments, exports, replay and background jobs.
Role revocation takes effect on the next authenticated request.
All risky actions have a concrete confirmation explaining effect and recovery.
Required tables/forms work at narrow widths and keyboard/large-font settings.
