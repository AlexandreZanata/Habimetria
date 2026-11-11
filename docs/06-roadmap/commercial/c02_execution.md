# C02 Execution — Hosted Entry Point Classification

Started 2026-11-11 at `7f992cc`, branch dev. Scope C02 only, ordered sub-slices
from [commit breakdown](commit_breakdown.md). C03-C64 remain pending.

## C02.a — Inventory

Inspected API registration/legacy guards, panel routes/services/documents,
VaultApi, UploadPump, ServerMirror, Android upload scheduling and platform
LOCAL_ONLY wiring. Recorded concrete surfaces, current enforcement,
control/hosted/recipient classifications and later gaps in
[hosted entry points](../../03-architecture/hosted_entry_points.md).

Existing anonymous unclaimed-resource migration remains C04; subscription
and quota enforcement remain later work. This inventory does not claim
payments, complete authenticated sync, future workers or store approval.

`make verify`, `make secrets`, diff checks, internal documentation links and
the 200-line changed-file limit passed. Commit `4fff8e1` was pushed to dev;
[CI run 349](https://github.com/AlexandreZanata/Habimetria/actions/runs/43020265618)
passed backend/mobile-linux. Skipped DB tests were not counted as integration.

## C02.b — Classified registration and isolation

- Added shared `httpapi/routepolicy` router, API catalog and explicit panel
  catalog. No registration without a reviewed rule; no private handler without
  a working guard. Public controls/recipients retain their deliberate boundary.
- Panel sections no longer use generic GET/POST wildcard dispatch. All 23
  concrete operations have policies; private session checks reuse one verified
  actor per request while service RBAC/ownership/Report grants stay authoritative.
- API public allowlist and all 24 owner routes/scopes are tested. Guard absence
  and nil-returning guard deny access. HEAD and policy-inspection mutations
  cannot bypass authorization.
- Real-DB tests use distinct customer accounts plus staff: every owner route
  rejects anonymous/other/staff access; owner GET paths work. Body/query owner
  claims cannot override path ownership. Panel account/profile/logout and
  authorized recipient verification/share/PDF paths remain available.
- Added a 95% coverage floor for the new security-boundary package; measured
  unit coverage was 97.5%. Existing coverage floors are unchanged.

Executed checks:

- `TEST_DATABASE_URL=<isolated PostgreSQL 18.6-alpine> make verify`: PASS,
  including new and previous integration suites.
- `TEST_DATABASE_URL=<isolated DB> GOTOOLCHAIN=go1.25.13 make cover`: PASS;
  all existing floors retained and new package floor 95% enforced.
- `go test -race ./internal/httpapi ./internal/httpapi/routepolicy
  ./internal/httpapi/panelweb` with that DB: PASS, including concurrent and
  two-customer authorization checks.
- `make secrets`, diff checks, new-file references, local documentation links
  and changed-file limits: PASS.

This delivery does not change local functionality, sealed bytes, the legacy
unclaimed-resource policy (C04), mobile authentication or future paid rights.
Commit identity/CI for this sub-slice are checked through Git/PR after push;
no self-referential hash or pending CI result is fabricated.

## Manual gates

No native device, PDF, user-session, legal or store gate is approved here.
