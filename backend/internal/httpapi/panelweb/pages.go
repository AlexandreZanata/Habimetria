package panelweb

import (
	"encoding/csv"
	"errors"
	"net/http"
	"net/url"
	"strconv"
	"strings"

	"vault/internal/service/panel"
	"vault/internal/store/postgres"
)

type page struct {
	Activation                                   bool
	SignupEnabled, Completed                     bool
	AccessCode                                   string
	Actor                                        postgres.PanelActor
	Section, Title, Subtitle, Error, Link, Query string
	Values                                       url.Values
	Metrics                                      postgres.PanelMetrics
	Accounts                                     []postgres.PanelAccount
	Roles                                        []postgres.PanelRole
	Events                                       []postgres.PanelEvent
	Properties                                   []postgres.Property
	Inspections                                  []postgres.PanelInspection
	Reports                                      []postgres.PanelReport
	Next, Previous, CSV                          string
}

type sectionCopy struct{ title, subtitle string }

var sections = map[string]sectionCopy{
	"administracao":    {"Visão geral", "Acompanhe a operação e os documentos disponibilizados."},
	"usuarios":         {"Usuários", "Consulte contas, gere acessos e gerencie o estado cadastral."},
	"equipe":           {"Equipe administrativa", "Administre os papéis e o acesso à plataforma."},
	"disponibilizados": {"Relatórios disponibilizados", "Consulte os PDFs que os titulares disponibilizaram à equipe."},
	"atividade":        {"Atividade", "Consulte os eventos administrativos registrados pelo servidor."},
	"configuracoes":    {"Configurações", "Defina o que cada perfil pode acessar no painel."},
	"minha-area":       {"Minha área", "Acompanhe seus imóveis, vistorias e relatórios no servidor."},
	"imoveis":          {"Meus imóveis", "Cadastre os imóveis que você quer acompanhar pela web."},
	"vistorias":        {"Minhas vistorias", "Consulte os rascunhos e as vistorias seladas do seu acervo web."},
	"relatorios":       {"Meus relatórios", "Abra seus PDFs e controle a disponibilização à equipe."},
	"perfil":           {"Minha conta", "Consulte seu e-mail de acesso e atualize seu nome."},
}

func (p *Web) requireActor(w http.ResponseWriter, r *http.Request) (postgres.PanelActor, bool) {
	a, err := p.actor(r)
	if errors.Is(err, postgres.ErrNotFound) {
		http.Redirect(w, r, "/painel/", http.StatusSeeOther)
		return a, false
	}
	if err != nil {
		p.fail(w, err)
		return a, false
	}
	return a, true
}

func (p *Web) section(w http.ResponseWriter, r *http.Request) {
	a, ok := p.requireActor(w, r)
	if !ok {
		return
	}
	p.showSection(w, r, a, r.PathValue("section"), nil, "")
}

func (p *Web) showSection(w http.ResponseWriter, r *http.Request, a postgres.PanelActor, section string, problem error, link string) {
	copy, ok := sections[section]
	if !ok {
		p.fail(w, postgres.ErrNotFound)
		return
	}
	query := strings.TrimSpace(r.URL.Query().Get("busca"))
	if len(query) > 200 {
		p.fail(w, panel.ErrInput)
		return
	}
	d := page{Actor: a, Section: section, Title: copy.title, Subtitle: copy.subtitle, Values: r.PostForm, Link: link, Query: query}
	ctx := r.Context()
	offset := pageOffset(r)
	var err error
	switch section {
	case "administracao", "minha-area":
		d.Metrics, err = p.service.Overview(ctx, a, section == "administracao")
	case "usuarios", "equipe":
		d.Accounts, err = p.service.Accounts(ctx, a, section == "equipe", query, offset)
		d.Accounts = pageRows(d.Accounts, &d, r, offset)
		csvQuery := r.URL.Query()
		csvQuery.Set("formato", "csv")
		d.CSV = "?" + csvQuery.Encode()
		if err == nil && r.Method == "GET" && r.URL.Query().Get("formato") == "csv" {
			writeAccountCSV(w, d.Accounts)
			return
		}
	case "configuracoes":
		d.Roles, err = p.service.Roles(ctx, a)
	case "atividade":
		d.Events, err = p.service.Activity(ctx, a, offset)
		d.Events = pageRows(d.Events, &d, r, offset)
	case "imoveis":
		d.Properties, err = p.service.Properties(ctx, a, offset)
		d.Properties = pageRows(d.Properties, &d, r, offset)
	case "vistorias":
		d.Inspections, err = p.service.Inspections(ctx, a, offset)
		d.Inspections = pageRows(d.Inspections, &d, r, offset)
	case "relatorios", "disponibilizados":
		d.Reports, err = p.service.Reports(ctx, a, section == "disponibilizados", offset)
		d.Reports = pageRows(d.Reports, &d, r, offset)
	}
	if err != nil {
		p.fail(w, err)
		return
	}
	status := http.StatusOK
	if problem != nil {
		status, d.Error = issue(problem)
		if status >= 500 {
			p.fail(w, problem)
			return
		}
	}
	p.render(w, status, "shell", d)
}

func pageRows[T any](rows []T, d *page, r *http.Request, offset int) []T {
	query := r.URL.Query()
	query.Del("formato")
	number := offset/25 + 1
	if number > 1 {
		query.Set("pagina", strconv.Itoa(number-1))
		d.Previous = "?" + query.Encode()
	}
	if len(rows) > 25 {
		query.Set("pagina", strconv.Itoa(number+1))
		d.Next = "?" + query.Encode()
		return rows[:25]
	}
	return rows
}

func writeAccountCSV(w http.ResponseWriter, accounts []postgres.PanelAccount) {
	w.Header().Set("Content-Type", "text/csv; charset=utf-8")
	w.Header().Set("Content-Disposition", `attachment; filename="contas-habimetria.csv"`)
	c := csv.NewWriter(w)
	_ = c.Write([]string{"Nome", "E-mail", "Perfil", "Estado", "Cadastro"})
	for _, a := range accounts {
		state := "Suspenso"
		if a.Active {
			state = "Ativo"
		}
		_ = c.Write([]string{csvCell(a.Name), csvCell(a.Email), roleLabel(a.Role), state, date(a.CreatedAt)})
	}
	c.Flush()
}

func csvCell(value string) string {
	trimmed := strings.TrimSpace(value)
	if strings.HasPrefix(value, "\t") || strings.HasPrefix(value, "\r") ||
		strings.HasPrefix(value, "\n") || (trimmed != "" && strings.ContainsRune("=+-@", rune(trimmed[0]))) {
		return "'" + value
	}
	return value
}

func roleLabel(role string) string {
	switch role {
	case "owner":
		return "Proprietário"
	case "admin":
		return "Administrador"
	case "support":
		return "Atendimento"
	default:
		return "Usuário"
	}
}

func eventLabel(event string) string {
	switch event {
	case "signed_in":
		return "Acesso autenticado"
	case "email_verified":
		return "E-mail confirmado"
	case "account_created":
		return "Conta cadastrada"
	case "account_updated":
		return "Conta atualizada"
	case "access_issued":
		return "Link de acesso gerado"
	case "permissions_updated":
		return "Permissões atualizadas"
	case "profile_updated":
		return "Perfil atualizado"
	default:
		return "Evento da plataforma"
	}
}
