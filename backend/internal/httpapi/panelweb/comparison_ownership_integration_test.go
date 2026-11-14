package panelweb_test

import (
	"encoding/json"
	"net/url"
	"strings"
	"testing"

	"vault/internal/service"
	"vault/internal/store/postgres"
)

func TestComparisonEvidenceOwnershipIntegration(t *testing.T) {
	f := newBoundaryFixture(t)
	h := f.h
	exit, err := service.NewInspection(h.pool).Create(h.ctx, f.ids["tenancies"], "exit")
	require(t, err)
	exitRoom, err := postgres.CreateRoom(h.ctx, h.pool, exit.ID, "exit-room", "Exit fixture")
	require(t, err)
	exitEvidence, err := h.evidence.Upload(h.ctx, service.UploadInput{
		InspectionID: exit.ID, RoomID: &exitRoom.ID, Type: "doc", Filename: "fixture.pdf", Mime: "application/pdf",
		Content: strings.NewReader("%PDF-1.4\n% synthetic exit\n%%EOF\n"),
	})
	require(t, err)
	h.call("POST", "/painel/imoveis", f.other, url.Values{"endereco": {"Other owner synthetic fixture"}}, 303)
	var otherProperty string
	require(t, h.pool.QueryRow(h.ctx, `SELECT id::text FROM properties WHERE owner_user_id=$1`, h.accountID("boundary-bob@example.test")).Scan(&otherProperty))
	otherTenancy, err := service.NewTenancy(h.pool).Create(h.ctx, otherProperty)
	require(t, err)
	otherInspection, err := service.NewInspection(h.pool).Create(h.ctx, otherTenancy.ID, "entry")
	require(t, err)
	otherRoom, err := postgres.CreateRoom(h.ctx, h.pool, otherInspection.ID, "other-room", "Other room")
	require(t, err)
	foreign, err := h.evidence.Upload(h.ctx, service.UploadInput{
		InspectionID: otherInspection.ID, RoomID: &otherRoom.ID, Type: "doc", Filename: "fixture.pdf", Mime: "application/pdf",
		Content: strings.NewReader("%PDF-1.4\n% synthetic other owner\n%%EOF\n"),
	})
	require(t, err)
	path := "/api/v1/tenancies/" + f.ids["tenancies"] + "/comparisons"
	valid := map[string]any{"entry_room_id": f.ids["rooms"], "exit_room_id": exitRoom.ID,
		"entry_evidence_id": f.evidenceID, "exit_evidence_id": exitEvidence.Evidence.ID, "shot_category": "overview", "tag": "no_change"}
	request := func(body map[string]any, want int) {
		t.Helper()
		data, err := json.Marshal(body)
		require(t, err)
		out := h.boundaryRequest("POST", path, f.owner, string(data))
		if out.Code != want {
			t.Fatalf("comparison: %d want %d: %s", out.Code, want, out.Body.String())
		}
	}
	request(valid, 200)
	for _, field := range []string{"entry_evidence_id", "exit_evidence_id"} {
		body := map[string]any{}
		for key, value := range valid {
			body[key] = value
		}
		body[field], body["tag"] = foreign.Evidence.ID, "changed"
		request(body, 404)
	}
	// A valid owned Evidence still cannot be attached on the wrong side.
	valid["exit_evidence_id"] = f.evidenceID
	request(valid, 404)
	rows, err := postgres.ListComparisons(h.ctx, h.pool, f.ids["tenancies"])
	require(t, err)
	if len(rows) != 1 || rows[0].Tag != "no_change" || rows[0].ExitEvidenceID == nil || *rows[0].ExitEvidenceID != exitEvidence.Evidence.ID {
		t.Fatal("rejected update mutated comparison")
	}

	// Old invalid rows must not leak foreign references through a read.
	_, err = h.pool.Exec(h.ctx, `UPDATE comparisons SET exit_evidence_id=$1 WHERE id=$2`, foreign.Evidence.ID, rows[0].ID)
	require(t, err)
	out := h.boundaryRequest("GET", path, f.owner, "")
	if out.Code != 404 || strings.Contains(out.Body.String(), foreign.Evidence.ID) {
		t.Fatal("legacy invalid reference escaped scoped read")
	}
}
