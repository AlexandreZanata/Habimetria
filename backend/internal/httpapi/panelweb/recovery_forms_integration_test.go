package panelweb_test

import (
	"net/http/httptest"
	"net/url"
	"strings"
	"testing"
)

const recoveredPassword = "recovered browser fixture password"

func (h *harness) recoveryToken(email string) string {
	h.t.Helper()
	var user, id string
	var encrypted []byte
	require(h.t, h.pool.QueryRow(h.ctx, `SELECT o.user_id::text,o.id::text,o.proof_ciphertext FROM account_mail_outbox o
		JOIN users u ON u.id=o.user_id WHERE u.email=$1 AND o.template='password_recovery' AND o.state='pending'`, email).Scan(&user, &id, &encrypted))
	token, err := accountKey(h.t).OpenRecovery(user, id, encrypted)
	require(h.t, err)
	return token
}

func TestRecoveryFormsIntegration(t *testing.T) {
	h := integration(t)
	primary, _ := h.primary()
	user, _ := h.invite(primary, "user", "recover@example.test")
	h.call("GET", "/painel/recuperar", nil, nil, 200)
	first := h.call("POST", "/painel/recuperar", nil, url.Values{"email": {"recover@example.test"}}, 200)
	unknown := h.call("POST", "/painel/recuperar", nil, url.Values{"email": {"missing@example.test"}}, 200)
	staff := h.call("POST", "/painel/recuperar", nil, url.Values{"email": {"primary@example.test"}}, 200)
	if first.Body.String() != unknown.Body.String() || first.Body.String() != staff.Body.String() {
		t.Fatal("web recovery enumerated identity")
	}
	h.call("POST", "/painel/recuperar", nil, url.Values{"email": {"invalid"}}, 400)
	token := h.recoveryToken("recover@example.test")
	h.call("GET", "/painel/redefinir", user, nil, 200)
	values := url.Values{"codigo": {token}, "senha": {recoveredPassword}, "confirmacao": {fixturePassword}}
	mismatch := h.call("POST", "/painel/redefinir", nil, values, 400)
	if !strings.Contains(mismatch.Body.String(), "value=\""+token+"\"") || strings.Contains(mismatch.Body.String(), recoveredPassword) {
		t.Fatal("retry lost proof or echoed password")
	}
	values.Set("confirmacao", recoveredPassword)
	r := httptest.NewRequest("POST", testOrigin+"/painel/redefinir", strings.NewReader(values.Encode()))
	r.Header.Set("Content-Type", "application/x-www-form-urlencoded")
	r.Header.Set("Origin", "https://untrusted.example.test")
	out := httptest.NewRecorder()
	h.handler.ServeHTTP(out, r)
	if out.Code != 403 {
		t.Fatal("cross-origin password replacement accepted")
	}
	out = h.call("POST", "/painel/redefinir", primary, values, 200)
	if len(out.Result().Cookies()) != 0 || !strings.Contains(out.Body.String(), "Senha atualizada") {
		t.Fatal("reset changed another browser account or missed result")
	}
	h.call("GET", "/painel/perfil", user, nil, 303)
	h.call("GET", "/painel/perfil", primary, nil, 200)
	h.call("POST", "/painel/redefinir", nil, values, 401)
	h.call("POST", "/painel/entrar", nil, url.Values{"email": {"recover@example.test"}, "senha": {fixturePassword}}, 401)
	h.call("POST", "/painel/entrar", nil, url.Values{"email": {"recover@example.test"}, "senha": {recoveredPassword}}, 303)
}
