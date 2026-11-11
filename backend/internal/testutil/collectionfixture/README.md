# Synthetic collection fixtures

- `fixture.go`: creates explicit owned/unclaimed Properties, DRAFT/SEALED
  Inspections, Room/Evidence/Finding, Manifest, Report and Occurrence through
  real core services; only test ownership setup uses direct SQL. Synthetic
  device-time claim stays separate from database/server instants. IDs are fresh.
- `snapshot.go`: captures all fixture core rows and private object digests for
  immutable-history checks, without logging payloads or credentials.
- `migration_integration_test.go`: upgrades schema 0004 with two owners and
  unclaimed data; pins unchanged bytes/rows, no enrollment/authority/catalog
  seed, migration replay/rollback and independent-database isolation.

Every generated object key has bounded cleanup, even after partial failure.
Use only with isolated test databases and storage; there is no production
seeding command. `make verify-infra` provisions the required dependencies.
Normative [contract](../../../../docs/04-backend/integration_fixtures.md).
