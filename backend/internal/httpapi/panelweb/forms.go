package panelweb

import (
	"net/http"

	"vault/internal/service/panel"
)

func (p *Web) change(w http.ResponseWriter, r *http.Request) {
	a, ok := p.requireActor(w, r)
	if !ok || !readForm(w, r) {
		return
	}
	section := r.PathValue("section")
	ctx := r.Context()
	values := r.PostForm
	var err error
	var code string
	switch section {
	case "usuarios", "equipe":
		role := values.Get("papel")
		if section == "usuarios" {
			role = "user"
		}
		switch values.Get("acao") {
		case "convidar":
			code, err = p.service.Invite(ctx, a.ID, values.Get("email"), values.Get("nome"), role)
		case "acesso":
			id := values.Get("id")
			if panel.ValidateID(id) != nil {
				err = panel.ErrInput
			} else {
				code, err = p.service.Reissue(ctx, a.ID, id)
			}
		case "atualizar":
			state := values.Get("estado")
			if state != "ativo" && state != "suspenso" {
				err = panel.ErrInput
			} else {
				err = p.service.UpdateAccount(ctx, a.ID, values.Get("id"), role, state == "ativo", values.Get("motivo"))
			}
		default:
			err = panel.ErrInput
		}
	case "configuracoes":
		err = p.service.SetRole(ctx, a.ID, values.Get("papel"), values["permissao"], values.Get("motivo"))
	case "perfil":
		err = p.service.UpdateProfile(ctx, a.ID, values.Get("nome"))
	case "imoveis":
		if values.Get("acao") == "vistoria" {
			err = p.service.CreateInspection(ctx, a, values.Get("id"))
			if err == nil {
				p.redirect(w, r, "vistorias")
				return
			}
		} else {
			err = p.service.CreateProperty(ctx, a, values.Get("endereco"))
		}
	case "relatorios":
		action := values.Get("acao")
		if action != "disponibilizar" && action != "revogar" {
			err = panel.ErrInput
		} else {
			err = p.service.SetReportAccess(ctx, a, values.Get("id"), action == "disponibilizar")
		}
	default:
		http.NotFound(w, r)
		return
	}
	if err != nil {
		p.showSection(w, r, a, section, err, "")
		return
	}
	if code != "" {
		r.PostForm = nil
		p.showSection(w, r, a, section, nil, p.origin+"/painel/#"+code)
		return
	}
	p.redirect(w, r, section)
}
