# Database — PostgreSQL 18.6-alpine

Migrations: `backend/migrations/0001_init.sql` (MVP core) →
`0002_geo.sql` (device geolocation claims) →
`0003_seal.sql` (manifest vault + nullable report artifacts) →
`0004_comparisons.sql` (V1.3 entry-vs-exit tags) →
`0005_panel.sql` (web identity, sessions, permissions and availability) →
`0006_service_plans.sql` (immutable individual monthly hosted-service catalog) →
`0007_panel_capabilities.sql` (scoped commercial permission matrix) →
`0008_panel_policy_audit.sql` (reasoned, typed administrative transitions) →
`0009_account_registration.sql` (pending self-registration and encrypted mail enqueue) →
`0010_account_mail_leases.sql` (bounded durable delivery and guarded completion) →
`0011_account_verification_audit.sql` (minimal email ownership audit) →
`0012_account_access_sessions.sql` (short-lived API audience and invalidation) →
`0013_account_password_recovery.sql` (customer proof, invalidation and security mail) →
`0014_account_device_sessions.sql` ([native families and rotation](device_sessions.md)).
One version per file. See [web panel](web_panel.md).

Planned commercial extensions are specified separately in
[subscription storage](subscription_storage.md). The implemented catalog is
specified in [ServicePlan catalog](service_plan_catalog.md); other groups remain planned.

## Core tables

- `users(id, email CITEXT UNIQUE, created_at)`
- `properties(id, owner_user_id, address, created_at)`
- `tenancies(id, property_id, type, status, cap_photos, cap_videos, cap_bytes)`
- `inspections(id, tenancy_id, kind(entry|exit), status(DRAFT|SEALED), sealed_at, manifest_sha256, report_id)`
- `rooms(id, inspection_id, key, label, sort)`
- `evidences(id, inspection_id, room_id, type(photo|video|text|doc), mime, size, storage_key, sha256, captured_at_device, received_at_server, sequence)`
- `findings(id, room_id, evidence_id, preset, note)`
- `reports(id, inspection_id, verify_code UNIQUE, report_sha256, pdf_key, created_at)`
- `occurrences(id, property_id, title, body, created_at)` + `occurrence_evidences`
- `comparisons(id, tenancy_id, entry_room_id, exit_room_id, shot_category, entry_evidence_id?, exit_evidence_id?, tag, created_at)` + `UNIQUE(exit_room_id, shot_category)` (0004)
- `shares(id, report_id, channel, token UNIQUE, created_at)` + `access_logs(share_id, event, at, ip_hash)`

## Invariants (enforce in service + CHECK)

Catalog tables are separate from the core: `service_plans` owns stable identity
and terminal retirement; `service_plan_versions` owns immutable monthly terms,
explicit hosted allowances and historical version numbers. See the
[catalog contract](service_plan_catalog.md) for columns, formats and rollout.

- `SEALED` requires `sealed_at NOT NULL`, `manifest_sha256 NOT NULL`.
- No update/delete on `evidences` when parent inspection `SEALED`.
- NULL legacy Property ownership is not public access. Production uses the
  owner-only guard; reviewed assignment follows the [operator runbook](legacy_ownership_runbook.md).
- Room creation/template writes lock their Inspection and require DRAFT,
  serialized against seal. See [fixture qualification](integration_fixtures.md).
- `sequence` unique per inspection. Manifest ordered by it.
- `verify_code`: `nanoid(10)`, unique index.

C07 adds `0007_panel_capabilities.sql`: closed commercial capability constraints,
restricted support grants and duplicate rejection; existing grants are retained.
Contract and test inventory: [administrative authorization](admin_authorization.md).

Pending identity, verification digests, encrypted outbox and HMAC throttle keys
follow [registration](account_registration.md). Existing verification status
is not backfilled or inferred. No private collection or paid right is seeded.
Atomic ownership proof and shared credentials follow
[account authentication](account_authentication.md).
[Recovery](account_recovery.md) extends the same identity and outbox, including
recipient-purpose privacy inventory. Rollback must retain committed audit facts.
