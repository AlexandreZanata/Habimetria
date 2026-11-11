package httpapi

import (
	"encoding/json"
	"testing"

	"vault/internal/service/account"

	"github.com/google/uuid"
)

func (f accountFixture) devicePair(device account.Device) account.DevicePair {
	f.t.Helper()
	body, _ := json.Marshal(map[string]any{"email": "api@example.test", "password": accountPassword, "device": device})
	out := f.call("POST", "sessions", string(body), "", 200)
	var pair account.DevicePair
	if err := json.Unmarshal(out.Body.Bytes(), &pair); err != nil {
		f.t.Fatal(err)
	}
	if len(pair.Refresh) != 47 || pair.ID == "" || pair.User == "" || len(out.Result().Cookies()) != 0 {
		f.t.Fatal("unsafe device response")
	}
	return pair
}

func TestDeviceSessionAPIIntegration(t *testing.T) {
	f := accountSetup(t)
	legacy := f.enrolled()
	d1 := account.Device{ID: uuid.NewString(), Platform: "android", Label: "Phone"}
	d2 := account.Device{ID: uuid.NewString(), Platform: "ios", Label: "Tablet"}
	one, two := f.devicePair(d1), f.devicePair(d2)
	out := f.call("GET", "sessions", "", one.Access, 200)
	var list struct {
		Sessions []struct {
			ID string `json:"session_id"`
		} `json:"sessions"`
	}
	if err := json.Unmarshal(out.Body.Bytes(), &list); err != nil || len(list.Sessions) != 2 {
		t.Fatal("bad own device list", err)
	}
	body, _ := json.Marshal(map[string]string{"refresh_token": one.Refresh, "device_id": d1.ID})
	f.call("POST", "sessions/refresh", string(body), one.Access, 403)
	out = f.call("POST", "sessions/refresh", string(body), "", 200)
	var next account.DevicePair
	if err := json.Unmarshal(out.Body.Bytes(), &next); err != nil {
		t.Fatal(err)
	}
	f.call("GET", "profile", "", one.Access, 401)
	f.call("GET", "profile", "", next.Access, 200)
	f.call("POST", "sessions/refresh", string(body), "", 401)
	f.call("GET", "profile", "", next.Access, 401)
	f.call("GET", "profile", "", two.Access, 200)
	f.call("DELETE", "sessions/"+two.ID, "", legacy, 204)
	f.call("DELETE", "sessions/"+two.ID, "", legacy, 204)
	f.call("GET", "profile", "", two.Access, 401)
	f.call("DELETE", "sessions/"+uuid.NewString(), "", legacy, 404)
	f.call("DELETE", "sessions/invalid", "", legacy, 404)
	f.call("POST", "sessions/refresh", `{"refresh_token":"bad","device_id":"bad"}`, "", 401)
	f.call("POST", "sessions", `{"email":"api@example.test","password":"`+accountPassword+`","device":{"id":"bad","platform":"android","label":"x"}}`, "", 400)
}

func TestDeviceSessionCrossUserAndOutageIntegration(t *testing.T) {
	f := accountSetup(t)
	legacy := f.enrolled()
	d := account.Device{ID: uuid.NewString(), Platform: "android", Label: "Phone"}
	pair := f.devicePair(d)
	f.sql(`INSERT INTO users(email,panel_managed,panel_password) SELECT 'other@example.test',true,panel_password FROM users WHERE email='api@example.test'`)
	response := f.call("POST", "sessions", `{"email":"other@example.test","password":"`+accountPassword+`"}`, "", 200)
	var other struct {
		Token string `json:"access_token"`
	}
	if err := json.Unmarshal(response.Body.Bytes(), &other); err != nil {
		t.Fatal(err)
	}
	f.call("DELETE", "sessions/"+pair.ID, "", other.Token, 404)
	out := f.call("GET", "sessions", "", other.Token, 200)
	if out.Body.String() != "{\"sessions\":[]}\n" {
		t.Fatal("another User saw device metadata")
	}
	f.sql(`ALTER TABLE account_device_sessions RENAME TO unavailable_devices`)
	f.call("GET", "sessions", "", legacy, 503)
	f.call("DELETE", "sessions/"+pair.ID, "", legacy, 503)
	body, _ := json.Marshal(map[string]string{"refresh_token": pair.Refresh, "device_id": d.ID})
	f.call("POST", "sessions/refresh", string(body), "", 503)
	f.sql(`ALTER TABLE unavailable_devices RENAME TO account_device_sessions`)
}
