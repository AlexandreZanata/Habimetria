package account_test

import (
	"context"
	"errors"
	"testing"

	"vault/internal/mail"
	"vault/internal/service/account"
	"vault/internal/store/postgres"
	"vault/internal/testutil/mailfixture"
)

func TestAccountMailInvalidExpiredAndIneligibleIntegration(t *testing.T) {
	for _, code := range []string{"invalid_proof", "expired", "ineligible"} {
		t.Run(code, func(t *testing.T) {
			f := setup(t)
			register(t, f, "terminal@example.test")
			query := map[string]string{
				"invalid_proof": `UPDATE account_mail_outbox SET proof_ciphertext=decode(repeat('ff',61),'hex')`,
				"expired":       `UPDATE account_verifications SET created_at=now()-interval '31 minutes',expires_at=now()-interval '1 minute'`,
				"ineligible":    `UPDATE users SET email_verified_at=now(),panel_active=true`,
			}[code]
			if _, err := f.pool.Exec(f.ctx, query); err != nil {
				t.Fatal(err)
			}
			d := delivery(t, f, senderFunc(func(context.Context, string, string, string) error { t.Error("invalid proof reached SMTP"); return nil }))
			result, err := d.Drain(f.ctx, 1)
			if err != nil || result.Failed != 1 {
				t.Fatalf("terminal proof result: %+v %v", result, err)
			}
			var reason string
			var erased bool
			if err := f.pool.QueryRow(f.ctx, `SELECT failure_code,state='failed' AND proof_ciphertext IS NULL AND completed_at IS NOT NULL FROM account_mail_outbox`).Scan(&reason, &erased); err != nil || reason != code || !erased {
				t.Fatal("terminal proof state differs")
			}
		})
	}
}

func TestAccountMailBoundedRetryAndInterruptedTransportIntegration(t *testing.T) {
	f := setup(t)
	register(t, f, "retry-limit@example.test")
	d := delivery(t, f, senderFunc(func(context.Context, string, string, string) error {
		return errors.New("synthetic private vendor text")
	}))
	for attempt := 1; attempt <= 5; attempt++ {
		result, err := d.Drain(f.ctx, 1)
		if err != nil || (attempt < 5 && result.Retried != 1) || (attempt == 5 && result.Failed != 1) {
			t.Fatal("retry attempts not bounded")
		}
		if _, err := f.pool.Exec(f.ctx, `UPDATE account_mail_outbox SET next_attempt_at=now()`); err != nil {
			t.Fatal(err)
		}
	}
	if result, err := d.Drain(f.ctx, 1); err != nil || result.Claimed != 0 {
		t.Fatal("terminal failure replayed")
	}
	if _, err := d.Drain(f.ctx, 0); !errors.Is(err, account.ErrInput) {
		t.Fatal("unbounded batch accepted")
	}
	if _, err := account.NewDelivery(nil, f.key, dSender()); !errors.Is(err, account.ErrInput) {
		t.Fatal("missing delivery store accepted")
	}
	if _, err := account.NewDelivery(postgres.NewAccountStore(f.pool), nil, dSender()); !errors.Is(err, account.ErrInput) {
		t.Fatal("missing delivery key accepted")
	}
	if _, err := account.NewDelivery(postgres.NewAccountStore(f.pool), f.key, nil); !errors.Is(err, account.ErrInput) {
		t.Fatal("missing delivery adapter accepted")
	}
	register(t, f, "interrupted@example.test")
	ctx, cancel := context.WithCancel(f.ctx)
	d = delivery(t, f, senderFunc(func(context.Context, string, string, string) error { cancel(); return context.Canceled }))
	if _, err := d.Drain(ctx, 1); !errors.Is(err, context.Canceled) {
		t.Fatal("interrupted SMTP falsely completed")
	}
	var reclaimable bool
	if err := f.pool.QueryRow(f.ctx, `SELECT state='sending' AND proof_ciphertext IS NOT NULL FROM account_mail_outbox WHERE state='sending'`).Scan(&reclaimable); err != nil || !reclaimable {
		t.Fatal("interrupted proof lost lease recovery")
	}
}
func dSender() account.Sender {
	return senderFunc(func(context.Context, string, string, string) error { return nil })
}

func TestAccountMailPermanentSMTPRejectionIntegration(t *testing.T) {
	capture := mailfixture.New(t)
	f := setup(t)
	register(t, f, "rejected@example.invalid")
	sender, err := mail.NewSMTP(mail.Settings{Address: capture.SMTP, Mode: "plain", From: "sender@example.test", Origin: "http://localhost:8080"}, nil)
	if err != nil {
		t.Fatal(err)
	}
	result, err := delivery(t, f, sender).Drain(f.ctx, 1)
	if err != nil || result.Failed != 1 {
		t.Fatal("permanent SMTP rejection retried")
	}
	var safe bool
	if err := f.pool.QueryRow(f.ctx, `SELECT failure_code='rejected' AND proof_ciphertext IS NULL FROM account_mail_outbox`).Scan(&safe); err != nil || !safe {
		t.Fatal("SMTP rejection lost safe terminal state")
	}
}
