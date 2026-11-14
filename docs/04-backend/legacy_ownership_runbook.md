# Legacy Ownership — C04 Operator Runbook

Authority: [web panel](web_panel.md), [privacy](../03-architecture/commercial_privacy.md)
and [C04 evidence](../06-roadmap/commercial/c04_execution.md).
This is a reviewed operator procedure, not an automatic startup migration or
a customer assignment endpoint. No production assignment was executed by C04.

## Runtime modes and rollback

`LEGACY_API_MODE=owned` is the default in Config, Compose and the env example.
Every existing private API resource resolves its Property owner in PostgreSQL.
No owner means 404; anonymous legacy Property creation is 403. The panel is
currently the authenticated owned-Property creation entry point. Turning off
PANEL_ORIGIN never grants access to unclaimed or owned collections. Owner
writes require the configured exact Origin; disabling the panel blocks those
session-authenticated writes rather than inventing a trusted origin.

For isolated throwaway legacy development, set APP_ENV=development,
VERIFY_BASE_URL=http://localhost:8080, PANEL_ORIGIN empty and
LEGACY_API_MODE=unclaimed-development. Bind/firewall that environment locally;
do not put this exception behind a public proxy or use production data.
Production, HTTPS/non-loopback advertised origins and panel-enabled exceptions
are rejected at startup. The exception never bypasses existing ownership.
LocalSession/on-device functionality needs none of these server settings.

Rollback exposure by keeping `owned` and disabling the panel if necessary.
Retain migrations/ownership and immutable objects. Do not restore an older
binary that infers anonymous access from an absent panel. Keep HMAC keys and
historical manifests unchanged. Public `/v` and deliberate `/s` capabilities
retain their minimized behavior, including already-issued Shares.

## Inventory and approval

1. Back up DB plus immutable objects and confirm the recovery plan. Apply
   existing migrations through 0005; C04 introduces no schema migration.
2. In a restricted operator session inventory NULL-owner Properties and their
   dependent Tenancies, Inspections, Reports and comparison references. Never
   assign every legacy row to the bootstrap owner, first account or caller.
3. Independently establish the titleholder and exact existing activated User.
   A guessed UUID/email, staff role, payment or mirror link is not proof.
   Obtain an approved change record identifying operator, Property, target User,
   verification method and reason; keep unnecessary PII/photos out of it.
4. Capture an integrity inventory: IDs, counts, sealed status/server instants,
   Manifest hashes/signatures, original object hashes/keys and finalized PDF
   hashes/keys. Verify actual retained bytes against those references. Keep
   sensitive inventory restricted, outside public CI/support activity.
5. Do not proceed with ambiguous ownership, missing bytes or existing staff
   grants on an unclaimed collection. Resolve those separately with documented
   titleholder authority. Unresolved records remain unavailable and preserved
   under the applicable retention/rights process, not exposed or deleted.

## One reviewed Property per transaction

Use psql with ON_ERROR_STOP enabled and three explicitly reviewed UUID variables:
`property_id`, `titleholder_id` and `operator_id`. Never interpolate shell text
into SQL. The quoted psql variables below are SQL literals, cast to UUID.

```sql
BEGIN;
SELECT id, owner_user_id FROM properties
WHERE id = :'property_id'::uuid FOR UPDATE;
SELECT id, panel_role, panel_active FROM users
WHERE id IN (:'titleholder_id'::uuid, :'operator_id'::uuid) FOR SHARE;
WITH assigned AS (
  UPDATE properties p SET owner_user_id = u.id
  FROM users u, users a
  WHERE p.id = :'property_id'::uuid AND p.owner_user_id IS NULL
    AND u.id = :'titleholder_id'::uuid AND u.panel_active
    AND u.panel_password IS NOT NULL
    AND a.id = :'operator_id'::uuid AND a.panel_active AND a.panel_role = 'owner'
    AND NOT EXISTS (
      SELECT 1 FROM panel_report_access g JOIN reports r ON r.id=g.report_id
      JOIN inspections i ON i.id=r.inspection_id
      JOIN tenancies t ON t.id=i.tenancy_id WHERE t.property_id=p.id)
  RETURNING p.id
)
INSERT INTO panel_audit(actor_id,event,subject_id)
SELECT :'operator_id'::uuid, 'legacy_property_assigned', id FROM assigned
RETURNING id, subject_id, occurred_at;
```

Require exactly one returned audit row. Zero rows, an unexpected prior owner,
failed assertion or integrity difference means ROLLBACK; never broaden the
WHERE clause. Check the integrity inventory again within the transaction.
Only then COMMIT. Bind the audit ID and reviewed target User to the approved
change record. Existing audit has no reason field; C07 extends reasoned
administration. Do not encode personal data or secrets in the event string.

After commit verify titleholder access, another User/staff denial, grant/revoke
controls, unchanged Report bytes and public integrity results. Re-running the
NULL-only assignment must change zero rows. Never repair a wrong assignment
by clearing all owners or rewriting a SEALED Inspection; contain exposure and
use a separately reviewed corrective transaction.

## Invalid comparisons, sessions and jobs

Comparison Evidence must belong to the selected entry/exit Room and Inspection.
Unknown, cross-owner and wrong-side references return 404. Legacy invalid rows
also fail scoped reads. Inventory and review their mutable comparison tags;
do not rewrite Evidence, Manifest or PDF to make a bad reference fit.

Role/capability, suspension, logout and Report-grant revocation apply on the
next request. Restoring an account does not resurrect an invalidated session.
No staff grant opens the owner's private resource API. Already-delivered bytes
cannot be recalled; explicit Share tokens remain separate capabilities.

There is no active Go background-job executable. Current mobile upload/mirror
workers use the same owner-guarded HTTP API; worker/User headers are not
credentials. No cookie/expired session means no owned-resource access.
Do not add a service-wide ownership bypass for future jobs. Session/enrollment
and scoped worker subjects belong to their later planned slices.
