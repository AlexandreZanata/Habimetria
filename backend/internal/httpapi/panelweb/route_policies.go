package panelweb

import "vault/internal/httpapi/routepolicy"

var panelRoutePolicies = []routepolicy.Rule{
	{Pattern: "GET /painel/entrar", Class: routepolicy.PublicControl},
	{Pattern: "GET /painel/cadastro", Class: routepolicy.PublicControl},
	{Pattern: "POST /painel/cadastro", Class: routepolicy.PublicControl},
	{Pattern: "GET /painel/verificar", Class: routepolicy.PublicControl},
	{Pattern: "POST /painel/verificar", Class: routepolicy.PublicControl},
	{Pattern: "GET /painel/assets/", Class: routepolicy.PublicControl},
	{Pattern: "GET /painel/{$}", Class: routepolicy.PublicControl},
	{Pattern: "POST /painel/entrar", Class: routepolicy.PublicControl},
	{Pattern: "POST /painel/ativar", Class: routepolicy.PublicControl},
	{Pattern: "POST /painel/sair", Class: routepolicy.PublicControl},
	{Pattern: "GET /painel/perfil", Class: routepolicy.AccountControl},
	{Pattern: "POST /painel/perfil", Class: routepolicy.AccountControl},
	{Pattern: "GET /painel/minha-area", Class: routepolicy.HostedOwner},
	{Pattern: "GET /painel/imoveis", Class: routepolicy.HostedOwner},
	{Pattern: "POST /painel/imoveis", Class: routepolicy.HostedOwner},
	{Pattern: "GET /painel/vistorias", Class: routepolicy.HostedOwner},
	{Pattern: "GET /painel/relatorios", Class: routepolicy.HostedOwner},
	{Pattern: "POST /painel/relatorios", Class: routepolicy.HostedOwner},
	{Pattern: "GET /painel/administracao", Class: routepolicy.StaffControl},
	{Pattern: "GET /painel/usuarios", Class: routepolicy.StaffControl},
	{Pattern: "POST /painel/usuarios", Class: routepolicy.StaffControl},
	{Pattern: "GET /painel/equipe", Class: routepolicy.StaffControl},
	{Pattern: "POST /painel/equipe", Class: routepolicy.StaffControl},
	{Pattern: "GET /painel/disponibilizados", Class: routepolicy.StaffControl},
	{Pattern: "GET /painel/atividade", Class: routepolicy.StaffControl},
	{Pattern: "GET /painel/configuracoes", Class: routepolicy.StaffControl},
	{Pattern: "POST /painel/configuracoes", Class: routepolicy.StaffControl},
	{Pattern: "GET /painel/documentos/{id}", Class: routepolicy.ReportAccess},
}
