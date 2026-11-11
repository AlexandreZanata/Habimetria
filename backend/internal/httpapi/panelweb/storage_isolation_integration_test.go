package panelweb_test

import (
	"net/http"
	"net/url"
	"strings"
	"testing"

	"vault/internal/testutil/collectionfixture"
	"vault/internal/testutil/s3fixture"
)

func TestPrivateCollectionStorageIsolationIntegration(t *testing.T) {
	objects := s3fixture.Store(t)
	h := integrationWithStorage(t, objects)
	staff, _ := h.primary()
	alice, aliceID := h.invite(staff, "user", "storage-alice@example.test")
	bob, bobID := h.invite(staff, "user", "storage-bob@example.test")
	a := collectionfixture.New(t, h.ctx, h.pool, objects, aliceID, "Alice")
	b := collectionfixture.New(t, h.ctx, h.pool, objects, bobID, "Bob")
	legacy := collectionfixture.New(t, h.ctx, h.pool, objects, "", "Unclaimed")
	before := a.Snapshot(t, h.ctx, h.pool, objects)
	for _, tc := range []struct {
		fixture collectionfixture.Fixture
		owner   *http.Cookie
	}{
		{a, alice}, {b, bob}, {legacy, nil},
	} {
		for _, actor := range []*http.Cookie{alice, bob, staff, nil} {
			want := 404
			if tc.owner != nil && actor == tc.owner {
				want = 200
			}
			for _, path := range []string{"/painel/documentos/" + tc.fixture.Report,
				"/api/v1/reports/" + tc.fixture.Report, "/api/v1/inspections/" + tc.fixture.Inspection + "/rooms"} {
				out := h.call("GET", path, actor, nil, want)
				if want == 200 && strings.HasPrefix(path, "/painel/documentos/") && out.Body.String() != collectionfixture.PDF {
					t.Fatal("authorized preview differs from private S3 bytes")
				}
			}
		}
	}
	path := "/painel/documentos/" + a.Report
	grant := url.Values{"acao": {"disponibilizar"}, "id": {a.Report}}
	h.call("POST", "/painel/relatorios", bob, grant, 404)
	h.call("POST", "/painel/relatorios", alice, grant, 303)
	if h.call("GET", path+"?baixar=1", staff, nil, 200).Body.String() != collectionfixture.PDF {
		t.Fatal("granted download differs from stored bytes")
	}
	h.call("GET", "/painel/documentos/"+b.Report, staff, nil, 404)
	h.call("GET", "/api/v1/reports/"+a.Report, staff, nil, 404)
	grant.Set("acao", "revogar")
	h.call("POST", "/painel/relatorios", alice, grant, 303)
	h.call("GET", path, staff, nil, 404)
	h.call("GET", path, alice, nil, 200)
	h.uploadPDF("/api/v1/reports/"+a.Report+"/pdf", alice, collectionfixture.PDF+"changed", 409)
	out := h.boundaryRequest("POST", "/api/v1/inspections/"+a.Inspection+"/rooms", alice, `{"key":"extra","label":"Extra"}`)
	if out.Code != 409 {
		t.Fatalf("sealed collection accepted Room mutation: %d", out.Code)
	}
	if after := a.Snapshot(t, h.ctx, h.pool, objects); after != before {
		t.Fatal("access/grant/rejected mutation changed frozen rows or private objects")
	}
	public := h.call("GET", "/v/"+a.VerifyCode, nil, nil, 200).Body.String()
	for _, private := range []string{aliceID, bobID, a.EvidenceKey, a.PDFKey, "storage-alice@example.test", "Synthetic fixture Alice"} {
		if strings.Contains(public, private) {
			t.Fatal("public verification disclosed fixture identity or object location")
		}
	}
}
