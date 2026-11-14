package panelweb

import (
	"errors"
	"net/http"
	"strings"

	"vault/internal/credential"
	"vault/internal/httpapi/accountauth"
	"vault/internal/service/account"
	"vault/internal/store/postgres"
)

func (p *Web) registerAccount(w http.ResponseWriter, r *http.Request) {
	if p.registration == nil {
		p.accountUnavailable(w)
		return
	}
	data := page{Title: "Criar conta"}
	status := http.StatusOK
	if r.Method == "POST" {
		if !readForm(w, r) {
			return
		}
		err := p.registration.Register(r.Context(), r.PostForm.Get("email"), accountauth.Source(r))
		if errors.Is(err, account.ErrInput) {
			data.Error = "Confira o endereço de e-mail informado."
			data.Values = r.PostForm
			status = 400
		} else if err != nil {
			p.accountUnavailable(w)
			return
		} else {
			data.Completed = true
		}
	}
	p.render(w, status, "register", data)
}

func (p *Web) verifyAccount(w http.ResponseWriter, r *http.Request) {
	if p.verification == nil {
		p.accountUnavailable(w)
		return
	}
	data := page{Title: "Confirmar e-mail"}
	status := http.StatusOK
	if r.Method == "POST" {
		if !readForm(w, r) || !p.limitAuth(w, r, "") {
			return
		}
		code := strings.TrimSpace(r.PostForm.Get("codigo"))
		err := p.verification.Verify(r.Context(), code, r.PostForm.Get("senha"))
		switch {
		case errors.Is(err, credential.ErrPolicy):
			data.Error = "Use pelo menos 15 caracteres. Se a senha for muito longa, reduza o tamanho."
			data.AccessCode = code
			status = 400
		case errors.Is(err, postgres.ErrNotFound):
			data.Error = "Link inválido, expirado ou já usado. Solicite um novo link de confirmação."
			status = 401
		case err != nil:
			p.accountUnavailable(w)
			return
		default:
			data.Completed = true
		}
	}
	p.render(w, status, "verify", data)
}

func (p *Web) accountUnavailable(w http.ResponseWriter) {
	p.render(w, 503, "failure", page{Error: "Este serviço está indisponível no momento. Tente novamente mais tarde."})
}
