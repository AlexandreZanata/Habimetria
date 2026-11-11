package panelweb_test

import (
	"encoding/json"
	"net/http"
	"net/http/httptest"
	"net/url"
	"strings"
	"testing"

	"vault/internal/service"
	"vault/internal/store/postgres"
)

type boundaryFixture struct {
	h                         *harness
	owner, other, staff       *http.Cookie
	ownerID                   string
	ids                       map[string]string
	evidenceID, verify, token string
}

func (h *harness) boundaryRequest(method, path string, cookie *http.Cookie, body string) *httptest.ResponseRecorder {
	req := httptest.NewRequest(method, testOrigin+path, strings.NewReader(body)).WithContext(h.ctx)
	req.Header.Set("Content-Type", "application/json")
	req.Header.Set("Origin", testOrigin)
	if cookie != nil {
		req.AddCookie(cookie)
	}
	out := httptest.NewRecorder()
	h.handler.ServeHTTP(out, req)
	return out
}

func newBoundaryFixture(t *testing.T) boundaryFixture {
	t.Helper()
	h := integration(t)
	staff, _ := h.primary()
	owner, ownerID := h.invite(staff, "user", "boundary-alice@example.test")
	other, _ := h.invite(staff, "user", "boundary-bob@example.test")
	h.call("POST", "/painel/imoveis", owner, url.Values{"endereco": {"Private boundary fixture Alice"}}, 303)
	var property, tenancy, inspection string
	require(t, h.pool.QueryRow(h.ctx, `SELECT id::text FROM properties WHERE owner_user_id=$1`, ownerID).Scan(&property))
	h.call("POST", "/painel/imoveis", owner, url.Values{"acao": {"vistoria"}, "id": {property}}, 303)
	require(t, h.pool.QueryRow(h.ctx, `SELECT i.id::text, t.id::text FROM inspections i JOIN tenancies t ON t.id=i.tenancy_id WHERE t.property_id=$1`, property).Scan(&inspection, &tenancy))
	room, err := postgres.CreateRoom(h.ctx, h.pool, inspection, "boundary-room", "Fixture room")
	require(t, err)
	const pdf = "%PDF-1.4\n% synthetic boundary document\n%%EOF\n"
	upload, err := h.evidence.Upload(h.ctx, service.UploadInput{
		InspectionID: inspection, RoomID: &room.ID, Type: "doc", Filename: "fixture.pdf", Mime: "application/pdf", Content: strings.NewReader(pdf),
	})
	require(t, err)
	occurrence, err := service.NewOccurrence(h.pool).Create(h.ctx, property, "Synthetic occurrence", nil)
	require(t, err)
	sealed := h.sealRequest(inspection, owner, `{}`)
	if sealed.Code != 201 {
		t.Fatalf("fixture seal: %d %s", sealed.Code, sealed.Body.String())
	}
	var report service.SealOutput
	require(t, json.Unmarshal(sealed.Body.Bytes(), &report))
	h.uploadPDF("/api/v1/reports/"+report.ReportID+"/pdf", owner, pdf, 201)
	shared := h.boundaryRequest("POST", "/api/v1/reports/"+report.ReportID+"/shares", owner, `{"channel":"link"}`)
	if shared.Code != 201 {
		t.Fatalf("fixture share: %d %s", shared.Code, shared.Body.String())
	}
	var share struct {
		Share postgres.Share `json:"share"`
	}
	require(t, json.Unmarshal(shared.Body.Bytes(), &share))
	return boundaryFixture{
		h: h, owner: owner, other: other, staff: staff, ownerID: ownerID,
		ids: map[string]string{"properties": property, "tenancies": tenancy, "inspections": inspection,
			"rooms": room.ID, "reports": report.ReportID, "occurrences": occurrence.ID},
		evidenceID: upload.Evidence.ID, verify: report.VerifyCode, token: share.Share.Token,
	}
}
