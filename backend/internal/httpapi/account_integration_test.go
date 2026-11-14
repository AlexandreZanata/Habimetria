package httpapi

import (
	"encoding/json"
	"strings"
	"testing"
)

func TestAccountEndpointIntegration(t *testing.T) {
	f := accountSetup(t)
	first := f.call("POST", "registrations", `{"email":"api@example.test"}`, "", 202)
	existing := f.call("POST", "registrations", `{"email":"api@example.test"}`, "", 202)
	if first.Body.String() != existing.Body.String() {
		t.Fatal("non-neutral initiation")
	}
	f.call("POST", "registrations", `{"email":"invalid"}`, "", 400)
	login := `{"email":"api@example.test","password":"` + accountPassword + `"}`
	f.call("POST", "sessions", login, "", 401)
	proof := f.proof()
	verify := `{"token":"` + proof + `","password":"` + accountPassword + `"}`
	f.call("POST", "verification", `{"token":"unknown","password":"`+accountPassword+`"}`, "", 401)
	f.call("POST", "verification", `{"token":"unknown","password":"short"}`, "", 400)
	f.sql(`CREATE FUNCTION reject_account_audit() RETURNS trigger LANGUAGE plpgsql AS $$ BEGIN RAISE EXCEPTION 'private-fixture-detail'; END $$;
		CREATE TRIGGER reject_account_audit BEFORE INSERT ON panel_audit FOR EACH ROW EXECUTE FUNCTION reject_account_audit()`)
	out := f.call("POST", "verification", verify, "", 503)
	if strings.Contains(out.Body.String(), "private-fixture") {
		t.Fatal("audit failure leaked")
	}
	f.sql(`DROP TRIGGER reject_account_audit ON panel_audit`)
	f.call("POST", "verification", verify, "", 200)
	f.call("POST", "verification", verify, "", 401)
	out = f.call("POST", "sessions", login, "", 200)
	var access struct {
		Token   string `json:"access_token"`
		Kind    string `json:"token_type"`
		Seconds int    `json:"expires_in"`
	}
	if err := json.Unmarshal(out.Body.Bytes(), &access); err != nil {
		t.Fatal(err)
	}
	if len(access.Token) != 43 || access.Kind != "Bearer" || access.Seconds != 900 || len(out.Result().Cookies()) != 0 {
		t.Fatal("invalid access response")
	}
	out = f.call("GET", "profile", "", access.Token, 200)
	var own map[string]string
	if err := json.Unmarshal(out.Body.Bytes(), &own); err != nil || len(own) != 3 || own["email"] != "api@example.test" {
		t.Fatal("wrong profile")
	}
	f.call("GET", "profile", "", "invalid", 401)
	f.call("GET", "profile", "", strings.Repeat("b", 43), 401)
	f.sql(`CREATE FUNCTION reject_access_delete() RETURNS trigger LANGUAGE plpgsql AS $$ BEGIN RAISE EXCEPTION 'private-fixture-detail'; END $$;
		CREATE TRIGGER reject_access_delete BEFORE DELETE ON account_access_sessions FOR EACH ROW EXECUTE FUNCTION reject_access_delete()`)
	f.call("DELETE", "sessions/current", "", access.Token, 503)
	f.sql(`DROP TRIGGER reject_access_delete ON account_access_sessions`)
	f.call("DELETE", "sessions/current", "", access.Token, 204)
	f.call("GET", "profile", "", access.Token, 401)
	f.sql(`DELETE FROM panel_login_limits`)
	for range 10 {
		f.call("POST", "sessions", `{"email":"api@example.test","password":"wrong fixture password"}`, "", 401)
	}
	out = f.call("POST", "sessions", login, "", 429)
	if out.Header().Get("Retry-After") != "900" {
		t.Fatal("missing throttle backoff")
	}
}

func TestAccountUnavailableIntegration(t *testing.T) {
	f := accountSetup(t)
	f.call("POST", "registrations", `{"email":"api@example.test"}`, "", 202)
	verify := `{"token":"` + f.proof() + `","password":"` + accountPassword + `"}`
	f.call("POST", "verification", verify, "", 200)
	login := `{"email":"api@example.test","password":"` + accountPassword + `"}`
	out := f.call("POST", "sessions", login, "", 200)
	var access struct {
		Token string `json:"access_token"`
	}
	if err := json.Unmarshal(out.Body.Bytes(), &access); err != nil {
		t.Fatal(err)
	}
	f.sql(`ALTER TABLE users RENAME TO unavailable_users`)
	for _, tc := range []struct{ method, path, body, token string }{
		{"GET", "profile", "", access.Token}, {"POST", "sessions", login, ""},
		{"POST", "verification", verify, ""}, {"POST", "registrations", `{"email":"new@example.test"}`, ""},
	} {
		out = f.call(tc.method, tc.path, tc.body, tc.token, 503)
		if out.Body.String() != "{\"code\":\"verification_unavailable\",\"error\":\"verification_unavailable\"}\n" {
			t.Fatal("infrastructure details escaped")
		}
	}
	f.sql(`ALTER TABLE unavailable_users RENAME TO users`)
	f.pool.Close()
	f.call("POST", "sessions", login, "", 503)
}
