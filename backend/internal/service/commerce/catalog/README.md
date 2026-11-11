# Internal monthly ServicePlan catalog

[Contract](../../../../../docs/04-backend/service_plan_catalog.md).
No public/admin route or sale enablement is introduced by this package.

- [validation.go](validation.go): monthly individual terms, hosted-only registry,
  finite allowances, exact units/windows and stable identifiers.
- [service.go](service.go): validated internal store port and historical queries.
- [ServicePlan types](../../../store/postgres/service_plan_types.go).
- [Plan persistence](../../../store/postgres/service_plans.go): creation/read and
  terminal retirement with stable identity.
- [Version persistence](../../../store/postgres/service_plan_versions.go): atomic
  version allocation, immutable terms and bounded historical pagination.
- [validation_test.go](validation_test.go): local/required-control exclusion,
  monthly units/windows, finite integers and pre-persistence input bounds.
- [integration_test.go](integration_test.go): real PostgreSQL roundtrip, immutable
  rights after caller edits/retirement, pagination and failure rollback/outages.
- [concurrency_test.go](concurrency_test.go): simultaneous version allocation and
  retirement/append serialization.
- [compatibility_test.go](compatibility_test.go): newer formats/unknown JSON fail
  closed without partial history; known old versions remain readable.
