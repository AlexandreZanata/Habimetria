package mail

import (
	"fmt"
	"io"
)

type AccountMessage struct{ Template, Recipient, Token, Operation string }

func (m AccountMessage) Format(state fmt.State, _ rune) {
	_, _ = io.WriteString(state, "[account message redacted]")
}
func (m AccountMessage) MarshalJSON() ([]byte, error) {
	return []byte(`"[account message redacted]"`), nil
}

func accountCopy(origin string, m AccountMessage) (subject, identity, body string, err error) {
	switch m.Template {
	case "email_verification":
		return "Confirme seu e-mail na Habimetria", "email-verification",
			fmt.Sprintf("Recebemos um pedido para criar uma conta Habimetria.\r\n\r\nPara confirmar seu e-mail, abra:\r\n%s/painel/verificar#verificar=%s\r\n\r\nEste link expira em 30 minutos. A senha será definida após a confirmação.\r\nSe você não fez este pedido, ignore esta mensagem.\r\n", origin, m.Token), nil
	case "password_recovery":
		return "Redefina sua senha na Habimetria", "password-recovery",
			fmt.Sprintf("Recebemos um pedido para redefinir a senha da sua conta Habimetria.\r\n\r\nEscolha uma nova senha pelo link:\r\n%s/painel/redefinir#recuperar=%s\r\n\r\nEste link vale por 30 minutos e só pode ser usado uma vez. Se você não fez este pedido, ignore a mensagem: sua senha continua igual.\r\n", origin, m.Token), nil
	case "password_changed":
		if m.Token != "" {
			return "", "", "", failure("invalid_proof", false, ErrConfiguration)
		}
		return "Sua senha Habimetria foi alterada", "password-changed",
			fmt.Sprintf("Uma alteração de senha da sua conta Habimetria foi concluída. As sessões anteriores foram encerradas.\r\n\r\nSe você não reconhece a alteração, solicite a recuperação da sua conta:\r\n%s/painel/recuperar\r\n\r\nNunca compartilhe sua senha ou códigos de acesso.\r\n", origin), nil
	default:
		return "", "", "", failure("invalid_proof", false, ErrConfiguration)
	}
}
