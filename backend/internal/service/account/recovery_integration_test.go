package account_test

import (
	"errors"
	"sync"
	"testing"

	"vault/internal/accountproof"
	"vault/internal/credential"
	"vault/internal/service/account"
	"vault/internal/store/postgres"
)

func TestPasswordRecoveryAtomicIntegration(t *testing.T) {
	f := setup(t)
	user, _ := f.identity(t, "user")
	auth, p := f.authentication()
	browser, err := p.Login(f.ctx, "user@example.test", password)
	if err != nil {
		t.Fatal(err)
	}
	api, err := auth.Login(f.ctx, "user@example.test", password)
	if err != nil {
		t.Fatal(err)
	}
	request, reset := f.recovery(t)
	if err = request.Request(f.ctx, " USER@example.test ", "192.0.2.13"); err != nil {
		t.Fatal(err)
	}
	proof := f.recoveryProof(t)
	if _, err = p.Session(f.ctx, browser); err != nil {
		t.Fatal("initiation revoked session", err)
	}
	lease, err := postgres.NewAccountStore(f.pool).ClaimMail(f.ctx)
	if err != nil || lease == nil {
		t.Fatal("fixture lease unavailable", err)
	}
	var wg sync.WaitGroup
	results := make(chan error, 4)
	for range 4 {
		wg.Add(1)
		go func() { defer wg.Done(); results <- reset.Reset(f.ctx, proof, replacement, replacement) }()
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
		t.Fatalf("reset consumed %d times", success)
	}
	var safe bool
	err = f.pool.QueryRow(f.ctx, `SELECT panel_active AND panel_role='user' AND email_verified_at IS NULL AND registration_started_at IS NULL
		AND NOT EXISTS(SELECT 1 FROM panel_sessions WHERE user_id=u.id)
		AND NOT EXISTS(SELECT 1 FROM account_access_sessions WHERE user_id=u.id)
		AND NOT EXISTS(SELECT 1 FROM account_password_recoveries WHERE user_id=u.id)
		AND (SELECT count(*) FROM panel_audit WHERE actor_id=u.id AND subject_id=u.id AND event='password_reset')=1
		AND (SELECT count(*) FROM account_mail_outbox WHERE user_id=u.id AND template='password_changed'
		AND token_hash IS NULL AND proof_ciphertext IS NULL AND octet_length(recipient_digest)=32)=1
		AND (SELECT count(*) FROM account_mail_outbox WHERE user_id=u.id AND template='password_recovery'
		AND state='cancelled' AND proof_ciphertext IS NULL AND lease_id IS NULL)=1 FROM users u WHERE id=$1`, user).Scan(&safe)
	if err != nil || !safe {
		t.Fatal("reset was not atomic/minimal", err)
	}
	if _, err = p.Session(f.ctx, browser); !errors.Is(err, postgres.ErrNotFound) {
		t.Fatal("old browser survived", err)
	}
	if _, err = auth.Session(f.ctx, api); !errors.Is(err, postgres.ErrNotFound) {
		t.Fatal("old API access survived", err)
	}
	if _, err = p.Login(f.ctx, "user@example.test", password); !errors.Is(err, postgres.ErrNotFound) {
		t.Fatal("old password survived", err)
	}
	if _, err = p.Login(f.ctx, "user@example.test", replacement); err != nil {
		t.Fatal(err)
	}
	if _, err = auth.Login(f.ctx, "user@example.test", replacement); err != nil {
		t.Fatal(err)
	}
}

func TestResetAlwaysRevokesAPIIntegration(t *testing.T) {
	f := setup(t)
	_, encoded := f.identity(t, "user")
	auth, _ := f.authentication()
	token, err := auth.Login(f.ctx, "user@example.test", password)
	if err != nil {
		t.Fatal(err)
	}
	request, _ := f.recovery(t)
	if err := request.Request(f.ctx, "user@example.test", "192.0.2.24"); err != nil {
		t.Fatal(err)
	}
	digest, err := accountproof.Digest(f.recoveryProof(t))
	if err != nil {
		t.Fatal(err)
	}
	// Revocation is a reset invariant, independent of a changed encoding/trigger.
	if err := postgres.NewAccountStore(f.pool).ResetPassword(f.ctx, digest, encoded); err != nil {
		t.Fatal(err)
	}
	if _, err := auth.Session(f.ctx, token); !errors.Is(err, postgres.ErrNotFound) {
		t.Fatal("unchanged credential retained access", err)
	}
}

func TestRecoveryExpiryRollbackIntegration(t *testing.T) {
	f := setup(t)
	f.identity(t, "user")
	request, reset := f.recovery(t)
	if err := request.Request(f.ctx, "user@example.test", "192.0.2.14"); err != nil {
		t.Fatal(err)
	}
	proof := f.recoveryProof(t)
	if err := reset.Reset(f.ctx, proof, "short", "short"); !errors.Is(err, credential.ErrPolicy) {
		t.Fatal(err)
	}
	if err := reset.Reset(f.ctx, proof, replacement, password); !errors.Is(err, account.ErrConfirmation) {
		t.Fatal(err)
	}
	f.exec(t, `UPDATE account_password_recoveries SET created_at=created_at-interval '31 minutes',expires_at=expires_at-interval '31 minutes'`)
	if err := reset.Reset(f.ctx, proof, replacement, replacement); !errors.Is(err, postgres.ErrNotFound) {
		t.Fatal("expired proof accepted", err)
	}
	if err := request.Request(f.ctx, "user@example.test", "192.0.2.14"); err != nil {
		t.Fatal(err)
	}
	if err := reset.Reset(f.ctx, proof, replacement, replacement); !errors.Is(err, postgres.ErrNotFound) {
		t.Fatal("replaced proof accepted", err)
	}
	current := f.recoveryProof(t)
	f.exec(t, `CREATE FUNCTION reject_reset() RETURNS trigger LANGUAGE plpgsql AS $$ BEGIN RAISE EXCEPTION 'fixture failure'; END $$`)
	for _, target := range []string{"panel_audit", "account_mail_outbox"} {
		condition := ""
		if target == "account_mail_outbox" {
			condition = " WHEN (NEW.template='password_changed')"
		}
		f.exec(t, `CREATE TRIGGER reject_reset BEFORE INSERT ON `+target+` FOR EACH ROW`+condition+` EXECUTE FUNCTION reject_reset()`)
		if err := reset.Reset(f.ctx, current, replacement, replacement); err == nil || errors.Is(err, postgres.ErrNotFound) {
			t.Fatal("failed persistence did not abort")
		}
		if f.recoveryProof(t) != current {
			t.Fatal("rollback lost proof")
		}
		f.exec(t, `DROP TRIGGER reject_reset ON `+target)
	}
	if err := reset.Reset(f.ctx, current, replacement, replacement); err != nil {
		t.Fatal(err)
	}
}
