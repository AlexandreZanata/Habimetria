package account_test

import (
	"errors"
	"fmt"
	"strings"
	"testing"

	"vault/internal/service/account"
	"vault/internal/store/postgres"
)

func TestRegistrationExistingIdentityIsolationIntegration(t *testing.T) {
	f := setup(t)
	for i, role := range []string{"owner", "admin", "support", "user"} {
		email := role + "@example.test"
		if _, err := f.pool.Exec(f.ctx, `INSERT INTO users(email,panel_role,panel_active) VALUES($1,$2,$3)`, email, role, i != 3); err != nil {
			t.Fatal(err)
		}
		var before, after string
		query := `SELECT row_to_json(u)::text FROM users u WHERE email=$1`
		if err := f.pool.QueryRow(f.ctx, query, email).Scan(&before); err != nil {
			t.Fatal(err)
		}
		if err := f.registration.Register(f.ctx, email, "192.0.2.3"); err != nil {
			t.Fatal(err)
		}
		if err := f.pool.QueryRow(f.ctx, query, email).Scan(&after); err != nil || before != after {
			t.Fatal("signup changed an existing identity")
		}
	}
	var queued int
	if err := f.pool.QueryRow(f.ctx, `SELECT count(*) FROM account_mail_outbox`).Scan(&queued); err != nil || queued != 0 {
		t.Fatal("existing identity received signup proof")
	}
	if err := f.registration.Register(f.ctx, "pending@example.test", "192.0.2.4"); err != nil {
		t.Fatal(err)
	}
	var pending, owner string
	if err := f.pool.QueryRow(f.ctx, `SELECT id::text FROM users WHERE email='pending@example.test'`).Scan(&pending); err != nil {
		t.Fatal(err)
	}
	if err := f.pool.QueryRow(f.ctx, `SELECT id::text FROM users WHERE panel_role='owner'`).Scan(&owner); err != nil {
		t.Fatal(err)
	}
	panel := postgres.NewPanelStore(f.pool)
	if err := panel.UpdateAccount(f.ctx, owner, pending, "admin", true, "access_review"); !errors.Is(err, postgres.ErrPanelDenied) {
		t.Fatalf("staff activated pending signup: %v", err)
	}
	if err := panel.Reissue(f.ctx, owner, pending, make([]byte, 32)); !errors.Is(err, postgres.ErrPanelDenied) {
		t.Fatalf("staff issued pending login link: %v", err)
	}
	if _, err := f.pool.Exec(f.ctx, `UPDATE users SET panel_active=true WHERE id=$1`, pending); err == nil {
		t.Fatal("SQL bypassed pending constraint")
	}
}

func TestRegistrationRollbackAndOutageIntegration(t *testing.T) {
	f := setup(t)
	if _, err := f.pool.Exec(f.ctx, `CREATE FUNCTION fixture_mail_failure() RETURNS TRIGGER LANGUAGE plpgsql AS $$
		BEGIN RAISE EXCEPTION 'synthetic enqueue outage'; END $$;
		CREATE TRIGGER fixture_mail_failure BEFORE INSERT ON account_mail_outbox FOR EACH ROW EXECUTE FUNCTION fixture_mail_failure()`); err != nil {
		t.Fatal(err)
	}
	err := f.registration.Register(f.ctx, "rollback@example.test", "192.0.2.5")
	if err == nil || strings.Contains(err.Error(), "rollback@example.test") {
		t.Fatal("queue failure was hidden or leaked input")
	}
	for _, format := range []string{"%v", "%+v", "%#v"} {
		if strings.Contains(fmt.Sprintf(format, err), "rollback@example.test") {
			t.Fatal("error formatting leaked input")
		}
	}
	var rows int
	if err := f.pool.QueryRow(f.ctx, `SELECT (SELECT count(*) FROM users)+(SELECT count(*) FROM account_verifications)+(SELECT count(*) FROM account_registration_limits)`).Scan(&rows); err != nil || rows != 0 {
		t.Fatal("failed enqueue left partial identity/proof/limit")
	}
	f.pool.Close()
	if err := f.registration.Register(f.ctx, "outage@example.test", "192.0.2.5"); err == nil {
		t.Fatal("database outage reported accepted")
	}
}

func TestRegistrationInputAndSourceLimitIntegration(t *testing.T) {
	f := setup(t)
	for _, email := range []string{"bad", "Name <person@example.test>", "a@example.test\r\nBCC:other@example.test", "a@é.test"} {
		if err := f.registration.Register(f.ctx, email, "192.0.2.6"); !errors.Is(err, account.ErrInput) {
			t.Fatal("invalid registration input accepted")
		}
	}
	if err := f.registration.Register(f.ctx, "valid@example.test", "claimed-source"); !errors.Is(err, account.ErrInput) {
		t.Fatal("unparsed source accepted")
	}
	for i := range 12 {
		if err := f.registration.Register(f.ctx, fmt.Sprintf("source%d@example.test", i), "192.0.2.7"); err != nil {
			t.Fatal(err)
		}
	}
	var jobs int
	if err := f.pool.QueryRow(f.ctx, `SELECT count(*) FROM account_mail_outbox`).Scan(&jobs); err != nil || jobs != 10 {
		t.Fatal("source limit bypassed")
	}
	if _, err := f.pool.Exec(f.ctx, `UPDATE account_registration_limits SET expires_at=now()-interval '1 second';
		INSERT INTO account_registration_limits(key_hash,attempts,expires_at)
		SELECT digest('expired-fixture-'||g,'sha256'),10,now()-interval '1 hour' FROM generate_series(1,110) g`); err != nil {
		t.Fatal(err)
	}
	if err := f.registration.Register(f.ctx, "source-reset@example.test", "192.0.2.7"); err != nil {
		t.Fatal(err)
	}
	if err := f.pool.QueryRow(f.ctx, `SELECT count(*) FROM account_mail_outbox`).Scan(&jobs); err != nil || jobs != 11 {
		t.Fatal("expired limit remained locked behind cleanup backlog")
	}
	if _, err := account.NewRegistration(nil, f.key); !errors.Is(err, account.ErrInput) {
		t.Fatal("missing persistence accepted")
	}
	if _, err := account.NewRegistration(postgres.NewAccountStore(f.pool), nil); !errors.Is(err, account.ErrInput) {
		t.Fatal("missing key accepted")
	}
}
