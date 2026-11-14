package panelweb

func eventLabel(event string) string {
	switch event {
	case "signed_in":
		return "Acesso autenticado"
	case "email_verified":
		return "E-mail confirmado"
	case "password_reset":
		return "Senha redefinida pelo titular"
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
