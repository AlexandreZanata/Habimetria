package panelweb_test

import (
	"net/url"
	"strings"
	"testing"

	"vault/internal/store/postgres"
)

func TestReasonedPolicyAuditIntegration(t *testing.T) {
	h := integration(t)
	owner, ownerID := h.primary()
	user, userID := h.invite(owner, "user", "audit-private@example.test")
	update := url.Values{"acao": {"atualizar"}, "id": {userID}, "estado": {"suspenso"}}
	for _, reason := range []string{"", "audit-private@example.test", "access_review\n"} {
		update.Set("motivo", reason)
		h.call("POST", "/painel/usuarios", owner, update, 400)
		h.call("GET", "/painel/perfil", user, nil, 200)
	}
	update.Set("motivo", "security_response")
	h.call("POST", "/painel/usuarios", owner, update, 303)
	h.call("GET", "/painel/perfil", user, nil, 303)
	h.call("POST", "/painel/usuarios", owner, update, 303) // No-op is not a new change.
	grant := url.Values{"papel": {"support"}, "permissao": {"commerce.read", "support.write"}}
	h.call("POST", "/painel/configuracoes", owner, grant, 400)
	grant.Set("motivo", "access_review")
	h.call("POST", "/painel/configuracoes", owner, grant, 303)
	h.call("POST", "/painel/configuracoes", owner, grant, 303)
	var count, operations int
	require(t, h.pool.QueryRow(h.ctx, `SELECT count(*),count(DISTINCT operation_id) FROM panel_audit
		WHERE event IN ('account_created','account_updated','permissions_updated')`).Scan(&count, &operations))
	if count != 3 || operations != 3 {
		t.Fatalf("successful changes not exactly once: %d/%d", count, operations)
	}
	var actor, subject, reason, before, after string
	var beforeActive, afterActive, serverTime bool
	require(t, h.pool.QueryRow(h.ctx, `SELECT actor_id::text,subject_id::text,reason_code,before_role,after_role,
		before_active,after_active,occurred_at BETWEEN now()-interval '1 minute' AND clock_timestamp()
		FROM panel_audit WHERE event='account_updated'`).Scan(&actor, &subject, &reason, &before, &after, &beforeActive, &afterActive, &serverTime))
	if actor != ownerID || subject != userID || reason != "security_response" || before != "user" || after != "user" || !beforeActive || afterActive || !serverTime {
		t.Fatal("policy transition was not attributed from actual server state")
	}
	var payload string
	require(t, h.pool.QueryRow(h.ctx, `SELECT jsonb_agg(to_jsonb(a))::text FROM panel_audit a`).Scan(&payload))
	for _, forbidden := range []string{"audit-private@example.test", fixturePassword, user.Value, owner.Value, "Fixture user"} {
		if strings.Contains(payload, forbidden) {
			t.Fatal("audit copied private content or credentials")
		}
	}
	view := h.call("GET", "/painel/atividade", owner, nil, 200).Body.String()
	for _, label := range []string{"Resposta a um incidente de segurança", "Revisão de acesso", "Operação:", "Suspenso", "Consultar contexto comercial"} {
		if !strings.Contains(view, label) {
			t.Fatalf("missing rendered audit detail: %s", label)
		}
	}
	store := postgres.NewPanelStore(h.pool)
	if err := store.SetRole(h.ctx, ownerID, "support", []string{"accounts.write"}, "access_review"); err == nil {
		t.Fatal("direct store caller bypassed policy validation")
	}
	h.call("POST", "/painel/configuracoes", owner, url.Values{"papel": {"support"}, "motivo": {"access_review"}}, 303)
	var cleared bool
	require(t, h.pool.QueryRow(h.ctx, `SELECT cardinality(after_permissions)=0 FROM panel_audit
		WHERE event='permissions_updated' ORDER BY id DESC LIMIT 1`).Scan(&cleared))
	if !cleared {
		t.Fatal("empty role grant not represented honestly in audit")
	}
}

func TestPendingActivationAuditIntegration(t *testing.T) {
	h := integration(t)
	owner, _ := h.primary()
	invite := h.call("POST", "/painel/usuarios", owner, url.Values{
		"acao": {"convidar"}, "email": {"pending-audit@example.test"}, "nome": {"Synthetic pending account"},
	}, 200)
	old := activationLink.FindStringSubmatch(invite.Body.String())
	if len(old) != 2 {
		t.Fatal("initial pending activation absent")
	}
	id := h.accountID("pending-audit@example.test")
	reissue := h.call("POST", "/painel/usuarios", owner, url.Values{"acao": {"acesso"}, "id": {id}}, 200)
	fresh := activationLink.FindStringSubmatch(reissue.Body.String())
	if len(fresh) != 2 || old[1] == fresh[1] {
		t.Fatal("pending activation not replaced")
	}
	h.call("POST", "/painel/ativar", nil, url.Values{"codigo": {old[1]}, "senha": {fixturePassword}}, 401)
	h.activate(fresh[1])
	var reason, payload string
	require(t, h.pool.QueryRow(h.ctx, `SELECT reason_code,to_jsonb(a)::text FROM panel_audit a
		WHERE event='access_issued' AND subject_id=$1`, id).Scan(&reason, &payload))
	if reason != "activation_delivery" || strings.Contains(payload, old[1]) || strings.Contains(payload, fresh[1]) {
		t.Fatal("activation audit omitted reason or copied a credential")
	}
}
