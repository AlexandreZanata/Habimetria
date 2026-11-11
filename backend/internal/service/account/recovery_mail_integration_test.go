package account_test

import (
	"context"
	"net"
	"regexp"
	"strings"
	"testing"

	"vault/internal/mail"
	"vault/internal/testutil/mailfixture"
)

func TestRecoveryMailAndNoticeIntegration(t *testing.T) {
	capture := mailfixture.New(t)
	f := setup(t)
	f.identity(t, "user")
	request, reset := f.recovery(t)
	if err := request.Request(f.ctx, "user@example.test", "192.0.2.21"); err != nil {
		t.Fatal(err)
	}
	var operation string
	if err := f.pool.QueryRow(f.ctx, `SELECT id::text FROM account_mail_outbox WHERE template='password_recovery'`).Scan(&operation); err != nil {
		t.Fatal(err)
	}
	listener, err := net.Listen("tcp", "127.0.0.1:0")
	if err != nil {
		t.Fatal(err)
	}
	closed := listener.Addr().String()
	if err = listener.Close(); err != nil {
		t.Fatal(err)
	}
	settings := mail.Settings{Address: closed, Mode: "plain", From: "sender@example.test", Origin: "http://localhost:8080"}
	sender, err := mail.NewSMTP(settings, nil)
	if err != nil {
		t.Fatal(err)
	}
	result, err := delivery(t, f, sender).Drain(f.ctx, 10)
	if err != nil || result.Retried != 1 {
		t.Fatal("SMTP outage lost recovery", err)
	}
	f.exec(t, `UPDATE account_mail_outbox SET next_attempt_at=now() WHERE state='pending'`)
	settings.Address = capture.SMTP
	sender, err = mail.NewSMTP(settings, nil)
	if err != nil {
		t.Fatal(err)
	}
	result, err = delivery(t, f, sender).Drain(f.ctx, 10)
	if err != nil || result.Sent != 1 {
		t.Fatal("SMTP retry did not deliver", err)
	}
	message := capture.FindKind(t, operation, "localhost", "password-recovery")
	match := regexp.MustCompile(`/painel/redefinir#recuperar=([A-Za-z0-9_-]{43})`).FindStringSubmatch(message.Text)
	if len(match) != 2 || len(message.To) != 1 || message.To[0].Address != "user@example.test" {
		t.Fatal("actual recovery message differs")
	}
	if err := reset.Reset(f.ctx, match[1], replacement, replacement); err != nil {
		t.Fatal(err)
	}
	if err := f.pool.QueryRow(f.ctx, `SELECT id::text FROM account_mail_outbox WHERE template='password_changed'`).Scan(&operation); err != nil {
		t.Fatal(err)
	}
	result, err = delivery(t, f, sender).Drain(f.ctx, 10)
	if err != nil || result.Sent != 1 {
		t.Fatal("security notice not delivered", err)
	}
	message = capture.FindKind(t, operation, "localhost", "password-changed")
	if strings.Contains(message.Text, match[1]) || strings.Contains(message.Text, replacement) || !strings.Contains(message.Text, "sessões anteriores") {
		t.Fatal("unsafe security notice")
	}
}

func TestRecoveryNoticeRecipientAndExpiryIntegration(t *testing.T) {
	f := setup(t)
	f.identity(t, "user")
	request, reset := f.recovery(t)
	if err := request.Request(f.ctx, "user@example.test", "192.0.2.22"); err != nil {
		t.Fatal(err)
	}
	proof := f.recoveryProof(t)
	if err := reset.Reset(f.ctx, proof, replacement, replacement); err != nil {
		t.Fatal(err)
	}
	f.exec(t, `UPDATE users SET email='changed@example.test' WHERE email='user@example.test'`)
	sender := senderFunc(func(context.Context, string, string, string) error {
		t.Fatal("changed-recipient notice was sent")
		return nil
	})
	result, err := delivery(t, f, sender).Drain(f.ctx, 10)
	if err != nil || result.Failed != 1 {
		t.Fatal("changed recipient was not rejected", err)
	}
	if err := request.Request(f.ctx, "changed@example.test", "192.0.2.23"); err != nil {
		t.Fatal(err)
	}
	f.exec(t, `UPDATE account_password_recoveries SET created_at=created_at-interval '31 minutes',expires_at=expires_at-interval '31 minutes'`)
	result, err = delivery(t, f, sender).Drain(f.ctx, 10)
	if err != nil || result.Failed != 1 {
		t.Fatal("expired recovery sent", err)
	}
	var count int
	if err := f.pool.QueryRow(f.ctx, `SELECT count(*) FROM account_password_recoveries`).Scan(&count); err != nil || count != 0 {
		t.Fatal("expired recovery not pruned", err)
	}
}
