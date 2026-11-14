# Scoped Administrative Authorization

C07 extends the existing panel matrix; it does not publish commercial services.
See [administration](commercial_admin.md), [web panel](web_panel.md) and
[privacy](../03-architecture/commercial_privacy.md).

## Registry and grants

The closed `backend/internal/authz` registry contains existing `accounts.read`,
`accounts.write`, `reports.read`, `audit.read` and the commercial capabilities
listed in commercial_admin.md. Unknown capabilities fail closed, including for
owners. Active owners have known capabilities; admin/support require explicit
current role grants; user and inactive accounts have no staff capabilities.
No migration automatically assigns new capabilities to existing roles.

Support may receive only `accounts.read`, `reports.read`, `audit.read`,
`commerce.read`, `support.write`. Admin may receive any registered capability.
Account writes require account reads. Duplicate/unknown grants are invalid.
Only active owners edit this matrix. Current primary-owner protection and
session invalidation remain mandatory; commercial powers never authorize
account role promotion or credential reset. Report bytes still require BOTH
`reports.read` and an exact titleholder availability grant. No commercial,
finance, privacy or operational capability implies collection access.

The service and store validate the matrix; PostgreSQL constrains the same
allowlist, support subset and dependency. Integration tests pin registry/schema
parity and preserve existing grants during migration. Guards use current
database authority on every request; administrative writes re-read authority
inside the serialized transaction. Future services must guard their specific
capability there too; assigning a capability does not create an action/endpoint.
Staff MFA and high-risk reauthentication remain C14 work before paid release.

## Reasoned mutations

Account role/state and permission-matrix changes require a closed reason:
`access_review`, `security_response` or `account_request`. Forms offer labelled
choices without a free-text field. Missing/unknown reasons fail before mutation.
Invitations and pending-link reissues use the explicit action-derived reasons
`account_onboarding` and `activation_delivery`. No-op saves emit no change.

The same transaction records actor/subject IDs (or target role), typed before/
after role, active state or permissions, a server-generated unique operation
UUID and server instant. No email, display name, password/digest, token/link,
photo, Report content or arbitrary JSON/text is copied into these audit fields.
Current display names in the activity view are resolved from accounts, not
duplicated into historical payloads. Activity shows reason, target, transition
and operation identity. Failed audit insertion rolls back the mutation and
session revocation. Denied attempts are not represented as successful changes.

Migration 0008 leaves historical rows' new fields NULL; do not fabricate old
reasons/operations. New policy events require complete typed metadata. Database
checks bound reasons/roles/permissions. Existing sign-in/profile and reviewed
legacy-assignment events retain their separate minimal contracts. Audit rows
reject normal update/delete/truncate. This is integrity protection, not a
retention period or a legal-hold decision; approved restricted retention/erasure
operations remain subject to the future privacy/operational workflow.

The common administrative lock precedes authority/target reads and audit writes.
Real-DB tests queue actual revocation/demotion and stale actor operations to
prove serial authority, primary-owner survival and atomic audit rollback.

## Files and evidence

- [Registry and tests](../../backend/internal/authz/README.md).
- `backend/migrations/0007_panel_capabilities.sql` extends constraints only;
  rollback restores the prior subset only after removing incompatible grants.
- `backend/internal/store/postgres/panel_capabilities_test.go` checks schema
  parity, migration replay and no automatic commercial authority.
- `backend/internal/httpapi/panelweb/capabilities.go` adapts registry options to
  Portuguese labels in the existing settings form.
- `backend/internal/httpapi/panelweb/capabilities_integration_test.go` checks
  explicit grants, support escalation denial and Report isolation.
- [Execution record](../06-roadmap/commercial/c07_execution.md).
- `backend/migrations/0008_panel_policy_audit.sql` and PostgreSQL
  `panel_audit.go` persist bounded policy transitions; `panel_audit_test.go`
  checks schema validation and immutable rows.
- `backend/internal/httpapi/panelweb/audit.go`, `views/change_reason.html`
  and `audit_integration_test.go` render and verify actual reasoned forms/events.
- `backend/internal/store/postgres/panel_policy_fixture_test.go` supplies isolated
  actors and lock-queue observation. `panel_concurrency_test.go` exercises blocked
  writes against current authority; `panel_audit_rollback_test.go` injects an
  audit failure to verify all-or-nothing changes.
