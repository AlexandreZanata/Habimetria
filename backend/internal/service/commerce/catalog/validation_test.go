package catalog_test

import (
	"context"
	"errors"
	"math"
	"strings"
	"testing"

	"vault/internal/service/commerce/catalog"
	"vault/internal/store/postgres"
	"vault/internal/testutil"
)

func fixtureTerms() postgres.ServicePlanVersionInput {
	return postgres.ServicePlanVersionInput{Name: "Synthetic fixture", BillingPeriod: "monthly", SubjectScope: "individual", FormatVersion: 1,
		Allowances: []postgres.HostedAllowance{
			{Capability: "cloud_backup", Unit: "bytes", Window: "retained", Limit: 1024},
			{Capability: "cross_device_sync", Unit: "bytes", Window: "subscription_month", Limit: 2048},
			{Capability: "web_library", Unit: "requests", Window: "subscription_month", Limit: 3},
			{Capability: "hosted_share", Unit: "bytes", Window: "subscription_month", Limit: 4096},
		}}
}

func TestMonthlyHostedValidation(t *testing.T) {
	if err := catalog.ValidateVersion(fixtureTerms()); err != nil {
		t.Fatal(err)
	}
	for _, change := range []func(*postgres.ServicePlanVersionInput){
		func(v *postgres.ServicePlanVersionInput) { v.BillingPeriod = "annual" },
		func(v *postgres.ServicePlanVersionInput) { v.SubjectScope = "organization" },
		func(v *postgres.ServicePlanVersionInput) { v.FormatVersion = 2 },
		func(v *postgres.ServicePlanVersionInput) { v.Name = " " },
		func(v *postgres.ServicePlanVersionInput) { v.Name = "x" },
		func(v *postgres.ServicePlanVersionInput) { v.Name = " untrimmed" },
		func(v *postgres.ServicePlanVersionInput) { v.Name = "embedded\ncontrol" },
		func(v *postgres.ServicePlanVersionInput) { v.Name = string([]byte{0xff, 0xfe, 0xff}) },
		func(v *postgres.ServicePlanVersionInput) { v.Name = strings.Repeat("á", 121) },
		func(v *postgres.ServicePlanVersionInput) { v.Allowances = nil },
		func(v *postgres.ServicePlanVersionInput) { v.Allowances = append(v.Allowances, v.Allowances[0]) },
		func(v *postgres.ServicePlanVersionInput) { v.Allowances[1] = v.Allowances[0] },
		func(v *postgres.ServicePlanVersionInput) { v.Allowances[0].Limit = 0 },
		func(v *postgres.ServicePlanVersionInput) { v.Allowances[0].Limit = -1 },
		func(v *postgres.ServicePlanVersionInput) { v.Allowances[0].Unit = "GB" },
		func(v *postgres.ServicePlanVersionInput) { v.Allowances[0].Window = "forever" },
		func(v *postgres.ServicePlanVersionInput) { v.Allowances[0].Capability = "local_pdf" },
		func(v *postgres.ServicePlanVersionInput) { v.Allowances[0].Capability = "local_capture" },
		func(v *postgres.ServicePlanVersionInput) { v.Allowances[0].Capability = "account_recovery" },
		func(v *postgres.ServicePlanVersionInput) { v.Allowances[0].Capability = "privacy_export" },
	} {
		terms := fixtureTerms()
		change(&terms)
		if err := catalog.ValidateVersion(terms); !errors.Is(err, catalog.ErrInvalid) {
			t.Fatalf("invalid terms accepted: %+v %v", terms, err)
		}
	}
	terms := fixtureTerms()
	terms.Allowances[0].Limit = math.MaxInt64
	if err := catalog.ValidateVersion(terms); err != nil {
		t.Fatalf("finite integer allowance rejected: %v", err)
	}
	for _, code := range []string{"fixture-plan", "valid_code-1", strings.Repeat("x", 64)} {
		if err := catalog.ValidateCode(code); err != nil {
			t.Fatal(err)
		}
	}
	for _, code := range []string{"x", "PAID", " paid", strings.Repeat("x", 65), "local plan"} {
		if err := catalog.ValidateCode(code); !errors.Is(err, catalog.ErrInvalid) {
			t.Fatalf("invalid code %q accepted", code)
		}
	}
}

// Invalid input must fail before even reaching a storage dependency.
func TestServiceBoundsBeforePersistence(t *testing.T) {
	s := catalog.New(nil)
	ctx := context.Background()
	id := testutil.ID(1)
	check := func(err error) {
		t.Helper()
		if !errors.Is(err, catalog.ErrInvalid) {
			t.Fatalf("expected validation failure, got %v", err)
		}
	}
	_, err := s.CreatePlan(ctx, " ")
	check(err)
	for _, invalidID := range []string{"bad-id", strings.ToUpper(id), "urn:uuid:" + id} {
		if invalidID == id {
			continue
		}
		_, err = s.Plan(ctx, invalidID)
		check(err)
		_, err = s.AppendVersion(ctx, invalidID, fixtureTerms())
		check(err)
		_, err = s.Version(ctx, invalidID, 1)
		check(err)
		_, err = s.Versions(ctx, invalidID, 0, 10)
		check(err)
		_, err = s.RetirePlan(ctx, invalidID)
		check(err)
	}
	_, err = s.AppendVersion(ctx, id, postgres.ServicePlanVersionInput{})
	check(err)
	for _, number := range []int{0, -1, math.MaxInt32 + 1} {
		_, err = s.Version(ctx, id, number)
		check(err)
	}
	for _, page := range [][2]int{{-1, 10}, {math.MaxInt32 + 1, 10}, {0, 0}, {0, 101}} {
		_, err = s.Versions(ctx, id, page[0], page[1])
		check(err)
	}
}
