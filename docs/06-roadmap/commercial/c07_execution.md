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

Final commit/ref and exact-commit CI are checked after push; no self-hash is
invented. C07.b remains pending until its separate commit and checks.

## Manual gates

G01-G12 remain UNVERIFIED. No production commercial action, store integration,
MFA/re-authentication or owner acceptance is approved by this slice.
