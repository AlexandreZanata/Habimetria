package mail

import (
	"bytes"
	"fmt"
	"mime/quotedprintable"
	stdmail "net/mail"
	"net/url"

	"vault/internal/accountproof"

	"github.com/google/uuid"
)

func message(settings Settings, to, token, operation string) ([]byte, error) {
	if !validAddress(to) {
		return nil, failure("rejected", false, ErrConfiguration)
	}
	if _, err := accountproof.Digest(token); err != nil {
		return nil, failure("invalid_proof", false, err)
	}
	id, err := uuid.Parse(operation)
	if err != nil || id.String() != operation {
		return nil, failure("invalid_proof", false, ErrConfiguration)
	}
	u, _ := url.Parse(settings.Origin) // Validated by NewSMTP.
	var out bytes.Buffer
	fmt.Fprintf(&out, "From: %s\r\nTo: %s\r\nSubject: Confirme seu e-mail na Habimetria\r\n", (&stdmail.Address{Name: "Habimetria", Address: settings.From}).String(), to)
	fmt.Fprintf(&out, "Message-ID: <%s.email-verification@%s>\r\nMIME-Version: 1.0\r\nContent-Type: text/plain; charset=UTF-8\r\nContent-Transfer-Encoding: quoted-printable\r\n\r\n", operation, u.Hostname())
	body := quotedprintable.NewWriter(&out)
	_, err = fmt.Fprintf(body, "Recebemos um pedido para criar uma conta Habimetria.\r\n\r\nPara confirmar seu e-mail, abra:\r\n%s/painel/verificar#verificar=%s\r\n\r\nEste link expira em 30 minutos. A senha será definida após a confirmação.\r\nSe você não fez este pedido, ignore esta mensagem.\r\n", settings.Origin, token)
	if err != nil {
		return nil, failure("invalid_proof", false, err)
	}
	if err := body.Close(); err != nil {
		return nil, failure("invalid_proof", false, err)
	}
	return out.Bytes(), nil
}
