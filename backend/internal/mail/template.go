package mail

import (
	"bytes"
	"fmt"
	"io"
	"mime/quotedprintable"
	stdmail "net/mail"
	"net/url"

	"vault/internal/accountproof"

	"github.com/google/uuid"
)

func message(settings Settings, to, token, operation string) ([]byte, error) {
	return accountMessage(settings, AccountMessage{"email_verification", to, token, operation})
}

func accountMessage(settings Settings, m AccountMessage) ([]byte, error) {
	if !validAddress(m.Recipient) {
		return nil, failure("rejected", false, ErrConfiguration)
	}
	if m.Template != "password_changed" {
		if _, err := accountproof.Digest(m.Token); err != nil {
			return nil, failure("invalid_proof", false, err)
		}
	}
	id, err := uuid.Parse(m.Operation)
	if err != nil || id.String() != m.Operation {
		return nil, failure("invalid_proof", false, ErrConfiguration)
	}
	u, _ := url.Parse(settings.Origin) // Validated by NewSMTP.
	subject, identity, text, err := accountCopy(settings.Origin, m)
	if err != nil {
		return nil, err
	}
	var out bytes.Buffer
	fmt.Fprintf(&out, "From: %s\r\nTo: %s\r\nSubject: %s\r\n", (&stdmail.Address{Name: "Habimetria", Address: settings.From}).String(), m.Recipient, subject)
	fmt.Fprintf(&out, "Message-ID: <%s.%s@%s>\r\nMIME-Version: 1.0\r\nContent-Type: text/plain; charset=UTF-8\r\nContent-Transfer-Encoding: quoted-printable\r\n\r\n", m.Operation, identity, u.Hostname())
	body := quotedprintable.NewWriter(&out)
	_, err = io.WriteString(body, text)
	if err != nil {
		return nil, failure("invalid_proof", false, err)
	}
	if err := body.Close(); err != nil {
		return nil, failure("invalid_proof", false, err)
	}
	return out.Bytes(), nil
}
