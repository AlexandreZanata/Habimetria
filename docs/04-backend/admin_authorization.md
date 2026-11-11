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
