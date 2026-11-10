package panelweb

import (
	"errors"
	"io"
	"log"
	"net/http"

	"vault/internal/store/postgres"
)

func (p *Web) document(w http.ResponseWriter, r *http.Request) {
	a, err := p.actor(r)
	if errors.Is(err, postgres.ErrNotFound) {
		http.NotFound(w, r)
		return
	}
	if err != nil {
		p.fail(w, err)
		return
	}
	key, err := p.service.ReportKey(r.Context(), a, r.PathValue("id"))
	if err != nil {
		p.fail(w, err)
		return
	}
	bytes, err := p.storage.OpenOriginal(r.Context(), key)
	if err != nil {
		p.fail(w, err)
		return
	}
	defer bytes.Close()
	disposition := "inline"
	if r.URL.Query().Get("baixar") == "1" {
		disposition = "attachment"
	}
	w.Header().Set("Content-Type", "application/pdf")
	w.Header().Set("Content-Disposition", disposition+`; filename="relatorio-habimetria.pdf"`)
	// Native PDF viewers need their own isolated renderer. The MIME type
	// and nosniff header keep the response out of HTML interpretation.
	w.Header().Set("Content-Security-Policy", "frame-ancestors 'none'; form-action 'none'; base-uri 'none'")
	w.Header().Set("Referrer-Policy", "no-referrer")
	if r.Method == http.MethodHead {
		return
	}
	if _, err := io.Copy(w, bytes); err != nil {
		log.Printf("stream authorized panel PDF: %v", err)
	}
}
