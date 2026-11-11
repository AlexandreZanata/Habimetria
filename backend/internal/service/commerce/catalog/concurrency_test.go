package catalog_test

import (
	"errors"
	"strconv"
	"testing"

	"vault/internal/store/postgres"
)

func TestConcurrentVersionAllocationIntegration(t *testing.T) {
	ctx, _, s := fixtureCatalog(t)
	plan, err := s.CreatePlan(ctx, "concurrent-fixture")
	if err != nil {
		t.Fatal(err)
	}
	start := make(chan struct{})
	type result struct {
		version postgres.ServicePlanVersion
		err     error
	}
	results := make(chan result, 8)
	for range 8 {
		go func() {
			<-start
			version, err := s.AppendVersion(ctx, plan.ID, fixtureTerms())
			results <- result{version, err}
		}()
	}
	close(start)
	numbers, ids := map[int]bool{}, map[string]bool{}
	for range 8 {
		r := <-results
		if r.err != nil {
			t.Fatal(r.err)
		}
		if numbers[r.version.Number] || ids[r.version.ID] {
			t.Fatal("duplicate concurrent version")
		}
		numbers[r.version.Number], ids[r.version.ID] = true, true
	}
	for n := 1; n <= 8; n++ {
		if !numbers[n] {
			t.Fatal("concurrent allocation skipped committed history")
		}
	}
	versions, err := s.Versions(ctx, plan.ID, 0, 100)
	if err != nil || len(versions) != 8 {
		t.Fatal("lost concurrent version")
	}
}

func TestAppendAndRetirementSerializeIntegration(t *testing.T) {
	ctx, _, s := fixtureCatalog(t)
	for n := range 8 {
		plan, err := s.CreatePlan(ctx, "retirement-race-"+strconv.Itoa(n))
		if err != nil {
			t.Fatal(err)
		}
		start := make(chan struct{})
		appendResult, retireResult := make(chan error, 1), make(chan error, 1)
		go func() { <-start; _, err := s.AppendVersion(ctx, plan.ID, fixtureTerms()); appendResult <- err }()
		go func() { <-start; _, err := s.RetirePlan(ctx, plan.ID); retireResult <- err }()
		close(start)
		appendErr, retireErr := <-appendResult, <-retireResult
		if retireErr != nil {
			t.Fatal(retireErr)
		}
		if appendErr != nil && !errors.Is(appendErr, postgres.ErrServicePlanRetired) {
			t.Fatal(appendErr)
		}
		versions, err := s.Versions(ctx, plan.ID, 0, 100)
		if err != nil {
			t.Fatal(err)
		}
		want := 1
		if appendErr != nil {
			want = 0
		}
		if len(versions) != want {
			t.Fatal("append/retirement lost or partially committed terms")
		}
		if _, err := s.AppendVersion(ctx, plan.ID, fixtureTerms()); !errors.Is(err, postgres.ErrServicePlanRetired) {
			t.Fatal("terminal retirement reopened")
		}
	}
}
