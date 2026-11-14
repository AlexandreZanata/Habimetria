package panelweb_test

import (
	"encoding/json"
	"errors"
	"net/http"
	"net/http/httptest"
	"net/url"
	"strings"
	"testing"

	"vault/internal/service"
	"vault/internal/store/postgres"
)

func (h *harness) sealRequest(inspection string, cookie *http.Cookie, body string) *httptest.ResponseRecorder {
	h.t.Helper()
	req := httptest.NewRequest("POST", testOrigin+"/api/v1/inspections/"+inspection+"/seal", strings.NewReader(body)).WithContext(h.ctx)
	req.Header.Set("Origin", testOrigin)
	req.Header.Set("Content-Type", "application/json")
	if cookie != nil {
		req.AddCookie(cookie)
	}
	out := httptest.NewRecorder()
	h.handler.ServeHTTP(out, req)
	return out
}

func TestSealWithoutPaymentIntegration(t *testing.T) {
	h := integration(t)
	owner, ownerID := h.primary()
	other, _ := h.invite(owner, "user", "other-seal@example.test")
	for _, body := range []string{`{}`, `{"payment_proof":"unpaid-legacy-proof"}`} {
		t.Run(body, func(t *testing.T) {
			address := "Synthetic seal fixture " + body
			h.call("POST", "/painel/imoveis", owner, url.Values{"endereco": {address}}, 303)
			var property, inspection string
			require(t, h.pool.QueryRow(h.ctx, `SELECT id::text FROM properties WHERE owner_user_id=$1 AND address=$2`, ownerID, address).Scan(&property))
			h.call("POST", "/painel/imoveis", owner, url.Values{"acao": {"vistoria"}, "id": {property}}, 303)
			require(t, h.pool.QueryRow(h.ctx, `SELECT i.id::text FROM inspections i JOIN tenancies t ON t.id=i.tenancy_id WHERE t.property_id=$1`, property).Scan(&inspection))
			for _, cookie := range []*http.Cookie{nil, other} {
				if got := h.sealRequest(inspection, cookie, body).Code; got != 404 {
					t.Fatalf("unauthorized seal returned %d", got)
				}
			}
			if got := h.sealRequest(inspection, owner, body).Code; got != 422 {
				t.Fatalf("empty inspection returned %d", got)
			}
			const original = "%PDF-1.4\n% synthetic seal evidence\n%%EOF\n"
			_, err := h.evidence.Upload(h.ctx, service.UploadInput{
				InspectionID: inspection, Type: "doc", Filename: "fixture.pdf", Mime: "application/pdf", Content: strings.NewReader(original),
			})
			require(t, err)
			if got := h.sealRequest(inspection, owner, `{`).Code; got != 400 {
				t.Fatalf("malformed seal returned %d", got)
			}

			results := make(chan *httptest.ResponseRecorder, 2)
			for range 2 {
				go func() { results <- h.sealRequest(inspection, owner, body) }()
			}
			created, conflicts := 0, 0
			for range 2 {
				out := <-results
				switch out.Code {
				case http.StatusCreated:
					created++
					var result service.SealOutput
					require(t, json.Unmarshal(out.Body.Bytes(), &result))
					if result.Status != "SEALED" || result.EvidenceCount != 1 || result.ManifestSHA256 == "" || result.ReportID == "" {
						t.Fatalf("incomplete seal acknowledgment: %+v", result)
					}
				case http.StatusConflict:
					conflicts++
				default:
					t.Errorf("concurrent seal returned %d: %s", out.Code, out.Body.String())
				}
			}
			if created != 1 || conflicts != 1 {
				t.Fatalf("seal race: created=%d conflicts=%d", created, conflicts)
			}
			var before, after string
			require(t, h.pool.QueryRow(h.ctx, `SELECT body::text FROM manifests WHERE inspection_id=$1`, inspection).Scan(&before))
			if got := h.sealRequest(inspection, owner, `{"payment_proof":"different-proof"}`).Code; got != 409 {
				t.Fatalf("sealed retry returned %d", got)
			}
			_, err = h.evidence.Upload(h.ctx, service.UploadInput{
				InspectionID: inspection, Type: "doc", Filename: "late.pdf", Mime: "application/pdf", Content: strings.NewReader(original),
			})
			if !errors.Is(err, postgres.ErrSealed) {
				t.Fatalf("sealed upload: want ErrSealed, got %v", err)
			}
			require(t, h.pool.QueryRow(h.ctx, `SELECT body::text FROM manifests WHERE inspection_id=$1`, inspection).Scan(&after))
			if before != after {
				t.Fatal("retry changed the frozen manifest")
			}
			var count int
			require(t, h.pool.QueryRow(h.ctx, `SELECT count(*) FROM reports WHERE inspection_id=$1`, inspection).Scan(&count))
			if count != 1 {
				t.Fatalf("seal produced %d reports", count)
			}
		})
	}
}
