package account_test

import (
	"errors"
	"fmt"
	"testing"

	"vault/internal/service/account"
	"vault/internal/service/panel"
	"vault/internal/store/postgres"
)

func TestRecoveryEligibilityPurposeIntegration(t *testing.T) {
	f := setup(t)
	request, reset := f.recovery(t)
	if _, err := account.NewRecoveryRequest(nil, f.key); !errors.Is(err, account.ErrInput) {
		t.Fatal(err)
	}
	if _, err := account.NewRecoveryRequest(postgres.NewAccountStore(f.pool), nil); !errors.Is(err, account.ErrInput) {
		t.Fatal(err)
	}
	for _, role := range []string{"owner", "admin", "support", "user"} {
		f.identity(t, role)
	}
	f.exec(t, `UPDATE users SET panel_active=false WHERE panel_role='user'`)
	for _, email := range []string{"owner@example.test", "admin@example.test", "support@example.test", "user@example.test", "absent@example.test"} {
		if err := request.Request(f.ctx, email, "192.0.2.15"); err != nil {
			t.Fatal(err)
		}
	}
	if err := request.Request(f.ctx, "invalid", "192.0.2.15"); !errors.Is(err, account.ErrInput) {
		t.Fatal(err)
	}
	var count int
	if err := f.pool.QueryRow(f.ctx, `SELECT count(*) FROM account_mail_outbox`).Scan(&count); err != nil || count != 0 {
		t.Fatal("ineligible recovery queued", err)
	}
	if err := f.registration.Register(f.ctx, "pending@example.test", "192.0.2.16"); err != nil {
		t.Fatal(err)
	}
	if err := request.Request(f.ctx, "pending@example.test", "192.0.2.15"); err != nil {
		t.Fatal(err)
	}
	if err := reset.Reset(f.ctx, f.proof(t, "pending@example.test"), replacement, replacement); !errors.Is(err, postgres.ErrNotFound) {
		t.Fatal("verification reset credential", err)
	}
	f.exec(t, `UPDATE users SET panel_active=true WHERE panel_role='user' AND email='user@example.test'`)
	if err := request.Request(f.ctx, "user@example.test", "192.0.2.16"); err != nil {
		t.Fatal(err)
	}
	proof := f.recoveryProof(t)
	if err := account.NewVerification(postgres.NewAccountStore(f.pool)).Verify(f.ctx, proof, replacement); !errors.Is(err, postgres.ErrNotFound) {
		t.Fatal("recovery verified email", err)
	}
	p := panel.New(postgres.NewPanelStore(f.pool))
	if _, err := p.Activate(f.ctx, proof, replacement); !errors.Is(err, postgres.ErrNotFound) {
		t.Fatal("recovery became invitation", err)
	}
}

func TestRecoveryIdentityChangeIntegration(t *testing.T) {
	for _, mutation := range []string{"panel_active=false", "panel_role='support'", "panel_password=NULL", "email='changed@example.test'"} {
		t.Run(mutation, func(t *testing.T) {
			f := setup(t)
			user, encoded := f.identity(t, "user")
			request, reset := f.recovery(t)
			if err := request.Request(f.ctx, "user@example.test", "192.0.2.17"); err != nil {
				t.Fatal(err)
			}
			proof := f.recoveryProof(t)
			f.exec(t, `UPDATE users SET `+mutation+` WHERE panel_role='user'`)
			if _, err := f.pool.Exec(f.ctx, `UPDATE users SET panel_active=true,panel_role='user',panel_password=$2,email='user@example.test' WHERE id=$1`, user, encoded); err != nil {
				t.Fatal(err)
			}
			if err := reset.Reset(f.ctx, proof, replacement, replacement); !errors.Is(err, postgres.ErrNotFound) {
				t.Fatal("changed identity proof resurrected", err)
			}
		})
	}
}

func TestRecoveryThrottlingIntegration(t *testing.T) {
	f := setup(t)
	_, encoded := f.identity(t, "user")
	request, _ := f.recovery(t)
	for range 2 {
		if err := request.Request(f.ctx, "user@example.test", "192.0.2.18"); err != nil {
			t.Fatal(err)
		}
	}
	f.exec(t, `UPDATE account_password_recoveries SET created_at=created_at-interval '61 seconds',expires_at=expires_at-interval '61 seconds'`)
	if err := request.Request(f.ctx, "user@example.test", "192.0.2.18"); err != nil {
		t.Fatal(err)
	}
	current := f.recoveryProof(t)
	f.exec(t, `UPDATE account_password_recoveries SET created_at=created_at-interval '61 seconds',expires_at=expires_at-interval '61 seconds'`)
	if err := request.Request(f.ctx, "user@example.test", "192.0.2.18"); err != nil {
		t.Fatal(err)
	}
	if f.recoveryProof(t) != current {
		t.Fatal("address limit bypassed")
	}
	if _, err := f.pool.Exec(f.ctx, `INSERT INTO users(email,panel_managed,panel_password) VALUES('other@example.test',true,$1)`, encoded); err != nil {
		t.Fatal(err)
	}
	for i := range 10 {
		if err := request.Request(f.ctx, fmt.Sprintf("unknown%d@example.test", i), "192.0.2.19"); err != nil {
			t.Fatal(err)
		}
	}
	if err := request.Request(f.ctx, "other@example.test", "192.0.2.19"); err != nil {
		t.Fatal(err)
	}
	var jobs int
	if err := f.pool.QueryRow(f.ctx, `SELECT count(*) FROM account_mail_outbox`).Scan(&jobs); err != nil || jobs != 2 {
		t.Fatal("cooldown/source budget bypassed", err)
	}
	if err := request.Request(f.ctx, "other@example.test", "192.0.2.20"); err != nil {
		t.Fatal(err)
	}
	if err := f.pool.QueryRow(f.ctx, `SELECT count(*) FROM account_mail_outbox`).Scan(&jobs); err != nil || jobs != 3 {
		t.Fatal("source limit leaked across peers", err)
	}
}
