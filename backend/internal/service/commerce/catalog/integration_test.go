package catalog_test

import (
	"context"
	"errors"
	"math"
	"reflect"
	"testing"
	"time"

	"vault/internal/service/commerce/catalog"
	"vault/internal/store/postgres"
	"vault/internal/testutil"
	"vault/migrations"

	"github.com/jackc/pgx/v5/pgxpool"
)

func fixtureCatalog(t *testing.T) (context.Context, *pgxpool.Pool, *catalog.Service) {
	t.Helper()
	ctx, pool := testutil.Database(t)
	if err := postgres.Apply(ctx, pool, migrations.FS); err != nil {
		t.Fatal(err)
	}
	return ctx, pool, catalog.New(postgres.NewServicePlanStore(pool))
}

func TestCatalogRoundTripIntegration(t *testing.T) {
	ctx, pool, s := fixtureCatalog(t)
	var before, after time.Time
	if err := pool.QueryRow(ctx, `SELECT clock_timestamp()`).Scan(&before); err != nil {
		t.Fatal(err)
	}
	plan, err := s.CreatePlan(ctx, "synthetic-roundtrip")
	if err != nil {
		t.Fatal(err)
	}
	terms := fixtureTerms()
	version, err := s.AppendVersion(ctx, plan.ID, terms)
	if err != nil {
		t.Fatal(err)
	}
	if err := pool.QueryRow(ctx, `SELECT clock_timestamp()`).Scan(&after); err != nil {
		t.Fatal(err)
	}
	if version.Number != 1 || version.PlanID != plan.ID || version.CreatedAt.Before(before) || version.CreatedAt.After(after) || plan.CreatedAt.Before(before) {
		t.Fatal("invalid identity or non-server creation instant")
	}
	terms.Allowances[0].Limit++ // Caller mutation cannot edit stored rights.
	stored, err := s.Version(ctx, plan.ID, 1)
	if err != nil {
		t.Fatal(err)
	}
	if !reflect.DeepEqual(version, stored) {
		t.Fatal("stored version changed after input mutation")
	}
	if _, err := s.CreatePlan(ctx, plan.Code); !errors.Is(err, postgres.ErrServicePlanConflict) {
		t.Fatalf("duplicate plan: %v", err)
	}
	for range 2 {
		if _, err := s.AppendVersion(ctx, plan.ID, fixtureTerms()); err != nil {
			t.Fatal(err)
		}
	}
	page, err := s.Versions(ctx, plan.ID, 0, 2)
	if err != nil {
		t.Fatal(err)
	}
	if len(page) != 2 || page[0].Number != 1 || page[1].Number != 2 {
		t.Fatal("unstable version page")
	}
	page, err = s.Versions(ctx, plan.ID, page[1].Number, 2)
	if err != nil {
		t.Fatal(err)
	}
	if len(page) != 1 || page[0].Number != 3 {
		t.Fatal("keyset cursor skipped/duplicated history")
	}
	empty, err := s.Versions(ctx, plan.ID, 3, 2)
	if err != nil || len(empty) != 0 {
		t.Fatal("bad empty page")
	}
	large := fixtureTerms()
	large.Allowances[0].Limit = math.MaxInt64
	exact, err := s.AppendVersion(ctx, plan.ID, large)
	if err != nil {
		t.Fatal(err)
	}
	exact, err = s.Version(ctx, plan.ID, exact.Number)
	if err != nil || exact.Allowances[0].Limit != math.MaxInt64 {
		t.Fatal("integer allowance lost precision")
	}
	retired, err := s.RetirePlan(ctx, plan.ID)
	if err != nil {
		t.Fatal(err)
	}
	again, err := s.RetirePlan(ctx, plan.ID)
	if err != nil {
		t.Fatal(err)
	}
	if retired.RetiredAt == nil || !reflect.DeepEqual(retired, again) {
		t.Fatal("retirement is not terminal/idempotent")
	}
	if _, err := s.AppendVersion(ctx, plan.ID, fixtureTerms()); !errors.Is(err, postgres.ErrServicePlanRetired) {
		t.Fatalf("retired plan accepted new terms: %v", err)
	}
	stored, err = s.Version(ctx, plan.ID, 1)
	if err != nil || !reflect.DeepEqual(version, stored) {
		t.Fatal("retirement changed purchased history")
	}
}

func TestCatalogFailuresIntegration(t *testing.T) {
	ctx, pool, s := fixtureCatalog(t)
	missing := testutil.ID(9)
	checkMissing := func(err error) {
		t.Helper()
		if !errors.Is(err, postgres.ErrNotFound) {
			t.Fatalf("missing catalog identity: %v", err)
		}
	}
	_, err := s.Plan(ctx, missing)
	checkMissing(err)
	_, err = s.AppendVersion(ctx, missing, fixtureTerms())
	checkMissing(err)
	_, err = s.Version(ctx, missing, 1)
	checkMissing(err)
	_, err = s.Versions(ctx, missing, 0, 10)
	checkMissing(err)
	_, err = s.RetirePlan(ctx, missing)
	checkMissing(err)
	plan, err := s.CreatePlan(ctx, "failure-fixture")
	if err != nil {
		t.Fatal(err)
	}
	// Direct store callers still cannot persist a local paid capability.
	bad := fixtureTerms()
	bad.Allowances[0].Capability = "local_pdf"
	if _, err := postgres.NewServicePlanStore(pool).AppendVersion(ctx, plan.ID, bad); !errors.Is(err, postgres.ErrServicePlanInvalid) {
		t.Fatalf("DB allowed invalid paid terms: %v", err)
	}
	versions, err := s.Versions(ctx, plan.ID, 0, 10)
	if err != nil || len(versions) != 0 {
		t.Fatal("failed append left partial history")
	}
	valid, err := s.AppendVersion(ctx, plan.ID, fixtureTerms())
	if err != nil || valid.Number != 1 {
		t.Fatal("failed append consumed version number")
	}
	pool.Close()
	if _, err := s.Plan(ctx, plan.ID); err == nil || errors.Is(err, postgres.ErrNotFound) {
		t.Fatal("DB outage became empty/not-found catalog")
	}
	if _, err := s.CreatePlan(ctx, "unavailable-fixture"); err == nil {
		t.Fatal("create masked DB outage")
	}
	if _, err := s.AppendVersion(ctx, plan.ID, fixtureTerms()); err == nil {
		t.Fatal("append masked DB outage")
	}
	if _, err := s.Version(ctx, plan.ID, 1); err == nil {
		t.Fatal("version masked DB outage")
	}
	if _, err := s.Versions(ctx, plan.ID, 0, 10); err == nil {
		t.Fatal("history masked DB outage")
	}
	if _, err := s.RetirePlan(ctx, plan.ID); err == nil {
		t.Fatal("retirement masked DB outage")
	}
}
