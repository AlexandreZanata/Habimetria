# Phase 7 — Qualification and Store Release

Depends on C01-C48. Read [acceptance](acceptance.md),
[release gates](release_gates.md), [store rules](store_compliance.md) and
[runbook](agent_runbook.md). Each slice commits/pushes its deliverable/evidence;
manual results remain user-owned. Do not put secrets or real collection in evidence.

## C49 — Run hostile billing and price scenarios

Automated real-DB concurrency plus store sandbox proof: interrupted delivery,
missing/duplicate/out-of-order events, pending/refund/revoke, grace/hold,
account conflict, upgrade, price cohort/consent and failed store publication.
Fix defects in separate small commits; record exact build and provider evidence.
Commit: `fix(billing): qualify failure and renewal recovery`.

## C50 — Qualify free-local and private-collection regressions

Run all free capabilities with no account, expired/revoked plan, disabled
network/billing and old data. Validate manifest/PDF bytes, original hashes,
server isolation and truthful sync states. Restore a full synthetic collection.
Commit: `app(local): qualify free workflow under billing failures`.

## C51 — Qualify Android release device and store artifacts

Release AAB/signing, supported target/Billing SDK, Play licensing/test track,
real device camera/GPS/permissions/PDF/share, process death and accessibility.
Run existing performance/size checks and record actual versions/timings.
Device observations are evidence; owner acceptance stays separate.
Commit: `app(android): qualify the store release candidate`.

## C52 — Qualify native iOS and TestFlight

macOS framework/host archive, signing/privacy/capabilities, real iPhone local
workflow, StoreKit purchase/restore, VoiceOver, permissions, background/kill
and independent PDF readers. Record Xcode/iOS/device/versions. Linux skip
never closes this gate; fix qualified native failures before submission.
Commit: `app(ios): qualify the TestFlight release candidate`.

## C53 — Prepare policies, metadata and review access

Owner/privacy/accountant-approved terms, retention/price disclosures, support/
deletion URLs, App Privacy/Data safety/SDK manifests, age/content classification,
real screenshots, store product review and synthetic reviewer account/access.
Check live URLs and actual data collection; no compliance claims from tests.
Commit: `docs(release): prepare store and privacy submission evidence`.

## C54 — Run beta and user-owned acceptance

Execute the existing beta/usability/device/PDF gates plus paid-service scenarios.
Apply account-specific Play tester requirements; record actual feedback,
crashes, performance and open defects. Publish no fabricated tester sessions.
Owner records acceptance or fixes; incomplete required gate remains unverified.
Commit: `docs(beta): record commercial candidate acceptance evidence`.

## C55 — Stage an approved production launch

Prepare exact version, price/allowances, store mappings, rollback/kill-switch,
support/incident coverage and all gate evidence. Owner authorizes production
store publication. Use staged rollout, verified products and actual account
contracts. Push repo release evidence without bypassing dev->main review/CI.
Commit: `infra(release): stage the monthly hosted service launch`.

## C56 — Observe delivery and reconcile launch

Verify actual store-to-entitlement-to-hosted-use chain, alert routing,
refund/support/recovery handling and early cost/usage against approved plan.
No unsolicited real-money test purchase; use authorized accounts/procedure.
Record issues and independently verified fixes, then close initial release
only when all mandatory gates have evidence and owner acceptance.
Commit: `docs(release): record commercial rollout verification`.

Exit: the individual monthly service is qualified and actually approved for
release; code completion alone cannot claim this outcome. Phase 8 remains
tracked if the requested extended product scope has not been delivered.
