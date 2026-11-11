package panelweb_test

import (
	"net/url"
	"strings"
	"testing"

	"vault/internal/authz"
)

func TestCommercialCapabilitiesIntegration(t *testing.T) {
	f := newBoundaryFixture(t)
	h := f.h
	admin, _ := h.invite(f.staff, "admin", "commercial-admin@example.test")
	support, _ := h.invite(f.staff, "support", "commercial-support@example.test")
	actor, err := h.panel.Session(h.ctx, admin.Value)
	require(t, err)
	if actor.Can("commerce.read") || actor.Can("finance.read") || actor.Can("invented.write") {
		t.Fatal("commercial capability auto-granted")
	}
	for _, capability := range authz.Registry() {
		if capability.Support {
			continue
		}
		h.call("POST", "/painel/configuracoes", f.staff, url.Values{
			"papel": {"support"}, "permissao": {"accounts.read", capability.ID},
		}, 400)
	}
	grant := url.Values{"papel": {"admin"}, "permissao": {"commerce.read", "finance.read", "grants.write", "events.replay"}}
	h.call("POST", "/painel/configuracoes", support, grant, 403)
	h.call("POST", "/painel/configuracoes", admin, grant, 403)
	h.call("POST", "/painel/configuracoes", f.staff, grant, 303)
	actor, err = h.panel.Session(h.ctx, admin.Value)
	require(t, err)
	if !actor.Can("finance.read") || actor.Can("accounts.write") || actor.Can("reports.read") {
		t.Fatal("explicit current grant not scoped")
	}
	path := "/painel/documentos/" + f.ids["reports"]
	h.call("GET", path, admin, nil, 404)
	h.call("POST", "/painel/relatorios", f.owner, url.Values{"acao": {"disponibilizar"}, "id": {f.ids["reports"]}}, 303)
	h.call("GET", path, admin, nil, 404) // Titleholder grant alone is insufficient.
	grant["permissao"] = []string{"reports.read"}
	h.call("POST", "/painel/configuracoes", f.staff, grant, 303)
	h.call("GET", path, admin, nil, 200)
	h.call("POST", "/painel/relatorios", f.owner, url.Values{"acao": {"revogar"}, "id": {f.ids["reports"]}}, 303)
	h.call("GET", path, admin, nil, 404)
	view := h.call("GET", "/painel/configuracoes", f.staff, nil, 200).Body.String()
	for _, capability := range authz.Registry() {
		if !strings.Contains(view, `value="`+capability.ID+`"`) {
			t.Fatalf("missing registry option %s", capability.ID)
		}
	}
}
