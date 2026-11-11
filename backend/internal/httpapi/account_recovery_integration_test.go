package httpapi

import (
	"encoding/json"
	"errors"
	"testing"

	"vault/internal/service/panel"
	"vault/internal/store/postgres"
)

const recoveredPassword = "recovered endpoint fixture password"

func (f accountFixture) enrolled() string {
	f.t.Helper()
	f.call("POST", "registrations", `{"email":"api@example.test"}`, "", 202)
	f.call("POST", "verification", `{"token":"`+f.proof()+`","password":"`+accountPassword+`"}`, "", 200)
	out := f.call("POST", "sessions", `{"email":"api@example.test","password":"`+accountPassword+`"}`, "", 200)
	var access struct {
		Token string `json:"access_token"`
	}
	if err := json.Unmarshal(out.Body.Bytes(), &access); err != nil {
		f.t.Fatal(err)
	}
	return access.Token
}

func (f accountFixture) recoveryToken() string {
	f.t.Helper()
	var user, id string
	var encrypted []byte
	if err := f.pool.QueryRow(f.ctx, `SELECT user_id::text,id::text,proof_ciphertext FROM account_mail_outbox
		WHERE template='password_recovery' AND state='pending'`).Scan(&user, &id, &encrypted); err != nil {
		f.t.Fatal(err)
	}
	token, err := f.key.OpenRecovery(user, id, encrypted)
	if err != nil {
		f.t.Fatal(err)
	}
	return token
}

func TestRecoveryEndpointsIntegration(t *testing.T) {
	f := accountSetup(t)
	access := f.enrolled()
	p := panel.New(postgres.NewPanelStore(f.pool))
	browser, err := p.Login(f.ctx, "api@example.test", accountPassword)
	if err != nil {
		t.Fatal(err)
	}
	first := f.call("POST", "recovery", `{"email":"api@example.test"}`, "", 202)
	unknown := f.call("POST", "recovery", `{"email":"unknown@example.test"}`, "", 202)
	if first.Body.String() != unknown.Body.String() {
		t.Fatal("recovery enumerated identity")
	}
	f.call("POST", "recovery", `{"email":"invalid"}`, "", 400)
	f.call("POST", "recovery", `{"email":"api@example.test"}`, access, 403)
	token := f.recoveryToken()
	body := `{"token":"` + token + `","password":"` + recoveredPassword + `","password_confirmation":"` + recoveredPassword + `"}`
	f.call("POST", "verification", `{"token":"`+token+`","password":"`+recoveredPassword+`"}`, "", 401)
	f.call("POST", "password", `{"token":"bad","password":"short","password_confirmation":"short"}`, "", 400)
	f.call("POST", "password", `{"token":"bad","password":"`+recoveredPassword+`","password_confirmation":"different"}`, "", 400)
	f.call("POST", "password", `{"token":"bad","password":"`+recoveredPassword+`","password_confirmation":"`+recoveredPassword+`"}`, "", 401)
	f.sql(`CREATE FUNCTION reject_notice() RETURNS trigger LANGUAGE plpgsql AS $$ BEGIN RAISE EXCEPTION 'private-detail'; END $$;
		CREATE TRIGGER reject_notice BEFORE INSERT ON account_mail_outbox FOR EACH ROW WHEN (NEW.template='password_changed') EXECUTE FUNCTION reject_notice()`)
	f.call("POST", "password", body, "", 503)
	f.call("GET", "profile", "", access, 200)
	f.sql(`DROP TRIGGER reject_notice ON account_mail_outbox`)
	out := f.call("POST", "password", body, "", 200)
	if len(out.Result().Cookies()) != 0 || out.Body.String() != "{\"status\":\"password_updated\"}\n" {
		t.Fatal("reset auto-signed in or leaked identity")
	}
	f.call("POST", "password", body, "", 401)
	f.call("GET", "profile", "", access, 401)
	if _, err = p.Session(f.ctx, browser); !errors.Is(err, postgres.ErrNotFound) {
		t.Fatal("old browser survived", err)
	}
	f.call("POST", "sessions", `{"email":"api@example.test","password":"`+accountPassword+`"}`, "", 401)
	f.call("POST", "sessions", `{"email":"api@example.test","password":"`+recoveredPassword+`"}`, "", 200)
}

func TestRecoveryEndpointUnavailableAndThrottleIntegration(t *testing.T) {
	f := accountSetup(t)
	f.enrolled()
	f.call("POST", "recovery", `{"email":"api@example.test"}`, "", 202)
	body := `{"token":"` + f.recoveryToken() + `","password":"` + recoveredPassword + `","password_confirmation":"` + recoveredPassword + `"}`
	f.sql(`ALTER TABLE users RENAME TO unavailable_users`)
	for _, tc := range []struct{ path, body string }{{"recovery", `{"email":"api@example.test"}`}, {"password", body}} {
		out := f.call("POST", tc.path, tc.body, "", 503)
		if out.Body.String() != "{\"code\":\"recovery_unavailable\",\"error\":\"recovery_unavailable\"}\n" {
			t.Fatal("storage details escaped")
		}
	}
	f.sql(`ALTER TABLE unavailable_users RENAME TO users; DELETE FROM panel_login_limits`)
	invalid := `{"token":"bad","password":"` + recoveredPassword + `","password_confirmation":"` + recoveredPassword + `"}`
	for range 150 {
		f.call("POST", "password", invalid, "", 401)
	}
	out := f.call("POST", "password", invalid, "", 429)
	if out.Header().Get("Retry-After") != "900" {
		t.Fatal("reset attempt budget missing")
	}
}
