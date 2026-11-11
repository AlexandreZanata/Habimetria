package account_test

import (
	"errors"
	"testing"

	"vault/internal/service/account"
	"vault/internal/store/postgres"

	"github.com/google/uuid"
)

func TestDeviceControlsScopeAndLimitsIntegration(t *testing.T) {
	f := setup(t)
	user, encoded := f.identity(t, "user")
	auth, _ := f.authentication()
	d := device()
	first := loginDevice(t, f, auth, d)
	second := loginDevice(t, f, auth, d)
	if _, err := auth.Session(f.ctx, first.Access); !errors.Is(err, postgres.ErrNotFound) {
		t.Fatal("relogin left previous device live")
	}
	list, err := auth.Devices(f.ctx, user)
	if err != nil || len(list) != 1 || list[0].ID != second.ID {
		t.Fatal("bad own session list", err)
	}
	var other string
	if err := f.pool.QueryRow(f.ctx, `INSERT INTO users(email,panel_managed,panel_password) VALUES('other@example.test',true,$1) RETURNING id::text`, encoded).Scan(&other); err != nil {
		t.Fatal(err)
	}
	if err := auth.RevokeDevice(f.ctx, other, second.ID); !errors.Is(err, postgres.ErrNotFound) {
		t.Fatal("another User revoked session")
	}
	if err := auth.RevokeDevice(f.ctx, user, second.ID); err != nil {
		t.Fatal(err)
	}
	if err := auth.RevokeDevice(f.ctx, user, second.ID); err != nil {
		t.Fatal("revoke not idempotent", err)
	}
	f.exec(t, `INSERT INTO account_device_sessions(id,user_id,device_id,platform,label)
		SELECT gen_random_uuid(),id,gen_random_uuid(),'android','fixture' FROM users CROSS JOIN generate_series(1,20) WHERE email='user@example.test'`)
	if _, err := auth.LoginDevice(f.ctx, "user@example.test", password, device()); !errors.Is(err, postgres.ErrSessionLimit) {
		t.Fatal("technical device cap bypassed", err)
	}
	for _, bad := range []account.Device{{ID: "bad", Platform: "android", Label: "x"}, {ID: uuid.NewString(), Platform: "web", Label: "x"}, {ID: uuid.NewString(), Platform: "ios", Label: "\n"}} {
		if _, err := auth.LoginDevice(f.ctx, "user@example.test", password, bad); !errors.Is(err, account.ErrInput) {
			t.Fatal("invalid device metadata accepted")
		}
	}
}

func TestIdentityAndPasswordResetRevokeFamiliesIntegration(t *testing.T) {
	f := setup(t)
	user, encoded := f.identity(t, "user")
	auth, _ := f.authentication()
	for _, mutation := range []string{"panel_active=false", "panel_role='support'", "panel_password=NULL", "email='changed@example.test'"} {
		d := device()
		p := loginDevice(t, f, auth, d)
		f.exec(t, `UPDATE users SET `+mutation+` WHERE id='`+user+`'`)
		if _, err := f.pool.Exec(f.ctx, `UPDATE users SET panel_active=true,panel_role='user',panel_password=$2,email='user@example.test' WHERE id=$1`, user, encoded); err != nil {
			t.Fatal(err)
		}
		if _, err := auth.Refresh(f.ctx, p.Refresh, d.ID); !errors.Is(err, postgres.ErrNotFound) {
			t.Fatal("identity change resurrected refresh", err)
		}
	}
	one := loginDevice(t, f, auth, device())
	two := loginDevice(t, f, auth, device())
	request, reset := f.recovery(t)
	if err := request.Request(f.ctx, "user@example.test", "192.0.2.31"); err != nil {
		t.Fatal(err)
	}
	if err := reset.Reset(f.ctx, f.recoveryProof(t), replacement, replacement); err != nil {
		t.Fatal(err)
	}
	for _, p := range []account.DevicePair{one, two} {
		if _, err := auth.Session(f.ctx, p.Access); !errors.Is(err, postgres.ErrNotFound) {
			t.Fatal("reset retained device access")
		}
	}
}
