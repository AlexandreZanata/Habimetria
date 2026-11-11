package panelweb_test

import (
	"encoding/json"
	"net/http"
	"net/url"
	"strings"
	"testing"

	"vault/internal/httpapi"
	"vault/internal/httpapi/routepolicy"
)

func TestClassifiedOwnerScopesIntegration(t *testing.T) {
	f := newBoundaryFixture(t)
	checked := 0
	for _, rule := range httpapi.NewRouter(httpapi.Deps{}).Rules() {
		if rule.Class != routepolicy.HostedOwner {
			continue
		}
		checked++
		method, path, _ := strings.Cut(rule.Pattern, " ")
		path = strings.ReplaceAll(path, "{id}", f.ids[rule.Scope])
		if strings.HasSuffix(path, "/evidences/download-url") {
			path += "?evidence_id=" + f.evidenceID
		}
		denied := 404
		if path == "/api/v1/properties" {
			denied = 403 // Panel-enabled creation is deliberately through the panel.
		}
		for _, actor := range []*http.Cookie{nil, f.other, f.staff} {
			out := f.h.boundaryRequest(method, path, actor, `{"owner_user_id":"`+f.ownerID+`"}`)
			if out.Code != denied {
				t.Fatalf("%s exposed another owner's resource: %d %s", rule.Pattern, out.Code, out.Body.String())
			}
			if method == "GET" {
				if got := f.h.boundaryRequest("HEAD", path, actor, "").Code; got != denied {
					t.Fatalf("HEAD bypassed %s: %d", rule.Pattern, got)
				}
			}
		}
		if method == "GET" {
			out := f.h.boundaryRequest(method, path, f.owner, "")
			if out.Code != 200 {
				t.Fatalf("owner cannot read %s: %d %s", rule.Pattern, out.Code, out.Body.String())
			}
		}
	}
	if checked != 24 {
		t.Fatalf("owner integration covered %d routes instead of 24", checked)
	}
	// Owner identity cannot be supplied through a query parameter either.
	path := "/api/v1/reports/" + f.ids["reports"] + "?owner_user_id=" + f.ownerID
	if got := f.h.boundaryRequest("GET", path, f.other, "").Code; got != 404 {
		t.Fatalf("query owner override returned %d", got)
	}
	ownerPage := f.h.call("GET", "/painel/imoveis", f.owner, nil, 200).Body.String()
	otherPage := f.h.call("GET", "/painel/imoveis", f.other, nil, 200).Body.String()
	if !strings.Contains(ownerPage, "Private boundary fixture Alice") || strings.Contains(otherPage, "Private boundary fixture Alice") {
		t.Fatal("panel library ownership is inconsistent with API ownership")
	}
}

func TestClassifiedControlsAndRecipientsIntegration(t *testing.T) {
	f := newBoundaryFixture(t)
	f.h.call("GET", "/painel/", nil, nil, 200)
	f.h.call("GET", "/painel/assets/favicon.svg", nil, nil, 200)
	f.h.call("GET", "/painel/perfil", f.other, nil, 200)
	f.h.call("POST", "/painel/perfil", f.other, url.Values{"nome": {"Updated boundary Bob"}}, 303)
	if strings.Contains(f.h.call("GET", "/painel/perfil", f.owner, nil, 200).Body.String(), "Updated boundary Bob") {
		t.Fatal("profile update escaped account scope")
	}
	f.h.call("GET", "/painel/configuracoes", f.other, nil, 403)
	for _, path := range []string{"/v/" + f.verify, "/s/" + f.token} {
		out := f.h.boundaryRequest("GET", path, nil, "")
		if out.Code != 200 || strings.Contains(out.Body.String(), "Private boundary fixture Alice") {
			t.Fatalf("public recipient boundary failed: %s %d", path, out.Code)
		}
	}
	verified := f.h.boundaryRequest("GET", "/v/"+f.verify, nil, "")
	var result struct {
		Valid bool `json:"signature_valid"`
	}
	require(t, json.Unmarshal(verified.Body.Bytes(), &result))
	if !result.Valid {
		t.Fatal("public verification no longer verifies the stored seal")
	}
	if out := f.h.boundaryRequest("GET", "/s/"+f.token+"/pdf", nil, ""); out.Code != 302 || out.Header().Get("Location") == "" {
		t.Fatal("authorized recipient cannot obtain the PDF")
	}
	for _, path := range []string{"/v/UNKNOWN234", "/s/UNKNOWN234", "/s/UNKNOWN234/pdf", "/painel/documentos/" + f.ids["reports"]} {
		if got := f.h.boundaryRequest("GET", path, nil, "").Code; got != 404 {
			t.Fatalf("unknown/unauthenticated recipient path returned %d: %s", got, path)
		}
	}
	f.h.call("POST", "/painel/sair", f.other, nil, 303)
	f.h.call("GET", "/painel/perfil", f.other, nil, 303)
}
