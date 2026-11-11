package panelweb

import (
	"strings"

	"vault/internal/store/postgres"
)

func reasonLabel(reason *string) string {
	if reason == nil {
		return "Motivo não registrado"
	}
	switch *reason {
	case "access_review":
		return "Revisão de acesso"
	case "security_response":
		return "Resposta a um incidente de segurança"
	case "account_request":
		return "Solicitação do titular da conta"
	case "account_onboarding":
		return "Cadastro e ativação de conta"
	case "activation_delivery":
		return "Reenvio da ativação pendente"
	default:
		return "Motivo indisponível"
	}
}

func policyStateLabel(state postgres.PanelPolicyState) string {
	if state.Role == nil {
		return "Sem conta anterior"
	}
	label := roleLabel(*state.Role)
	if state.Active != nil {
		if *state.Active {
			return label + " · Ativo"
		}
		return label + " · Suspenso"
	}
	permissions := []string{}
	for _, option := range roleCapabilities(*state.Role) {
		for _, id := range state.Permissions {
			if option.ID == id {
				permissions = append(permissions, option.Label)
			}
		}
	}
	if len(permissions) == 0 {
		return label + " · Sem permissões"
	}
	return label + " · " + strings.Join(permissions, ", ")
}
