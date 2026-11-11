package panelweb_test

import (
	"bytes"
	"encoding/json"
	"mime/multipart"
	"net/http"
	"net/http/httptest"
	"net/textproto"
	"testing"

	"vault/internal/config"
	"vault/internal/httpapi"
	"vault/internal/httpapi/panelweb"
	"vault/internal/service"
	"vault/internal/storage/localstore"
)

type legacyServer struct {
	*harness
	deps httpapi.Deps
}

func newLegacyServer(t *testing.T) *legacyServer {
	h := integration(t)
	store := localstore.New(t.TempDir())
	return &legacyServer{h, httpapi.Deps{
		Health: service.NewHealth(h.pool), Property: service.NewProperty(h.pool),
		Tenancy: service.NewTenancy(h.pool), Inspection: service.NewInspection(h.pool), Room: service.NewRoom(h.pool),
		Evidence: service.NewEvidence(h.pool, store), Finding: service.NewFinding(h.pool), Coverage: service.NewCoverage(h.pool),
		Seal: service.NewSeal(h.pool, "integration-test-key"), Report: service.NewReport(h.pool, store, testOrigin),
		Share: service.NewShare(h.pool, store, testOrigin, "integration-test-key"), HMACKey: "integration-test-key",
		Occurrence: service.NewOccurrence(h.pool), Compare: service.NewCompare(h.pool),
	}}
}

func (s *legacyServer) mode(mode string) {
	cfg := config.Config{Environment: "development", VerifyBaseURL: testOrigin, LegacyAPIMode: mode, HMACKey: s.deps.HMACKey}
	require(s.t, cfg.Validate())
	d := s.deps
	d.Guard = panelweb.ProtectLegacy(s.panel, "", panelweb.LegacyPolicy{
		AllowUnclaimedDevelopment: cfg.LegacyAPIMode == config.LegacyUnclaimedDevelopment,
	})
	s.handler = httpapi.SecurityHeaders(httpapi.NewRouter(d))
}

func (s *legacyServer) postID(path, body string) string {
	s.t.Helper()
	out := s.boundaryRequest("POST", path, nil, body)
	if out.Code != 201 {
		s.t.Fatalf("legacy create %s: %d %s", path, out.Code, out.Body.String())
	}
	var row struct {
		ID string `json:"id"`
	}
	require(s.t, json.Unmarshal(out.Body.Bytes(), &row))
	if row.ID == "" {
		s.t.Fatal("missing legacy row ID")
	}
	return row.ID
}

func (s *legacyServer) upload(path, room, contents string, want int) *httptest.ResponseRecorder {
	s.t.Helper()
	var body bytes.Buffer
	writer := multipart.NewWriter(&body)
	require(s.t, writer.WriteField("type", "doc"))
	if room != "" {
		require(s.t, writer.WriteField("room_id", room))
	}
	part, err := writer.CreatePart(textproto.MIMEHeader{
		"Content-Disposition": {`form-data; name="file"; filename="fixture.pdf"`}, "Content-Type": {"application/pdf"},
	})
	require(s.t, err)
	_, err = part.Write([]byte(contents))
	require(s.t, err)
	require(s.t, writer.Close())
	req := httptest.NewRequest(http.MethodPost, testOrigin+path, &body).WithContext(s.ctx)
	req.Header.Set("Content-Type", writer.FormDataContentType())
	out := httptest.NewRecorder()
	s.handler.ServeHTTP(out, req)
	if out.Code != want {
		s.t.Fatalf("legacy upload %s: %d want %d: %s", path, out.Code, want, out.Body.String())
	}
	return out
}

func newLegacyRequest(h *harness, method, id string, cookie *http.Cookie) *http.Request {
	req := httptest.NewRequest(method, testOrigin, nil).WithContext(h.ctx)
	req.SetPathValue("id", id)
	req.Header.Set("Origin", testOrigin)
	if cookie != nil {
		req.AddCookie(cookie)
	}
	return req
}

func legacyGuardResult(guard func(string, http.HandlerFunc) http.HandlerFunc, scope string, req *http.Request, t *testing.T) *httptest.ResponseRecorder {
	t.Helper()
	out := httptest.NewRecorder()
	guard(scope, func(w http.ResponseWriter, _ *http.Request) { w.WriteHeader(204) })(out, req)
	return out
}
