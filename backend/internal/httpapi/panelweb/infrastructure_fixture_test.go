package panelweb_test

import (
	"testing"

	"vault/internal/httpapi"
	"vault/internal/httpapi/panelweb"
	"vault/internal/service"
	"vault/internal/service/panel"
	"vault/internal/storage/localstore"
	"vault/internal/store/postgres"
	"vault/internal/testutil"
	"vault/internal/testutil/collectionfixture"
	"vault/migrations"
)

func integration(t *testing.T) *harness {
	t.Helper()
	return integrationWithStorage(t, localstore.New(t.TempDir()))
}

func integrationWithStorage(t *testing.T, store collectionfixture.Objects) *harness {
	t.Helper()
	ctx, pool := testutil.Database(t)
	require(t, postgres.Apply(ctx, pool, migrations.FS))
	p := panel.New(postgres.NewPanelStore(pool))
	accounts := accountServices(t, pool, p)
	web, err := panelweb.NewConnected(p, store, testOrigin, panelweb.AccountServices{
		Registration: accounts.Registration, Verification: accounts.Verification, Recovery: accounts.Recovery, PasswordReset: accounts.PasswordReset})
	require(t, err)
	evidence := service.NewEvidence(pool, store)
	router := httpapi.NewRouter(httpapi.Deps{
		Panel: web.Routes(), Account: accounts, Guard: panelweb.ProtectLegacy(p, testOrigin, panelweb.LegacyPolicy{API: accounts.Authentication}),
		Room: service.NewRoom(pool), Evidence: evidence,
		Property: service.NewProperty(pool), Tenancy: service.NewTenancy(pool), Inspection: service.NewInspection(pool),
		Finding: service.NewFinding(pool), Coverage: service.NewCoverage(pool),
		Occurrence: service.NewOccurrence(pool), Compare: service.NewCompare(pool),
		Share: service.NewShare(pool, store, testOrigin, testutil.HMACKey()), HMACKey: testutil.HMACKey(),
		Seal: service.NewSeal(pool, testutil.HMACKey()), Report: service.NewReport(pool, store, testOrigin),
	})
	return &harness{t: t, ctx: ctx, pool: pool, panel: p, evidence: evidence, handler: httpapi.SecurityHeaders(router)}
}
