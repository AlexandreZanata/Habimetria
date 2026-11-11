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
the 200-line changed-file limit passed. Exact commit/push/CI references are
recorded in the next update; no integration pass is claimed by skipped tests.

## C02.b — Pending

Bind explicit route policy catalogs to registration, deny private routes
without an authorization guard, reject unclassified routes and verify all
current owner scopes with two-User integration tests. Preserve account/public
recipient access and existing Report-specific availability rules.

## Manual gates

No native device, PDF, user-session, legal or store gate is approved here.
