package mail

import (
	"bytes"
	"context"
	"crypto/tls"
	"fmt"
	"io"
	"net"
	"net/smtp"
	"time"
)

type SMTP struct {
	settings Settings
	password []byte
}

func (s *SMTP) Format(state fmt.State, _ rune) {
	_, _ = io.WriteString(state, "[account SMTP redacted]")
}
func (s *SMTP) MarshalJSON() ([]byte, error) { return []byte(`"[account SMTP redacted]"`), nil }

func NewSMTP(settings Settings, password []byte) (*SMTP, error) {
	if settings.Validate() != nil || len(password) > 4096 || bytes.ContainsAny(password, "\r\n\x00") || ((settings.Username == "") != (len(password) == 0)) ||
		(settings.Mode == "plain" && len(password) != 0) {
		return nil, ErrConfiguration
	}
	return &SMTP{settings, bytes.Clone(password)}, nil
}

func (s *SMTP) SendVerification(ctx context.Context, to, token, operation string) error {
	return s.SendAccount(ctx, AccountMessage{"email_verification", to, token, operation})
}

func (s *SMTP) SendAccount(ctx context.Context, message AccountMessage) error {
	payload, err := accountMessage(s.settings, message)
	if err != nil {
		return err
	}
	ctx, cancel := context.WithTimeout(ctx, 15*time.Second)
	defer cancel()
	host, _, _ := net.SplitHostPort(s.settings.Address)
	connection, err := (&net.Dialer{Timeout: 5 * time.Second}).DialContext(ctx, "tcp", s.settings.Address)
	if err != nil {
		return protocolFailure(err)
	}
	defer connection.Close() // Closing after SMTP acceptance cannot retract DATA.
	stop := context.AfterFunc(ctx, func() { _ = connection.Close() })
	defer stop()
	deadline, _ := ctx.Deadline()
	if err := connection.SetDeadline(deadline); err != nil {
		return protocolFailure(err)
	}
	tlsConfig := &tls.Config{MinVersion: tls.VersionTLS12, ServerName: host, RootCAs: s.settings.Roots}
	var transport net.Conn = connection
	if s.settings.Mode == "tls" {
		secured := tls.Client(connection, tlsConfig)
		if err := secured.HandshakeContext(ctx); err != nil {
			return secureFailure(err)
		}
		transport = secured
	}
	client, err := smtp.NewClient(transport, host)
	if err != nil {
		return protocolFailure(err)
	}
	defer client.Close()
	if s.settings.Mode == "starttls" {
		if ok, _ := client.Extension("STARTTLS"); !ok {
			return failure("tls", false, ErrConfiguration)
		}
		if err := client.StartTLS(tlsConfig); err != nil {
			return secureFailure(err)
		}
	}
	if s.settings.Username != "" {
		if err := client.Auth(smtp.PlainAuth("", s.settings.Username, string(s.password), host)); err != nil {
			return protocolFailure(err)
		}
	}
	if err := client.Mail(s.settings.From); err != nil {
		return protocolFailure(err)
	}
	if err := client.Rcpt(message.Recipient); err != nil {
		return protocolFailure(err)
	}
	data, err := client.Data()
	if err != nil {
		return protocolFailure(err)
	}
	if _, err := data.Write(payload); err != nil {
		return protocolFailure(err)
	}
	if err := data.Close(); err != nil {
		return protocolFailure(err)
	}
	// The server accepted DATA. A failed QUIT must not cause another delivery.
	_ = client.Quit()
	return nil
}
