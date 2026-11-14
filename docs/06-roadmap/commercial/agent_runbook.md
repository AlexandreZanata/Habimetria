# Agent Runbook — One Commercial Slice at a Time

Authority: AGENTS.md and [plan](README.md). This is an execution instruction,
not authorization to fabricate approvals or change the business model.

## Start or resume

1. Read AGENTS.md, the product contract, current phase, progress record,
   [commit breakdown](commit_breakdown.md) and [code map](implementation_map.md).
2. Inspect branch/status/remote and diff. Work only on dev. Fetch and compare
   upstream; fast-forward only when safe. Preserve unrelated changes and
   never reset/stash them blindly. Record the actual starting commit.
3. Inspect relevant current code and docs before choosing the next slice.
   Allocate migrations from HEAD. Check completed evidence before repeating work.
4. Check dependencies and external gates. Continue independent implementation
   while missing credentials/devices/legal inputs remain explicitly unverified.
   Never substitute fake accounts, zero cost or mock provider approvals.
5. Execute the smallest pending slice; split if multiple concepts grow large.

## Per-slice protocol

- Explain the concrete outcome and relevant uncertainty to the user.
- Add only needed schema/service/API/UI changes. No dead controls, TODOs,
  fake production data or localStorage authority. Test doubles belong in tests.
- Respect Go handler -> service -> store, KMP feature-first adapters,
  PostgreSQL 18.6-alpine and the current brand/design system.
- Preserve free local behavior, ownership, append-only paid facts and SEALED.
- Add meaningful tests for grants, billing, ownership, storage and failure
  recovery. Do not mirror implementation with trivial assertions.
- Update affected docs and folder README references. Keep each file around
  200 lines maximum, one concept per file. English code/docs; UI copy pt-BR.
- Run `make verify`; run applicable checks below and fix real failures.
  An unconfigured integration suite that skips is not a database pass.
- Inspect `git diff --check`, staged diff, secrets and status. Stage only
  intended files, including the slice's progress/evidence update.
- Commit `type(scope): verb object`, push that commit to origin/dev and
  verify the remote contains it. Do not finish a slice uncommitted/unpushed.
- Inspect CI on that exact pushed commit. A queue/running job is not green.
  Record the run; fix failures in another small commit before advancing.

## Checks selected by impact

- All slices: `make verify`, `make secrets`, doc links/line limits and diff check.
- DB/auth/commerce/cloud: `TEST_DATABASE_URL=<disposable-pg18.6> make test-integration`;
  test migrations and concurrency on real DB, not mocked SQL alone.
- Mobile/shared code: `make verify-mobile`; platform tests as applicable.
- iOS host/StoreKit: macOS `make verify-ios-macos` plus native archive/tests;
  document toolchain and device. Linux compilation is a separate check.
- Storage/sync: real private S3-compatible integration, process interruption,
  corrupt bytes/full disk and clean-device snapshot restore.
- Purchase code: provider sandbox/license tests and event/reconciliation tests.
- Price code: deterministic currency/units/formula tests plus store readback.
- UI: actual relevant screens, keyboard/accessibility/large fonts; meaningful
  device validation is recorded without claiming owner acceptance.
- Release: full [acceptance](acceptance.md), [gates](release_gates.md),
  applicable existing benchmarks/coverage/fixture verification.

These commands are existing checks unless a phase explicitly introduces a
new harness. Never record an imagined command as executed. If unavailable,
record tool/dependency, exact failure and manual owner gate separately.

## Progress entry

For each slice record ID, scope/sub-slice, date, starting commit, implementation
status, commit SHA, pushed ref, checks with commands/results/artifact references,
CI run and outstanding external/manual gates. Add final commit SHA in the next
progress update if writing it inside its own commit would be circular.
Avoid real customer photos, CPF, address, token or keys in evidence.
Use synthetic fixtures and redacted logs; committed documentation has no secrets.

## Pull requests and main

After a coherent small group, maintain one small English dev->main PR.
Describe final behavior, scope and executed validation; list manual gates as
unverified. Required backend/mobile CI must be green for the proposed head.
Do not invent another feature branch or bypass branch protection. Main merges
remain subject to repository review; never force push, delete protections or
merge a failing PR. Check for an existing PR before creating another.

## Blocking and release discipline

Failed verification: fix before slice completion. Unavailable store/production
credential: keep dependent activation disabled; finish independent tests/docs.
Missing price/retention/provider decision: prepare a concrete admin configuration
or decision record; ask only for the missing decision, not repeated permission
for already-authorized repo work. No silent defaults for financial/legal inputs.

Never upload a production build, change a live store price, charge a user or
perform irreversible disposal based only on this plan document. Prepare the
reviewable release and obtain the applicable owner-controlled release gate.
Keep every free local path working even when connected rollout is disabled.

## Suggested execution prompt

“Read AGENTS.md and docs/06-roadmap/commercial/README.md. Resume the first
pending eligible slice from progress.md. Implement only that slice and its
necessary tests/docs, keep all on-device features free, and verify meaningful
failure cases. Run required checks, commit and push to dev, inspect exact-head
CI, and report the delivered commit and remaining manual gates. Continue to
the next eligible small slice without fabricating any external approval.”
