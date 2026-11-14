package panelweb

import "vault/internal/authz"

type capabilityOption struct{ ID, Label string }

func roleCapabilities(role string) []capabilityOption {
	labels := map[string]string{
		"accounts.read": "Consultar contas", "accounts.write": "Gerenciar contas de usuários",
		"reports.read": "Consultar relatórios disponibilizados", "audit.read": "Consultar atividade administrativa",
		"commerce.read": "Consultar contexto comercial", "catalog.write": "Gerenciar catálogo",
		"pricing.review": "Revisar preços", "pricing.publish": "Publicar preços",
		"grants.write": "Gerenciar concessões comerciais", "finance.read": "Consultar finanças",
		"refunds.request": "Solicitar reembolsos", "events.replay": "Reprocessar eventos comerciais",
		"support.write": "Gerenciar atendimento", "privacy.process": "Processar solicitações de privacidade",
		"operations.write": "Gerenciar operações",
	}
	var out []capabilityOption
	for _, capability := range authz.Registry() {
		if authz.Assignable(role, capability.ID) {
			out = append(out, capabilityOption{capability.ID, labels[capability.ID]})
		}
	}
	return out
}
