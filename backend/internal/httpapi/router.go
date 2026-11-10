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
	Finding    *service.FindingService
	Coverage   *service.CoverageService
	Seal       *service.SealService
	Report     *service.ReportService
	Share      *service.ShareService
	Occurrence *service.OccurrenceService
	Compare    *service.CompareService
	HMACKey    string
	Panel      http.Handler
	Guard      func(string, http.HandlerFunc) http.HandlerFunc
}

// NewRouter wires MVP routes.
func NewRouter(d Deps) *http.ServeMux {
	mux := http.NewServeMux()
	protected := func(pattern, scope string, handler http.HandlerFunc) {
		if d.Guard != nil {
			handler = d.Guard(scope, handler)
		}
		mux.HandleFunc(pattern, handler)
	}
	if d.Panel != nil {
		mux.Handle("/painel/", d.Panel)
	}
	hh := NewHealthHandler(d.Health)
	mux.HandleFunc("GET /healthz", hh.Live)
	mux.HandleFunc("GET /readyz", hh.Ready)

	ph := NewPropertyHandler(d.Property)
	protected("POST /api/v1/properties", "properties", ph.Create)

	th := NewTenancyHandler(d.Tenancy)
	protected("POST /api/v1/properties/{id}/tenancies", "properties", th.Create)

	ih := NewInspectionHandler(d.Inspection)
	protected("POST /api/v1/tenancies/{id}/inspections", "tenancies", ih.Create)

	rh := NewRoomHandler(d.Room)
	protected("POST /api/v1/inspections/{id}/rooms", "inspections", rh.Create)
	protected("GET /api/v1/inspections/{id}/rooms", "inspections", rh.List)
	protected("POST /api/v1/inspections/{id}/rooms:ensure-template", "inspections", rh.EnsureTemplate)

	eh := NewEvidenceHandler(d.Evidence)
	protected("POST /api/v1/inspections/{id}/evidences", "inspections", eh.Upload)
	protected("GET /api/v1/inspections/{id}/evidences", "inspections", eh.List)
	protected("GET /api/v1/inspections/{id}/evidences/download-url", "inspections", eh.DownloadURL)

	fh := NewFindingHandler(d.Finding)
	protected("POST /api/v1/rooms/{id}/findings", "rooms", fh.Create)
	protected("GET /api/v1/rooms/{id}/findings", "rooms", fh.List)

	ch := NewCoverageHandler(d.Coverage)
	protected("GET /api/v1/inspections/{id}/coverage", "inspections", ch.Get)

	sh := NewSealHandler(d.Seal)
	protected("POST /api/v1/inspections/{id}/seal", "inspections", sh.Seal)

	rp := NewReportHandler(d.Report)
	protected("GET /api/v1/reports/{id}", "reports", rp.Detail)
	protected("POST /api/v1/reports/{id}/pdf", "reports", rp.Attach)
	protected("GET /api/v1/reports/{id}/download-url", "reports", rp.DownloadURL)

	vh := NewVerifyHandler(d.Report, d.HMACKey)
	mux.HandleFunc("GET /v/{code}", vh.Verify)

	shr := NewShareHandler(d.Share)
	protected("POST /api/v1/reports/{id}/shares", "reports", shr.Create)
	protected("GET /api/v1/reports/{id}/shares", "reports", shr.Timeline)
	mux.HandleFunc("GET /s/{token}", shr.Open)
	mux.HandleFunc("GET /s/{token}/pdf", shr.DownloadPDF)

	oc := NewOccurrenceHandler(d.Occurrence)
	protected("POST /api/v1/properties/{id}/occurrences", "properties", oc.Create)
	protected("GET /api/v1/properties/{id}/occurrences", "properties", oc.List)
	protected("POST /api/v1/occurrences/{id}/evidences", "occurrences", oc.Attach)
	protected("GET /api/v1/occurrences/{id}/evidences", "occurrences", oc.ListEvidences)

	cmp := NewCompareHandler(d.Compare)
	protected("POST /api/v1/tenancies/{id}/comparisons", "tenancies", cmp.Upsert)
	protected("GET /api/v1/tenancies/{id}/comparisons", "tenancies", cmp.List)

	mux.HandleFunc("GET /api/v1/ping", func(w http.ResponseWriter, r *http.Request) {
		writeJSON(w, http.StatusOK, map[string]string{"ping": "pong"})
	})
	return mux
}

func writeJSON(w http.ResponseWriter, code int, v any) {
	w.Header().Set("Content-Type", "application/json")
	w.WriteHeader(code)
	_ = json.NewEncoder(w).Encode(v)
}
