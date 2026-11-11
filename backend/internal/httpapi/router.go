package httpapi

import (
	"encoding/json"
	"net/http"

	"vault/internal/httpapi/routepolicy"
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
	Account    *AccountHandler
	Guard      func(string, http.HandlerFunc) http.HandlerFunc
}

// NewRouter binds API endpoints to reviewed transport policies.
func NewRouter(d Deps) *routepolicy.Router {
	mux := routepolicy.New(apiRoutePolicies, func(rule routepolicy.Rule, next http.Handler) http.Handler {
		if rule.Class == routepolicy.AccountControl {
			return d.Account.Guard(next)
		}
		if d.Guard == nil || rule.Class != routepolicy.HostedOwner || rule.Scope == "" {
			return nil
		}
		protected := d.Guard(rule.Scope, next.ServeHTTP)
		if protected == nil {
			return nil
		}
		return protected
	})
	mux.HandleFunc("POST /api/v1/account/registrations", d.Account.Register)
	mux.HandleFunc("POST /api/v1/account/verification", d.Account.Verify)
	mux.HandleFunc("POST /api/v1/account/recovery", d.Account.Recover)
	mux.HandleFunc("POST /api/v1/account/password", d.Account.ResetPassword)
	mux.HandleFunc("POST /api/v1/account/sessions", d.Account.Login)
	mux.HandleFunc("GET /api/v1/account/profile", d.Account.Profile)
	mux.HandleFunc("DELETE /api/v1/account/sessions/current", d.Account.Logout)
	if d.Panel != nil {
		mux.Handle("/painel/", d.Panel)
	}
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
	mux.HandleFunc("GET /api/v1/inspections/{id}/evidences", eh.List)
	mux.HandleFunc("GET /api/v1/inspections/{id}/evidences/download-url", eh.DownloadURL)

	fh := NewFindingHandler(d.Finding)
	mux.HandleFunc("POST /api/v1/rooms/{id}/findings", fh.Create)
	mux.HandleFunc("GET /api/v1/rooms/{id}/findings", fh.List)

	ch := NewCoverageHandler(d.Coverage)
	mux.HandleFunc("GET /api/v1/inspections/{id}/coverage", ch.Get)

	sh := NewSealHandler(d.Seal)
	mux.HandleFunc("POST /api/v1/inspections/{id}/seal", sh.Seal)

	rp := NewReportHandler(d.Report)
	mux.HandleFunc("GET /api/v1/reports/{id}", rp.Detail)
	mux.HandleFunc("POST /api/v1/reports/{id}/pdf", rp.Attach)
	mux.HandleFunc("GET /api/v1/reports/{id}/download-url", rp.DownloadURL)

	vh := NewVerifyHandler(d.Report, d.HMACKey)
	mux.HandleFunc("GET /v/{code}", vh.Verify)

	shr := NewShareHandler(d.Share)
	mux.HandleFunc("POST /api/v1/reports/{id}/shares", shr.Create)
	mux.HandleFunc("GET /api/v1/reports/{id}/shares", shr.Timeline)
	mux.HandleFunc("GET /s/{token}", shr.Open)
	mux.HandleFunc("GET /s/{token}/pdf", shr.DownloadPDF)

	oc := NewOccurrenceHandler(d.Occurrence)
	mux.HandleFunc("POST /api/v1/properties/{id}/occurrences", oc.Create)
	mux.HandleFunc("GET /api/v1/properties/{id}/occurrences", oc.List)
	mux.HandleFunc("POST /api/v1/occurrences/{id}/evidences", oc.Attach)
	mux.HandleFunc("GET /api/v1/occurrences/{id}/evidences", oc.ListEvidences)

	cmp := NewCompareHandler(d.Compare)
	mux.HandleFunc("POST /api/v1/tenancies/{id}/comparisons", cmp.Upsert)
	mux.HandleFunc("GET /api/v1/tenancies/{id}/comparisons", cmp.List)

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
