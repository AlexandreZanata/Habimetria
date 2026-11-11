# Phase 1 — Free Local and Hosted Boundaries

Depends on approved [product contract](../../02-product/service_monetization.md).
Apply [runbook](agent_runbook.md) to every slice; each includes tests/docs,
`make verify`, commit, push and exact-commit CI. No production sale in this phase.

## C01 — Remove seal/payment coupling

Read `service/seal.go`, `payment*.go`, config/wiring and callers. Remove the
per-seal proof gate, deprecate obsolete config/default price safely, and
document compatibility of old proof fields. Hosted server consumption will
be protected by entitlement at the API/service boundary, not by buying a seal.
Test local seal independent of payment and retain terminality/race tests.
Commit: `fix(commerce): decouple sealing from payment proofs`.

## C02 — Inventory and classify all hosted entry points

Trace routes, panel reads, UploadPump, ServerMirror, public verify/share and
background jobs. Add a route/capability classification with ownership and
paid/recovery/public-control categories. Fail closed for hosted resources;
do not blanket-block required account controls or public verification.
Test a route allowlist and two-User isolation on existing private paths.
Commit: `docs(commerce): map hosted authorization boundaries`.

## C03 — Protect the complete free journey

Add a reusable synthetic never-registered/expired-account local scenario:
Property, Rooms, capture bytes, findings, seal, PDF, import/export, verify,
comparison, assistant and declarations. Assert no billing/network call.
Keep safety bounds without subscription caps. Reuse actual local repositories.
Run shared/mobile checks and compare immutable fixture hashes.
Commit: `app(local): preserve the complete free workflow`.

## C04 — Close legacy ownership bypasses

Migrate private legacy route guards and anonymous/unclaimed production policy
without claiming existing rows for an arbitrary User. Preserve documented
development/local mode and explicit public routes; add a migration/runbook
for any owned production records. Test IDOR, role revocation and job scopes.
Commit: `fix(auth): enforce ownership across hosted routes`.

## C05 — Add the monthly capability catalog

Introduce small catalog schema/types/service with immutable versions and
explicit hosted capabilities/allowances. Reject any local paid capability,
nonmonthly launch product or unbounded/ambiguous unit. No retail amount seed.
Test migration replay, version immutability and catalog validation.
Commit: `db(commerce): add versioned monthly service plans`.

## C06 — Add safe commercial configuration

Define configured app/store/environment identities, secret references and
disabled-by-default purchase exposure. Reject sandbox/production mixing and
missing mandatory configured values at feature activation, not local startup.
Update env example/parity tests and operational migration instructions.
Commit: `infra(commerce): validate store environment configuration`.

## C07 — Extend scoped permissions and audit

Register commercial/finance/support/privacy/replay capabilities against the
existing matrix. Protect primary owner; no grant implies private Report access.
Add reasoned audit for mutations and ensure secrets/PII are excluded.
Test support escalation denial and concurrent owner/role changes on real DB.
Commit: `feat(admin): scope commercial permissions and audit`.

## C08 — Qualify infrastructure and migration fixtures

Provide disposable PostgreSQL 18.6, private object storage and mail-test setup,
synthetic multi-owner/legacy fixtures and documented verification commands.
Verify configuration isolation and reliable cleanup; never use real customer data.
Update baseline/progress with executed evidence, not current delivery assumptions.
Commit: `infra(test): qualify commercial integration fixtures`.

Exit: free journey and private-route invariants pass. Account/commerce/cloud
schemas remain incremental; do not build all future migrations here.
