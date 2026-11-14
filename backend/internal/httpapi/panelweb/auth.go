package panelweb

import (
	"errors"
	"net/http"

	"vault/internal/httpapi/accountauth"
	"vault/internal/service/panel"
	"vault/internal/store/postgres"
)

func (p *Web) cookieFor(value string, age int) *http.Cookie {
	return &http.Cookie{Name: p.cookie, Value: value, Path: "/", MaxAge: age,
		Secure: p.secure, HttpOnly: true, SameSite: http.SameSiteLaxMode}
}

func (p *Web) start(w http.ResponseWriter, r *http.Request) {
	a, err := p.actor(r)
	if errors.Is(err, postgres.ErrNotFound) {
		p.render(w, http.StatusOK, "login", page{})
		return
	}
	if err != nil {
		p.fail(w, err)
		return
	}
	section := "minha-area"
	if a.Role != "user" {
		section = "administracao"
	}
	p.redirect(w, r, section)
}

func (p *Web) signIn(w http.ResponseWriter, r *http.Request) {
	if !readForm(w, r) || !p.limitAuth(w, r, r.PostForm.Get("email")) {
		return
	}
	value, err := p.service.Login(r.Context(), r.PostForm.Get("email"), r.PostForm.Get("senha"))
	if errors.Is(err, postgres.ErrNotFound) {
		p.render(w, http.StatusUnauthorized, "login", page{Error: "E-mail ou senha inválidos.", Values: r.PostForm})
		return
	}
	p.finishAuth(w, r, value, err)
}

func (p *Web) activate(w http.ResponseWriter, r *http.Request) {
	if !readForm(w, r) || !p.limitAuth(w, r, "") {
		return
	}
	code := r.PostForm.Get("codigo")
	value, err := p.service.Activate(r.Context(), code, r.PostForm.Get("senha"))
	if errors.Is(err, postgres.ErrNotFound) {
		p.render(w, http.StatusUnauthorized, "login", page{Error: "Código inválido, expirado ou já usado.", Activation: true})
		return
	}
	if errors.Is(err, panel.ErrPassword) {
		p.render(w, http.StatusBadRequest, "login", page{Error: "Escolha uma senha de pelo menos 15 caracteres.", Activation: true, AccessCode: code})
		return
	}
	p.finishAuth(w, r, value, err)
}

func (p *Web) limitAuth(w http.ResponseWriter, r *http.Request, email string) bool {
	err := p.service.AllowAuthentication(r.Context(), accountauth.Source(r), email)
	if errors.Is(err, postgres.ErrPanelRate) {
		w.Header().Set("Retry-After", "900")
		p.render(w, http.StatusTooManyRequests, "login", page{Error: "Muitas tentativas. Aguarde 15 minutos e tente novamente."})
		return false
	}
	if err != nil {
		p.fail(w, err)
		return false
	}
	return true
}

func (p *Web) finishAuth(w http.ResponseWriter, r *http.Request, value string, err error) {
	if err != nil {
		p.fail(w, err)
		return
	}
	http.SetCookie(w, p.cookieFor(value, 12*60*60))
	http.Redirect(w, r, "/painel/", http.StatusSeeOther)
}

func (p *Web) signOut(w http.ResponseWriter, r *http.Request) {
	if err := p.service.Logout(r.Context(), sessionValue(r, p.cookie)); err != nil {
		p.fail(w, err)
		return
	}
	http.SetCookie(w, p.cookieFor("", -1))
	http.Redirect(w, r, "/painel/", http.StatusSeeOther)
}
