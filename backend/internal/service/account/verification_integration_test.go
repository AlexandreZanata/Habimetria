package account_test

import (
	"errors"
	"sync"
	"testing"

	"vault/internal/credential"
	"vault/internal/service/account"
	"vault/internal/service/panel"
	"vault/internal/store/postgres"
)

const password = "verification fixture password"

func (f fixture) proof(t *testing.T, email string) string {
	t.Helper()
	var user, operation string
	var encrypted []byte
	if err := f.pool.QueryRow(f.ctx, `SELECT u.id::text,o.id::text,o.proof_ciphertext FROM users u
		JOIN account_mail_outbox o ON o.user_id=u.id WHERE u.email=$1 AND o.state='pending'`, email).Scan(&user, &operation, &encrypted); err != nil {
		t.Fatal(err)
	}
	token, err := f.key.Open(user, operation, encrypted)
	if err != nil {
		t.Fatal(err)
	}
	return token
}

func TestAtomicVerificationIntegration(t *testing.T) {
	f := setup(t)
	if err := f.registration.Register(f.ctx, "verified@example.test", "192.0.2.9"); err != nil {
		t.Fatal(err)
	}
	token := f.proof(t, "verified@example.test")
	v := account.NewVerification(postgres.NewAccountStore(f.pool))
	var wg sync.WaitGroup
	results := make(chan error, 6)
	for range 6 {
		wg.Add(1)
		go func() { defer wg.Done(); results <- v.Verify(f.ctx, token, password) }()
	}
	wg.Wait()
	close(results)
	success := 0
	for err := range results {
		if err == nil {
			success++
		} else if !errors.Is(err, postgres.ErrNotFound) {
			t.Fatal(err)
		}
	}
	if success != 1 {
		t.Fatalf("consumed %d times", success)
	}
	var active bool
	var proofs, jobs, audits, sessions int
	err := f.pool.QueryRow(f.ctx, `SELECT panel_active AND panel_role='user' AND email_verified_at>=registration_started_at,
		(SELECT count(*) FROM account_verifications),
		(SELECT count(*) FROM account_mail_outbox WHERE state='cancelled' AND proof_ciphertext IS NULL AND lease_id IS NULL),
		(SELECT count(*) FROM panel_audit WHERE event='email_verified' AND actor_id=users.id AND subject_id=users.id),
		(SELECT count(*) FROM panel_sessions) FROM users`).Scan(&active, &proofs, &jobs, &audits, &sessions)
	if err != nil || !active || proofs != 0 || jobs != 1 || audits != 1 || sessions != 0 {
		t.Fatalf("non-atomic activation: %v %v %d %d %d %d", err, active, proofs, jobs, audits, sessions)
	}
	p := panel.New(postgres.NewPanelStore(f.pool))
	session, err := p.Login(f.ctx, "VERIFIED@example.test", password)
	if err != nil {
		t.Fatal(err)
	}
	actor, err := p.Session(f.ctx, session)
	if err != nil || actor.Role != "user" || actor.Pending || actor.Can("accounts.write") {
		t.Fatal("incorrect authority after verification", err)
	}
	if _, err = p.Login(f.ctx, "verified@example.test", password+"x"); !errors.Is(err, postgres.ErrNotFound) {
		t.Fatal("wrong credential accepted")
	}
}

func TestVerificationFailureIntegration(t *testing.T) {
	f := setup(t)
	if err := f.registration.Register(f.ctx, "expiry@example.test", "192.0.2.10"); err != nil {
		t.Fatal(err)
	}
	token := f.proof(t, "expiry@example.test")
	v := account.NewVerification(postgres.NewAccountStore(f.pool))
	if err := v.Verify(f.ctx, token, "short"); !errors.Is(err, credential.ErrPolicy) {
		t.Fatal(err)
	}
	if err := v.Verify(f.ctx, "malformed", password); !errors.Is(err, postgres.ErrNotFound) {
		t.Fatal(err)
	}
	if _, err := f.pool.Exec(f.ctx, `UPDATE account_verifications SET created_at=created_at-interval '31 minutes',expires_at=expires_at-interval '31 minutes'`); err != nil {
		t.Fatal(err)
	}
	if err := v.Verify(f.ctx, token, password); !errors.Is(err, postgres.ErrNotFound) {
		t.Fatal("expired proof accepted", err)
	}
	if err := f.registration.Register(f.ctx, "expiry@example.test", "192.0.2.10"); err != nil {
		t.Fatal(err)
	}
	if err := v.Verify(f.ctx, token, password); !errors.Is(err, postgres.ErrNotFound) {
		t.Fatal("replaced proof accepted", err)
	}
	current := f.proof(t, "expiry@example.test")
	if _, err := f.pool.Exec(f.ctx, `CREATE FUNCTION reject_verification() RETURNS trigger LANGUAGE plpgsql AS $$ BEGIN RAISE EXCEPTION 'fixture failure'; END $$;
		CREATE TRIGGER reject_verification BEFORE INSERT ON panel_audit FOR EACH ROW EXECUTE FUNCTION reject_verification()`); err != nil {
		t.Fatal(err)
	}
	if err := v.Verify(f.ctx, current, password); err == nil || errors.Is(err, postgres.ErrNotFound) {
		t.Fatal("failed audit did not abort")
	}
	if f.proof(t, "expiry@example.test") != current {
		t.Fatal("proof lost on rollback")
	}
	if _, err := f.pool.Exec(f.ctx, `DROP TRIGGER reject_verification ON panel_audit`); err != nil {
		t.Fatal(err)
	}
	if err := v.Verify(f.ctx, current, password); err != nil {
		t.Fatal(err)
	}
}

func TestVerificationPurposeIntegration(t *testing.T) {
	f := setup(t)
	p := panel.New(postgres.NewPanelStore(f.pool))
	invite, err := p.Bootstrap(f.ctx, "owner@example.test", "Fixture owner")
	if err != nil {
		t.Fatal(err)
	}
	v := account.NewVerification(postgres.NewAccountStore(f.pool))
	if err = v.Verify(f.ctx, invite, password); !errors.Is(err, postgres.ErrNotFound) {
		t.Fatal("panel proof verified email", err)
	}
	if err = f.registration.Register(f.ctx, "pending@example.test", "192.0.2.11"); err != nil {
		t.Fatal(err)
	}
	if _, err = p.Activate(f.ctx, f.proof(t, "pending@example.test"), password); !errors.Is(err, postgres.ErrNotFound) {
		t.Fatal("verification proof activated panel", err)
	}
	if _, err = p.Login(f.ctx, "pending@example.test", password); !errors.Is(err, postgres.ErrNotFound) {
		t.Fatal("pending account signed in", err)
	}
}
