package panelweb_test

import (
	"encoding/json"
	"testing"
)

func TestRecoveryPreservesPrivateSealedReportIntegration(t *testing.T) {
	f := newBoundaryFixture(t)
	h := f.h
	old := h.access("boundary-alice@example.test")
	other := h.access("boundary-bob@example.test")
	path := "/api/v1/reports/" + f.ids["reports"]
	var before, after string
	query := `SELECT row_to_json(r)::text FROM reports r WHERE id=$1`
	require(t, h.pool.QueryRow(h.ctx, query, f.ids["reports"]).Scan(&before))
	h.json("POST", "/api/v1/account/recovery", `{"email":"boundary-alice@example.test"}`, "", 202)
	token := h.recoveryToken("boundary-alice@example.test")
	body, _ := json.Marshal(map[string]string{"token": token, "password": recoveredPassword, "password_confirmation": recoveredPassword})
	h.json("POST", "/api/v1/account/password", string(body), "", 200)
	h.json("GET", path, "", old, 404)
	h.json("GET", path, "", other, 404)
	login, _ := json.Marshal(map[string]string{"email": "boundary-alice@example.test", "password": recoveredPassword})
	out := h.json("POST", "/api/v1/account/sessions", string(login), "", 200)
	var access struct {
		Token string `json:"access_token"`
	}
	require(t, json.Unmarshal(out.Body.Bytes(), &access))
	h.json("GET", path, "", access.Token, 200)
	h.json("POST", "/api/v1/inspections/"+f.ids["inspections"]+"/rooms", `{"key":"new","label":"New"}`, access.Token, 409)
	require(t, h.pool.QueryRow(h.ctx, query, f.ids["reports"]).Scan(&after))
	if before != after {
		t.Fatal("recovery rewrote sealed Report")
	}
	h.json("GET", "/api/v1/account/profile", "", other, 200)
}
