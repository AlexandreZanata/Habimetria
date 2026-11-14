package panelweb_test

import (
	"bytes"
	"encoding/json"
	"mime/multipart"
	"net/http"
	"net/http/httptest"
	"net/url"
	"strings"
	"testing"

	"vault/internal/service"
)

func (h *harness) uploadPDF(path string, cookie *http.Cookie, contents string, status int) {
	h.t.Helper()
	var body bytes.Buffer
	writer := multipart.NewWriter(&body)
	part, err := writer.CreateFormFile("file", "fixture.pdf")
	require(h.t, err)
	_, err = part.Write([]byte(contents))
	require(h.t, err)
	require(h.t, writer.Close())
	req := httptest.NewRequest("POST", testOrigin+path, &body).WithContext(h.ctx)
	req.Header.Set("Origin", testOrigin)
	req.Header.Set("Content-Type", writer.FormDataContentType())
	req.AddCookie(cookie)
	out := httptest.NewRecorder()
	h.handler.ServeHTTP(out, req)
	if out.Code != status {
		h.t.Fatalf("attach PDF: %d, want %d: %s", out.Code, status, out.Body.String())
	}
}

func TestPanelDocumentIsolationIntegration(t *testing.T) {
	h := integration(t)
	admin, _ := h.primary()
	alice, aliceID := h.invite(admin, "user", "alice@example.test")
	bob, _ := h.invite(admin, "user", "bob@example.test")
	h.call("POST", "/painel/imoveis", alice, url.Values{"endereco": {"Endereço privado sintético"}}, 303)
	var property string
	require(t, h.pool.QueryRow(h.ctx, `SELECT id::text FROM properties WHERE owner_user_id=$1`, aliceID).Scan(&property))
	newInspection := url.Values{"acao": {"vistoria"}, "id": {property}}
	h.call("POST", "/painel/imoveis", bob, newInspection, 404)
	h.call("POST", "/painel/imoveis", alice, newInspection, 303)
	var inspection string
	require(t, h.pool.QueryRow(h.ctx, `SELECT i.id::text FROM inspections i JOIN tenancies t ON t.id=i.tenancy_id WHERE t.property_id=$1`, property).Scan(&inspection))
	h.call("GET", "/api/v1/inspections/"+inspection+"/rooms", nil, nil, 404)
	h.call("GET", "/api/v1/inspections/"+inspection+"/rooms", bob, nil, 404)
	h.call("GET", "/api/v1/inspections/"+inspection+"/rooms", alice, nil, 200)
	if strings.Contains(h.call("GET", "/painel/imoveis", bob, nil, 200).Body.String(), "Endereço privado") {
		t.Fatal("property escaped owner scope")
	}

	const pdf = "%PDF-1.4\n% synthetic panel document\n%%EOF\n"
	_, err := h.evidence.Upload(h.ctx, service.UploadInput{
		InspectionID: inspection, Type: "doc", Filename: "fixture.pdf", Mime: "application/pdf", Content: strings.NewReader(pdf),
	})
	require(t, err)
	seal := h.call("POST", "/api/v1/inspections/"+inspection+"/seal", alice, nil, 201)
	var record struct {
		ReportID string `json:"report_id"`
	}
	require(t, json.Unmarshal(seal.Body.Bytes(), &record))
	if record.ReportID == "" {
		t.Fatal("missing sealed report")
	}
	path := "/painel/documentos/" + record.ReportID
	api := "/api/v1/reports/" + record.ReportID
	h.uploadPDF(api+"/pdf", alice, pdf, 201)
	h.uploadPDF(api+"/pdf", alice, pdf, 201)
	h.uploadPDF(api+"/pdf", alice, pdf+"different bytes", 409)
	_, err = h.evidence.Upload(h.ctx, service.UploadInput{
		InspectionID: inspection, Type: "doc", Filename: "second.pdf", Mime: "application/pdf", Content: strings.NewReader(pdf),
	})
	if err == nil {
		t.Fatal("sealed inspection accepted more evidence")
	}

	h.call("GET", path, nil, nil, 404)
	h.call("GET", path, bob, nil, 404)
	h.call("GET", path, admin, nil, 404)
	h.call("GET", api+"/download-url", admin, nil, 404)
	h.call("POST", "/painel/relatorios", bob, url.Values{"acao": {"disponibilizar"}, "id": {record.ReportID}}, 404)
	if strings.Contains(h.call("GET", "/painel/disponibilizados", admin, nil, 200).Body.String(), "Endereço privado") {
		t.Fatal("private report metadata escaped")
	}
	h.call("POST", "/painel/relatorios", alice, url.Values{"acao": {"disponibilizar"}, "id": {record.ReportID}}, 303)
	preview := h.call("GET", path, admin, nil, 200)
	download := h.call("GET", path+"?baixar=1", admin, nil, 200)
	if preview.Body.String() != pdf || download.Body.String() != pdf {
		t.Fatal("preview and download do not match stored PDF")
	}
	if !strings.HasPrefix(preview.Header().Get("Content-Disposition"), "inline") || !strings.HasPrefix(download.Header().Get("Content-Disposition"), "attachment") {
		t.Fatal("unexpected PDF dispositions")
	}
	h.call("GET", path, bob, nil, 404)
	h.call("POST", "/painel/relatorios", alice, url.Values{"acao": {"revogar"}, "id": {record.ReportID}}, 303)
	h.call("GET", path, admin, nil, 404)
	h.call("GET", path, alice, nil, 200)
}
