# Phase 8 — Extended Hosted Product and Automation

Activation depends on C01-C56 and stable measured operation. Independent
organization/provider code may proceed after C40-C41 while manual launch
gates remain pending, with explicit dependency evidence. This completes the extended
scope without charging for any local functionality. Provider, seat-billing,
quota, offer and privacy decisions are explicit prerequisites to activation;
implement selected providers fully, never ship fake selectable integrations.
Apply [runbook](agent_runbook.md), requirements and release gates per slice.

## C57 — Add organization identity and ownership

Versioned Organization/membership/invite vocabulary, scope, legal ownership,
paid-subject model and migration contract. Preserve individual ownership;
move/copy collection only through explicit authorized workflows with immutable IDs.
Test cross-organization access and member departure without historical loss.
Commit: `feat(teams): add organization-scoped hosted ownership`.

## C58 — Deliver team collaboration and administration

Invites, roles, property/Report authorization, member revocation, shared DRAFT
workflow and audit. Permissions remain distinct from store benefit sharing;
an admin seat cannot rewrite a seal or access another organization's collection.
Test concurrent edits, removed member, owner protection and private grants.
Commit: `feat(teams): deliver scoped collection collaboration`.

## C59 — Integrate monthly team capacity and seats

Choose a store-supported B2B/multiseat or finite monthly-tier model after a
fresh policy review for target accounts/storefronts. Define payer, seat count,
invitation reservation, reassignment and end-of-plan behavior. Do not fabricate
cross-platform Family Sharing semantics or implement per-seat external checkout
inside store apps as a shortcut. Test purchase, restore, revoke and quotas.
Commit: `feat(teams): enforce verified monthly team allowances`.

## C60 — Add a fully connected remote assistant

Owner chooses provider, rates, privacy/transfer/training terms and monthly
allowance. Explicitly select Evidence to send; authenticated metered jobs,
consent/disclosure, timeout/cancel/result provenance. Suggestions need user
confirm/ignore; no automatic Finding/liability. Existing on-device assistant
and any future fully local inference remain free and available offline.
Commit: `feat(assistant): deliver consented metered remote suggestions`.

## C61 — Integrate selected external trust services

Only after provider/format/legal decisions: actual signature/timestamp service,
identity proof, verification/revocation and independent interoperability tests.
Monthly allowance must cover provider costs; no made-up certification or
backdated shutter time. Preserve free user-supplied local certificate operation.
One fully functioning provider per sub-slice; unselected providers are absent.
Commit: `feat(trust): integrate the approved hosted trust provider`.

## C62 — Automate finance and commercial reconciliation

Scheduled official report ingestion, invoice/cost-rate review inputs,
differences/alerts and cohort forecasts. Reuse C42's real importer. Automate
price recommendations only; admin approval/store publication and subscriber
consent remain required. Test late reports, schema drift, FX and partial import.
Commit: `feat(finance): automate store and infrastructure reconciliation`.

## C63 — Add configured monthly offers and recovery campaigns

Store-supported monthly trial/promotional eligibility only after owner chooses
terms. Clear post-offer price, renewal, native redemption and user communication
preferences. No paid local benefit, invented discount, deceptive urgency or
unapproved annual/consumable product. Test ineligible and previously subscribed
accounts, refunds and offer-price state across both stores.
Commit: `feat(offers): add verified monthly subscription promotions`.

## C64 — Close the full program and regression coverage

Map all requirements to actual commits/checks and extended-provider gates;
run the full device/backup/privacy/security/cost matrix for the enabled scope.
Record final operating commitments, support ownership and residual limitations.
Do not mark complete while required scope is merely documented or disabled.
Periodic revalidation/retention/incident exercises become an operator runbook,
not automatically scheduled agent tasks without user request.
Commit: `docs(commerce): record full program acceptance`.
