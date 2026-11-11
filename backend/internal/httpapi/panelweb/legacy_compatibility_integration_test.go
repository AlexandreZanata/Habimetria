package panelweb_test

import (
	"encoding/json"
	"net/http"
	"strings"
	"testing"

	"vault/internal/config"
	"vault/internal/httpapi/panelweb"
	"vault/internal/service"
	"vault/internal/store/postgres"
)

func TestUnclaimedDevelopmentJourneyIntegration(t *testing.T) {
	s := newLegacyServer(t)
	s.mode(config.LegacyUnclaimedDevelopment)
	property := s.postID("/api/v1/properties", `{"address":"Synthetic development fixture"}`)
	tenancy := s.postID("/api/v1/properties/"+property+"/tenancies", `{}`)
	inspection := s.postID("/api/v1/tenancies/"+tenancy+"/inspections", `{}`)
	room := s.postID("/api/v1/inspections/"+inspection+"/rooms", `{"key":"manual-room","label":"Synthetic room"}`)
	const pdf = "%PDF-1.4\n% synthetic compatibility document\n%%EOF\n"
	s.upload("/api/v1/inspections/"+inspection+"/evidences", room, pdf, 201)
	seal := s.boundaryRequest("POST", "/api/v1/inspections/"+inspection+"/seal", nil, `{}`)
	if seal.Code != 201 {
		t.Fatalf("legacy seal: %d %s", seal.Code, seal.Body.String())
	}
	var report service.SealOutput
	require(t, json.Unmarshal(seal.Body.Bytes(), &report))
	s.upload("/api/v1/reports/"+report.ReportID+"/pdf", "", pdf, 201)
	s.upload("/api/v1/reports/"+report.ReportID+"/pdf", "", pdf+"changed", 409)
	shared := s.boundaryRequest("POST", "/api/v1/reports/"+report.ReportID+"/shares", nil, `{"channel":"link"}`)
	if shared.Code != 201 {
		t.Fatalf("legacy share: %d", shared.Code)
	}
	var share struct {
		Share postgres.Share `json:"share"`
	}
	require(t, json.Unmarshal(shared.Body.Bytes(), &share))
	var before, after string
	require(t, s.pool.QueryRow(s.ctx, `SELECT row_to_json(m)::text FROM manifests m WHERE inspection_id=$1`, inspection).Scan(&before))
	s.mode(config.LegacyOwned)
	for _, path := range []string{"/api/v1/inspections/" + inspection + "/rooms", "/api/v1/reports/" + report.ReportID} {
		if got := s.boundaryRequest("GET", path, nil, "").Code; got != 404 {
			t.Fatalf("strict mode exposed unclaimed data: %d", got)
		}
	}
	if got := s.boundaryRequest("POST", "/api/v1/properties", nil, `{"address":"Denied fixture"}`).Code; got != 403 {
		t.Fatal("strict mode created anonymous property")
	}
	for _, path := range []string{"/healthz", "/readyz", "/api/v1/ping", "/v/" + report.VerifyCode, "/s/" + share.Share.Token} {
		out := s.boundaryRequest("GET", path, nil, "")
		if out.Code != 200 || strings.Contains(out.Body.String(), "Synthetic development fixture") {
			t.Fatalf("public boundary %s: %d", path, out.Code)
		}
	}
	if out := s.boundaryRequest("GET", "/s/"+share.Share.Token+"/pdf", nil, ""); out.Code != 302 || out.Header().Get("Location") == "" {
		t.Fatal("recipient PDF lost its explicit Share capability")
	}
	require(t, s.pool.QueryRow(s.ctx, `SELECT row_to_json(m)::text FROM manifests m WHERE inspection_id=$1`, inspection).Scan(&after))
	if before != after {
		t.Fatal("mode transition rewrote sealed bytes")
	}
	var owner *string
	require(t, s.pool.QueryRow(s.ctx, `SELECT owner_user_id::text FROM properties WHERE id=$1`, property).Scan(&owner))
	if owner != nil {
		t.Fatal("mode transition claimed a legacy property")
	}
}

func TestDevelopmentExceptionNeverExposesOwnedDataIntegration(t *testing.T) {
	f := newBoundaryFixture(t)
	for _, origin := range []string{"", testOrigin} {
		guard := panelweb.ProtectLegacy(f.h.panel, origin, panelweb.LegacyPolicy{AllowUnclaimedDevelopment: true})
		for _, cookie := range []*http.Cookie{nil, f.other, f.staff} {
			req := newLegacyRequest(f.h, "GET", f.ids["reports"], cookie)
			out := legacyGuardResult(guard, "reports", req, t)
			if out.Code != 404 {
				t.Fatalf("development exception exposed owned Report: %d", out.Code)
			}
		}
	}
}
