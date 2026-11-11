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

func (p *Web) recoverAccount(w http.ResponseWriter, r *http.Request) {
	if p.recovery == nil {
		p.accountUnavailable(w)
		return
	}
	data := page{Title: "Recuperar senha"}
	status := 200
	if r.Method == "POST" {
		if !readForm(w, r) {
			return
		}
		err := p.recovery.Request(r.Context(), r.PostForm.Get("email"), accountauth.Source(r))
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
	p.render(w, status, "recover", data)
}

func (p *Web) resetPassword(w http.ResponseWriter, r *http.Request) {
	if p.passwordReset == nil {
		p.accountUnavailable(w)
		return
	}
	data := page{Title: "Redefinir senha"}
	status := 200
	if r.Method == "POST" {
		if !readForm(w, r) || !p.limitAuth(w, r, "") {
			return
		}
		code := strings.TrimSpace(r.PostForm.Get("codigo"))
		err := p.passwordReset.Reset(r.Context(), code, r.PostForm.Get("senha"), r.PostForm.Get("confirmacao"))
		switch {
		case errors.Is(err, credential.ErrPolicy):
			data.Error = "Use pelo menos 15 caracteres. Se a senha for muito longa, reduza o tamanho."
			data.AccessCode = code
			status = 400
		case errors.Is(err, account.ErrConfirmation):
			data.Error = "As senhas não são iguais. Digite a mesma senha nos dois campos."
			data.AccessCode = code
			status = 400
		case errors.Is(err, postgres.ErrNotFound):
			data.Error = "Link inválido, expirado ou já usado. Solicite outro link de recuperação."
			status = 401
		case err != nil:
			p.accountUnavailable(w)
			return
		default:
			data.Completed = true
		}
	}
	p.render(w, status, "reset-password", data)
}
