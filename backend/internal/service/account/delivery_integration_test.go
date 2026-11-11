package account_test

import (
	"bytes"
	"context"
	"net"
	"regexp"
	"sync"
	"sync/atomic"
	"testing"

	"vault/internal/accountproof"
	"vault/internal/mail"
	"vault/internal/service/account"
	"vault/internal/store/postgres"
	"vault/internal/testutil/mailfixture"
)

type senderFunc func(context.Context, string, string, string) error

func (f senderFunc) SendVerification(ctx context.Context, to, proof, operation string) error {
	return f(ctx, to, proof, operation)
}
func delivery(t *testing.T, f fixture, sender account.Sender) *account.Delivery {
	t.Helper()
	d, err := account.NewDelivery(postgres.NewAccountStore(f.pool), f.key, sender)
	if err != nil {
		t.Fatal(err)
	}
	return d
}
func register(t *testing.T, f fixture, email string) {
	t.Helper()
	if err := f.registration.Register(f.ctx, email, "192.0.2.20"); err != nil {
		t.Fatal(err)
	}
}

func TestAccountMailDeliveryIntegration(t *testing.T) {
	capture := mailfixture.New(t)
	f := setup(t)
	sender, err := mail.NewSMTP(mail.Settings{Address: capture.SMTP, Mode: "plain", From: "sender@example.test", Origin: "http://localhost:8080"}, nil)
	if err != nil {
		t.Fatal(err)
	}
	register(t, f, "delivered@example.test")
	d := delivery(t, f, sender)
	result, err := d.Drain(f.ctx, 20)
	if err != nil || result.Sent != 1 || result.Claimed != 1 {
		t.Fatalf("durable SMTP result: %+v %v", result, err)
	}
	var operation string
	var digest []byte
	var safe bool
	if err := f.pool.QueryRow(f.ctx, `SELECT o.id::text,v.token_hash,o.state='sent' AND o.proof_ciphertext IS NULL AND o.lease_id IS NULL
		AND o.completed_at IS NOT NULL AND NOT u.panel_active AND u.panel_password IS NULL AND u.email_verified_at IS NULL
		FROM account_mail_outbox o JOIN users u ON u.id=o.user_id JOIN account_verifications v ON v.user_id=u.id`).Scan(&operation, &digest, &safe); err != nil || !safe {
		t.Fatal("SMTP acceptance activated identity or retained raw envelope")
	}
	message := capture.Find(t, operation, "localhost")
	match := regexp.MustCompile(`#verificar=([A-Za-z0-9_-]{43})`).FindStringSubmatch(message.Text)
	if len(match) != 2 {
		t.Fatal("verification proof absent from actual SMTP capture")
	}
	hash, err := accountproof.Digest(match[1])
	if err != nil || !bytes.Equal(hash, digest) {
		t.Fatal("delivered proof differs from current digest")
	}
	var leaked bool
	if err := f.pool.QueryRow(f.ctx, `SELECT position($1 IN row_to_json(o)::text)>0 FROM account_mail_outbox o`, match[1]).Scan(&leaked); err != nil || leaked {
		t.Fatal("raw token persisted after send")
	}
	result, err = d.Drain(f.ctx, 20)
	if err != nil || result.Claimed != 0 {
		t.Fatal("completed batch replay resent mail")
	}
}

func TestAccountMailWorkersDoNotDuplicateLiveClaimsIntegration(t *testing.T) {
	f := setup(t)
	for _, email := range []string{"one@example.test", "two@example.test", "three@example.test", "four@example.test"} {
		register(t, f, email)
	}
	var calls atomic.Int32
	d := delivery(t, f, senderFunc(func(context.Context, string, string, string) error { calls.Add(1); return nil }))
	var wg sync.WaitGroup
	results := make(chan error, 8)
	for range 8 {
		wg.Add(1)
		go func() { defer wg.Done(); _, err := d.Drain(f.ctx, 10); results <- err }()
	}
	wg.Wait()
	close(results)
	for err := range results {
		if err != nil {
			t.Fatal(err)
		}
	}
	status, err := postgres.NewAccountStore(f.pool).MailStatus(f.ctx)
	if err != nil || status.Sent != 4 || status.Sending != 0 || status.Pending != 0 || calls.Load() != 4 {
		t.Fatal("workers duplicated or lost a live claim")
	}
}

func TestAccountMailSMTPOutageRecoveryIntegration(t *testing.T) {
	capture := mailfixture.New(t)
	f := setup(t)
	register(t, f, "retry@example.test")
	listener, err := net.Listen("tcp", "127.0.0.1:0")
	if err != nil {
		t.Fatal(err)
	}
	address := listener.Addr().String()
	listener.Close()
	sender, err := mail.NewSMTP(mail.Settings{Address: address, Mode: "plain", From: "sender@example.test", Origin: "http://localhost:8080"}, nil)
	if err != nil {
		t.Fatal(err)
	}
	result, err := delivery(t, f, sender).Drain(f.ctx, 1)
	if err != nil || result.Retried != 1 || result.Sent != 0 {
		t.Fatal("SMTP outage lost retry state")
	}
	var bounded bool
	if err := f.pool.QueryRow(f.ctx, `SELECT state='pending' AND attempts=1 AND proof_ciphertext IS NOT NULL
		AND lease_id IS NULL AND failure_code='transport' AND next_attempt_at BETWEEN now()+interval '25 seconds' AND now()+interval '36 seconds' FROM account_mail_outbox`).Scan(&bounded); err != nil || !bounded {
		t.Fatal("retry backoff/envelope was not durable")
	}
	if _, err := f.pool.Exec(f.ctx, `UPDATE account_mail_outbox SET next_attempt_at=now()`); err != nil {
		t.Fatal(err)
	}
	sender, err = mail.NewSMTP(mail.Settings{Address: capture.SMTP, Mode: "plain", From: "sender@example.test", Origin: "http://localhost:8080"}, nil)
	if err != nil {
		t.Fatal(err)
	}
	result, err = delivery(t, f, sender).Drain(f.ctx, 1)
	if err != nil || result.Sent != 1 {
		t.Fatal("recovered SMTP did not complete queued mail")
	}
	var operation string
	if err := f.pool.QueryRow(f.ctx, `SELECT id::text FROM account_mail_outbox WHERE attempts=2 AND state='sent'`).Scan(&operation); err != nil {
		t.Fatal(err)
	}
	capture.Find(t, operation, "localhost")
}
