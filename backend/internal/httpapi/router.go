package httpapi

import (
	"encoding/json"
	"net/http"

	"vault/internal/service"
)

// Deps carries service-layer dependencies. Handlers stay thin.
type Deps struct {
	Health     *service.HealthService
	Property   *service.PropertyService
	Tenancy    *service.TenancyService
	Inspection *service.InspectionService
	Room       *service.RoomService
	Evidence   *service.EvidenceService
}

// NewRouter wires MVP routes.
func NewRouter(d Deps) *http.ServeMux {
	mux := http.NewServeMux()
	hh := NewHealthHandler(d.Health)
	mux.HandleFunc("GET /healthz", hh.Live)
	mux.HandleFunc("GET /readyz", hh.Ready)

	ph := NewPropertyHandler(d.Property)
	mux.HandleFunc("POST /api/v1/properties", ph.Create)

	th := NewTenancyHandler(d.Tenancy)
	mux.HandleFunc("POST /api/v1/properties/{id}/tenancies", th.Create)

	ih := NewInspectionHandler(d.Inspection)
	mux.HandleFunc("POST /api/v1/tenancies/{id}/inspections", ih.Create)

	rh := NewRoomHandler(d.Room)
	mux.HandleFunc("POST /api/v1/inspections/{id}/rooms", rh.Create)
	mux.HandleFunc("GET /api/v1/inspections/{id}/rooms", rh.List)
	mux.HandleFunc("POST /api/v1/inspections/{id}/rooms:ensure-template", rh.EnsureTemplate)

	eh := NewEvidenceHandler(d.Evidence)
	mux.HandleFunc("POST /api/v1/inspections/{id}/evidences", eh.Upload)
	mux.HandleFunc("GET /api/v1/inspections/{id}/evidences/download-url", eh.DownloadURL)

	mux.HandleFunc("GET /v/{code}", handleVerify)
	mux.HandleFunc("GET /api/v1/ping", func(w http.ResponseWriter, r *http.Request) {
		writeJSON(w, http.StatusOK, map[string]string{"ping": "pong"})
	})
	return mux
}

// handleVerify is the public verification contract (no auth).
// Full impl in S8: lookup reports by verify_code, return hashes.
func handleVerify(w http.ResponseWriter, r *http.Request) {
	writeJSON(w, http.StatusOK, map[string]any{
		"authentic":       false,
		"verify_code":     r.PathValue("code"),
		"note":            "S8: lookup report_sha256 + manifest_sha256 + sealed_at",
		"evidence_count":  0,
		"report_sha256":   nil,
		"manifest_sha256": nil,
	})
}

func writeJSON(w http.ResponseWriter, code int, v any) {
	w.Header().Set("Content-Type", "application/json")
	w.WriteHeader(code)
	_ = json.NewEncoder(w).Encode(v)
}
