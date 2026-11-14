package panelweb_test

import (
	"encoding/json"
	"net/http"
	"strings"
	"testing"
)

func (h *harness) access(email string) string {
	h.t.Helper()
	body, _ := json.Marshal(map[string]string{"email": email, "password": fixturePassword})
	out := h.json("POST", "/api/v1/account/sessions", string(body), "", 200)
	var result struct {
		Token   string `json:"access_token"`
		Type    string `json:"token_type"`
		Expires int    `json:"expires_in"`
	}
	require(h.t, json.Unmarshal(out.Body.Bytes(), &result))
	if len(result.Token) != 43 || result.Type != "Bearer" || result.Expires != 900 || len(out.Result().Cookies()) != 0 {
		h.t.Fatal("unsafe API session result")
	}
	return result.Token
}

func TestAccountAPIIntegration(t *testing.T) {
	h := integration(t)
	h.primary()
	first := h.json("POST", "/api/v1/account/registrations", `{"email":"json@example.test"}`, "", 202)
	existing := h.json("POST", "/api/v1/account/registrations", `{"email":"primary@example.test"}`, "", 202)
	if first.Body.String() != existing.Body.String() {
		t.Fatal("API enumerated registration")
	}
	h.json("POST", "/api/v1/account/registrations", `{"email":"invalid"}`, "", 400)
	proof := h.proof("json@example.test")
	body, _ := json.Marshal(map[string]string{"token": proof, "password": fixturePassword})
	h.json("POST", "/api/v1/account/verification", `{"token":"unknown","password":"fixture password for integration"}`, "", 401)
	h.json("POST", "/api/v1/account/verification", `{"token":"unknown","password":"short"}`, "", 400)
	h.json("POST", "/api/v1/account/verification", string(body), "", 200)
	h.json("POST", "/api/v1/account/verification", string(body), "", 401)
	token := h.access("json@example.test")
	profile := h.json("GET", "/api/v1/account/profile", "", token, 200)
	var own map[string]string
	require(t, json.Unmarshal(profile.Body.Bytes(), &own))
	if len(own) != 3 || own["id"] != h.accountID("json@example.test") || own["email"] != "json@example.test" {
		t.Fatal("profile leaked another identity")
	}
	h.json("GET", "/api/v1/account/profile", "", "", 401)
	h.json("GET", "/api/v1/account/profile", "", token, 401, http.Header{"Cookie": {"ambient=ignored"}})
	staff := h.json("POST", "/api/v1/account/sessions", `{"email":"primary@example.test","password":"fixture password for integration"}`, "", 401)
	unknown := h.json("POST", "/api/v1/account/sessions", `{"email":"absent@example.test","password":"fixture password for integration"}`, "", 401)
	if staff.Body.String() != unknown.Body.String() {
		t.Fatal("staff login differed from unknown identity")
	}
	h.json("DELETE", "/api/v1/account/sessions/current", "", token, 204)
	h.json("GET", "/api/v1/account/profile", "", token, 401)
	// A rolled-back enqueue must never look accepted or expose SQL details.
	require(t, execSQL(h, `CREATE FUNCTION reject_signup() RETURNS trigger LANGUAGE plpgsql AS $$ BEGIN RAISE EXCEPTION 'fixture-secret-detail'; END $$;
		CREATE TRIGGER reject_signup BEFORE INSERT ON account_mail_outbox FOR EACH ROW EXECUTE FUNCTION reject_signup()`))
	out := h.json("POST", "/api/v1/account/registrations", `{"email":"failure@example.test"}`, "", 503)
	if strings.Contains(out.Body.String(), "fixture-secret") {
		t.Fatal("storage detail escaped")
	}
}

func execSQL(h *harness, sql string) error { _, err := h.pool.Exec(h.ctx, sql); return err }
