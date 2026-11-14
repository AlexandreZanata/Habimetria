# C07 Execution — Scoped Administration and Audit

Started 2026-11-11 at `f2a5cef`, branch dev. Scope C07 only; C08-C64 pending.
Ordered sub-slices follow [commit breakdown](commit_breakdown.md).

## C07.a — Capability registry and guards

Defined [authorization](../../04-backend/admin_authorization.md) before code.
Registered the existing four and eleven commercial capabilities, with closed
support grants, account-write/read dependency and duplicate rejection.
Active owners retain known powers only; unknown IDs, inactive accounts and
ordinary users fail closed. Admin/support require explicit current grants.
Migration 0007 preserves existing role grants; no new commercial power is seeded.
Service/store checks and SQL constraints enforce the same matrix. Settings
forms render allowed registry options with Portuguese labels. Commercial powers
do not publish future services or imply private Report access.

Executed with disposable PostgreSQL 18.6-alpine:

- `GOTOOLCHAIN=go1.25.13 make verify test-integration cover` with
  TEST_DATABASE_URL: PASS, including schema/registry parity, migration replay,
  support escalation denial, explicit current grants and Report isolation.
- `go test -race -cover ./internal/authz`: PASS, 100% statement coverage;
  enforced a new 100% package floor. Registry unit tests exercise known/unknown, explicit/implicit, inactive/user,
  dependency, duplicate and defensive-copy behavior.
- `make secrets`, diff, new-file reference and changed-file line checks: PASS.

Commit `3890a2d985b8e1912c6ddf59d1d8c0aaff63a21d` was pushed to origin/dev;
[CI run 369](https://github.com/AlexandreZanata/Habimetria/actions/runs/43042660243)
passed backend and mobile-linux before C07.b began.

## C07.b — Reasoned audit and concurrent authority

Added [reasoned mutation contract](../../04-backend/admin_authorization.md),
closed reason choices for account/role changes, and action-derived reasons for
invitations/pending-link reissues. Store validation independently enforces
reasons. Every successful policy transition atomically records bounded typed
prior/result states, actor/target, server instant and a unique server-generated
operation UUID. No-op saves do not duplicate changes. Activity displays this
metadata; sign-in/profile events keep their existing minimal contracts.

Migration 0008 preserves historical fields and leaves unavailable metadata
NULL. It restricts new policy details/reasons/roles/grants and protects audit
rows against normal update/delete/truncate. Privacy inventory records the
minimized metadata without inventing retention periods or granting legal holds.
No customer content, names/emails, credentials, links or free text are copied.

Real PostgreSQL tests observe lock queues before releasing conflicting writes.
Capability revocation and owner demotion deny queued stale-authority mutations;
concurrent primary-owner mutations and direct SQL cannot remove its protection.
Injected audit insertion failure rolls back both policy state and session
revocation. New tests also cover legacy migration/replay, immutable audit,
closed fields, unique operation IDs, missing/invalid reasons, no-op saves,
empty grants, activation replacement and rendered activity with credential
exclusion. Existing Report/privacy/local boundaries remain in regression suites.

Executed with disposable PostgreSQL 18.6-alpine:

- `GOTOOLCHAIN=go1.25.13 make verify test-integration cover` with
  TEST_DATABASE_URL: PASS, including unchanged coverage floors and authz 100%.
- `go test -race ./internal/authz ./internal/store/postgres ./internal/httpapi/panelweb`:
  PASS. Additional reason/empty-grant/activation checks pass on the final tree.
- `make secrets`, diff checks, document links/new-file references and changed
  file line limits: PASS.

Final commit/ref and exact-commit CI are checked after push; no self-hash is
invented here. C08-C64 remain pending.

## Manual gates

G01-G12 remain UNVERIFIED. No production commercial action, store integration,
MFA/re-authentication or owner acceptance is approved by this slice.
