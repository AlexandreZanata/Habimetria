package account_test

import (
	"bytes"
	"context"
	"sync"
	"testing"

	"vault/internal/accountproof"
	"vault/internal/service/account"
	"vault/internal/store/postgres"
	"vault/internal/testutil"
	"vault/migrations"

	"github.com/jackc/pgx/v5/pgxpool"
)

type fixture struct {
	ctx          context.Context
	pool         *pgxpool.Pool
	key          *accountproof.Key
	registration *account.Registration
}

func setup(t *testing.T) fixture {
	t.Helper()
	ctx, pool := testutil.Database(t)
	if err := postgres.Apply(ctx, pool, migrations.FS); err != nil {
		t.Fatal(err)
	}
	key, err := accountproof.New(bytes.Repeat([]byte{77}, 32))
	if err != nil {
		t.Fatal(err)
	}
	registration, err := account.NewRegistration(postgres.NewAccountStore(pool), key)
	if err != nil {
		t.Fatal(err)
	}
	return fixture{ctx, pool, key, registration}
}

func TestPendingRegistrationIntegration(t *testing.T) {
	f := setup(t)
	var wg sync.WaitGroup
	results := make(chan error, 8)
	for range 8 {
		wg.Add(1)
		go func() {
			defer wg.Done()
			results <- f.registration.Register(f.ctx, " Pending@EXAMPLE.test ", "192.0.2.1")
		}()
	}
	wg.Wait()
	close(results)
	for err := range results {
		if err != nil {
			t.Fatal(err)
		}
	}
	var user, operation string
	var digest, ciphertext []byte
	var safe bool
	if err := f.pool.QueryRow(f.ctx, `SELECT u.id::text,o.id::text,v.token_hash,o.proof_ciphertext,
		NOT u.panel_active AND u.panel_managed AND u.panel_password IS NULL AND u.panel_role='user'
		AND u.registration_started_at IS NOT NULL AND u.email_verified_at IS NULL
		AND v.purpose='email_verification' AND v.expires_at-v.created_at=interval '30 minutes'
		FROM users u JOIN account_verifications v ON v.user_id=u.id JOIN account_mail_outbox o ON o.user_id=u.id
		WHERE u.email='pending@example.test'`).Scan(&user, &operation, &digest, &ciphertext, &safe); err != nil {
		t.Fatal(err)
	}
	if !safe {
		t.Fatal("registration acquired authority or wrong proof scope")
	}
	token, err := f.key.Open(user, operation, ciphertext)
	if err != nil {
		t.Fatal(err)
	}
	hash, err := accountproof.Digest(token)
	if err != nil || !bytes.Equal(hash, digest) {
		t.Fatal("queue proof does not match verification digest")
	}
	var users, jobs, sessions int
	if err := f.pool.QueryRow(f.ctx, `SELECT (SELECT count(*) FROM users),(SELECT count(*) FROM account_mail_outbox),(SELECT count(*) FROM panel_sessions)`).Scan(&users, &jobs, &sessions); err != nil {
		t.Fatal(err)
	}
	if users != 1 || jobs != 1 || sessions != 0 {
		t.Fatal("duplicate registration or premature session")
	}
	var raw bool
	if err := f.pool.QueryRow(f.ctx, `SELECT position($1 in row_to_json(o)::text)>0 FROM account_mail_outbox o`, token).Scan(&raw); err != nil || raw {
		t.Fatal("raw proof persisted")
	}
}

func TestRegistrationResendIntegration(t *testing.T) {
	f := setup(t)
	register := func() {
		t.Helper()
		if err := f.registration.Register(f.ctx, "resend@example.test", "192.0.2.2"); err != nil {
			t.Fatal(err)
		}
	}
	register()
	register()
	var first []byte
	if err := f.pool.QueryRow(f.ctx, `SELECT token_hash FROM account_verifications`).Scan(&first); err != nil {
		t.Fatal(err)
	}
	if _, err := f.pool.Exec(f.ctx, `UPDATE account_verifications SET created_at=created_at-interval '61 seconds',expires_at=expires_at-interval '61 seconds'`); err != nil {
		t.Fatal(err)
	}
	register()
	var second []byte
	var cancelled, pending int
	if err := f.pool.QueryRow(f.ctx, `SELECT token_hash FROM account_verifications`).Scan(&second); err != nil {
		t.Fatal(err)
	}
	if err := f.pool.QueryRow(f.ctx, `SELECT count(*) FILTER(WHERE state='cancelled' AND proof_ciphertext IS NULL),count(*) FILTER(WHERE state='pending') FROM account_mail_outbox`).Scan(&cancelled, &pending); err != nil {
		t.Fatal(err)
	}
	if bytes.Equal(first, second) || cancelled != 1 || pending != 1 {
		t.Fatal("resend did not invalidate prior proof/queue")
	}
	if _, err := f.pool.Exec(f.ctx, `UPDATE account_verifications SET created_at=created_at-interval '61 seconds',expires_at=expires_at-interval '61 seconds'`); err != nil {
		t.Fatal(err)
	}
	register() // Fourth address request is suppressed even after cooldown.
	var jobs int
	if err := f.pool.QueryRow(f.ctx, `SELECT count(*) FROM account_mail_outbox`).Scan(&jobs); err != nil || jobs != 2 {
		t.Fatal("address limit bypassed")
	}
}
