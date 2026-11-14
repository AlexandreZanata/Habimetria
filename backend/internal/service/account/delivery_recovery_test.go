package account_test

import (
	"context"
	"errors"
	"testing"

	"vault/internal/store/postgres"
)

func TestAccountMailStaleLeaseAndReplacementIntegration(t *testing.T) {
	f := setup(t)
	register(t, f, "leased@example.test")
	store := postgres.NewAccountStore(f.pool)
	first, err := store.ClaimMail(f.ctx)
	if err != nil || first == nil {
		t.Fatal("first claim absent")
	}
	if _, err := f.pool.Exec(f.ctx, `UPDATE account_mail_outbox SET lease_until=now()-interval '1 second'`); err != nil {
		t.Fatal(err)
	}
	second, err := store.ClaimMail(f.ctx)
	if err != nil || second == nil || first.Lease == second.Lease || first.ID != second.ID {
		t.Fatal("abandoned claim not rotated")
	}
	if _, err := store.CompleteMail(f.ctx, *first, "", false); !errors.Is(err, postgres.ErrAccountMailLease) {
		t.Fatal("stale worker overwrote new lease")
	}
	if _, err := store.MailPayload(f.ctx, *first); !errors.Is(err, postgres.ErrAccountMailLease) {
		t.Fatal("stale worker retained payload")
	}
	if _, err := f.pool.Exec(f.ctx, `UPDATE account_verifications SET created_at=created_at-interval '61 seconds',expires_at=expires_at-interval '61 seconds'`); err != nil {
		t.Fatal(err)
	}
	register(t, f, "leased@example.test")
	if _, err := store.CompleteMail(f.ctx, *second, "", false); !errors.Is(err, postgres.ErrAccountMailLease) {
		t.Fatal("resend left old lease authoritative")
	}
	var cancelled bool
	if err := f.pool.QueryRow(f.ctx, `SELECT state='cancelled' AND proof_ciphertext IS NULL AND lease_id IS NULL FROM account_mail_outbox WHERE id=$1`, first.ID).Scan(&cancelled); err != nil || !cancelled {
		t.Fatal("replaced in-flight proof not cancelled")
	}
}

func TestAccountMailLostCompletionAndDatabaseFailureIntegration(t *testing.T) {
	f := setup(t)
	register(t, f, "lost-result@example.test")
	var operation string
	d := delivery(t, f, senderFunc(func(_ context.Context, _, _ string, id string) error {
		operation = id
		_, err := f.pool.Exec(f.ctx, `UPDATE account_mail_outbox SET lease_until=now()-interval '1 second' WHERE id=$1`, id)
		return err
	}))
	result, err := d.Drain(f.ctx, 1)
	if err != nil || result.Lost != 1 || result.Sent != 0 {
		t.Fatal("lost completion was marked sent")
	}
	d = delivery(t, f, senderFunc(func(_ context.Context, _, _ string, id string) error {
		if id != operation {
			t.Error("reclaim changed operation Message-ID")
		}
		return nil
	}))
	if _, err := f.pool.Exec(f.ctx, `CREATE FUNCTION fixture_completion_failure() RETURNS TRIGGER LANGUAGE plpgsql AS $$
		BEGIN IF NEW.state='sent' THEN RAISE EXCEPTION 'synthetic completion outage'; END IF; RETURN NEW; END $$;
		CREATE TRIGGER fixture_completion_failure BEFORE UPDATE ON account_mail_outbox FOR EACH ROW EXECUTE FUNCTION fixture_completion_failure()`); err != nil {
		t.Fatal(err)
	}
	if _, err := d.Drain(f.ctx, 1); err == nil {
		t.Fatal("DB failure after transport falsely completed")
	}
	var recoverable bool
	if err := f.pool.QueryRow(f.ctx, `SELECT state='sending' AND proof_ciphertext IS NOT NULL FROM account_mail_outbox`).Scan(&recoverable); err != nil || !recoverable {
		t.Fatal("failed completion erased recoverable delivery")
	}
	if _, err := f.pool.Exec(f.ctx, `DROP TRIGGER fixture_completion_failure ON account_mail_outbox;
		UPDATE account_mail_outbox SET lease_until=now()-interval '1 second'`); err != nil {
		t.Fatal(err)
	}
	result, err = d.Drain(f.ctx, 1)
	if err != nil || result.Sent != 1 {
		t.Fatal("recovered completion did not settle delivery")
	}
	f.pool.Close()
	if _, err := d.Drain(f.ctx, 1); err == nil {
		t.Fatal("queue DB outage was swallowed")
	}
}

func TestAccountMailExhaustedLeaseErasureIntegration(t *testing.T) {
	f := setup(t)
	register(t, f, "abandoned@example.test")
	store := postgres.NewAccountStore(f.pool)
	for range 5 {
		lease, err := store.ClaimMail(f.ctx)
		if err != nil || lease == nil {
			t.Fatal("reclaim ended before attempt limit")
		}
		if _, err := f.pool.Exec(f.ctx, `UPDATE account_mail_outbox SET lease_until=now()-interval '1 second'`); err != nil {
			t.Fatal(err)
		}
	}
	lease, err := store.ClaimMail(f.ctx)
	if err != nil || lease != nil {
		t.Fatal("abandoned delivery exceeded claim limit")
	}
	var erased bool
	if err := f.pool.QueryRow(f.ctx, `SELECT state='failed' AND attempts=5 AND failure_code='attempt_limit'
		AND proof_ciphertext IS NULL AND lease_id IS NULL AND completed_at IS NOT NULL FROM account_mail_outbox`).Scan(&erased); err != nil || !erased {
		t.Fatal("exhausted proof retained")
	}
}
