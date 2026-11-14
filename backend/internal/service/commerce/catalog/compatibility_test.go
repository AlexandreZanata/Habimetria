package catalog_test

import (
	"errors"
	"testing"

	"vault/internal/service/commerce/catalog"
	"vault/internal/store/postgres"
)

func TestNewerCatalogFormatFailsClosedIntegration(t *testing.T) {
	ctx, pool, s := fixtureCatalog(t)
	plan, err := s.CreatePlan(ctx, "forward-format-fixture")
	if err != nil {
		t.Fatal(err)
	}
	if _, err := s.AppendVersion(ctx, plan.ID, fixtureTerms()); err != nil {
		t.Fatal(err)
	}
	// Simulate a later schema in this disposable database only. Old readers
	// must never silently drop new rights or treat an unknown format as v1.
	if _, err := pool.Exec(ctx, `ALTER TABLE service_plan_versions DROP CONSTRAINT service_plan_versions_format_version_check`); err != nil {
		t.Fatal(err)
	}
	newer := fixtureTerms()
	newer.FormatVersion = 2
	if _, err := postgres.NewServicePlanStore(pool).AppendVersion(ctx, plan.ID, newer); err != nil {
		t.Fatal(err)
	}
	if _, err := s.Version(ctx, plan.ID, 2); !errors.Is(err, catalog.ErrInvalid) {
		t.Fatal("unknown format was interpreted as current terms")
	}
	if rows, err := s.Versions(ctx, plan.ID, 0, 100); !errors.Is(err, catalog.ErrInvalid) || rows != nil {
		t.Fatal("history returned partially understood rights")
	}
	if _, err := s.Version(ctx, plan.ID, 1); err != nil {
		t.Fatal("old committed version became unreadable")
	}
	if _, err := pool.Exec(ctx, `ALTER TABLE service_plan_versions DROP CONSTRAINT service_plan_versions_allowances_check`); err != nil {
		t.Fatal(err)
	}
	if _, err := pool.Exec(ctx, `INSERT INTO service_plan_versions(plan_id,version_number,name,billing_period,subject_scope,format_version,allowances)
		VALUES($1,3,'Future fixture','monthly','individual',1,
		'[{"capability":"cloud_backup","unit":"bytes","window":"retained","limit":1,"unknown_meter":2}]')`, plan.ID); err != nil {
		t.Fatal(err)
	}
	if _, err := s.Version(ctx, plan.ID, 3); err == nil {
		t.Fatal("unknown JSON fields were silently ignored")
	}
	if rows, err := s.Versions(ctx, plan.ID, 2, 100); err == nil || rows != nil {
		t.Fatal("failed decoding exposed partial history")
	}
}
