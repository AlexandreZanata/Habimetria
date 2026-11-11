package account_test

import (
	"errors"
	"fmt"
	"sync"
	"testing"

	"vault/internal/service/account"
	"vault/internal/store/postgres"

	"github.com/google/uuid"
)

func device() account.Device {
	return account.Device{ID: uuid.NewString(), Platform: "android", Label: "Fixture phone"}
}
func loginDevice(t *testing.T, f fixture, auth *account.Authentication, d account.Device) account.DevicePair {
	t.Helper()
	p, err := auth.LoginDevice(f.ctx, "user@example.test", password, d)
	if err != nil {
		t.Fatal(err)
	}
	if len(p.Access) != 43 || len(p.Refresh) != 47 || p.Kind != "Bearer" || p.Seconds != 900 {
		t.Fatal("unsafe native credential response")
	}
	return p
}

func TestDeviceRotationReuseAndLogoutIntegration(t *testing.T) {
	f := setup(t)
	f.identity(t, "user")
	auth, panel := f.authentication()
	d1, d2 := device(), device()
	one := loginDevice(t, f, auth, d1)
	two := loginDevice(t, f, auth, d2)
	if _, err := panel.Session(f.ctx, one.Access); !errors.Is(err, postgres.ErrNotFound) {
		t.Fatal("device access authorized browser")
	}
	if _, err := auth.Session(f.ctx, one.Refresh); !errors.Is(err, postgres.ErrNotFound) {
		t.Fatal("refresh authorized access")
	}
	next, err := auth.Refresh(f.ctx, one.Refresh, d1.ID)
	if err != nil {
		t.Fatal(err)
	}
	if next.ID != one.ID || next.User != one.User || !next.Expires.Equal(one.Expires) || next.Refresh == one.Refresh {
		t.Fatal("rotation changed scope/deadline")
	}
	if _, err := auth.Session(f.ctx, one.Access); !errors.Is(err, postgres.ErrNotFound) {
		t.Fatal("previous generation access survived")
	}
	if _, err := auth.Refresh(f.ctx, next.Refresh, d2.ID); !errors.Is(err, postgres.ErrNotFound) {
		t.Fatal("wrong device accepted")
	}
	if _, err := auth.Session(f.ctx, next.Access); err != nil {
		t.Fatal("wrong device revoked legitimate session")
	}
	if _, err := auth.Refresh(f.ctx, one.Refresh, d1.ID); !errors.Is(err, postgres.ErrNotFound) {
		t.Fatal("spent refresh accepted")
	}
	if _, err := auth.Session(f.ctx, next.Access); !errors.Is(err, postgres.ErrNotFound) {
		t.Fatal("reuse revocation rolled back")
	}
	if _, err := auth.Refresh(f.ctx, next.Refresh, d1.ID); !errors.Is(err, postgres.ErrNotFound) {
		t.Fatal("reuse descendant survived")
	}
	if _, err := auth.Session(f.ctx, two.Access); err != nil {
		t.Fatal("other device lost authority", err)
	}
	if err := auth.Logout(f.ctx, two.Access); err != nil {
		t.Fatal(err)
	}
	if _, err := auth.Refresh(f.ctx, two.Refresh, d2.ID); !errors.Is(err, postgres.ErrNotFound) {
		t.Fatal("logout left refresh authority")
	}
	var leaked bool
	if err := f.pool.QueryRow(f.ctx, `SELECT EXISTS(SELECT 1 FROM account_refresh_tokens r WHERE position($1 in row_to_json(r)::text)>0)`, one.Refresh).Scan(&leaked); err != nil || leaked {
		t.Fatal("raw refresh persisted", err)
	}
	if text := fmt.Sprintf("%+v", next); text != "[device credentials redacted]" {
		t.Fatal("pair formatting leaked")
	}
}

func TestConcurrentRefreshCommitsReuseDenialIntegration(t *testing.T) {
	f := setup(t)
	f.identity(t, "user")
	auth, _ := f.authentication()
	d := device()
	first := loginDevice(t, f, auth, d)
	var wg sync.WaitGroup
	results := make(chan struct {
		pair account.DevicePair
		err  error
	}, 2)
	for range 2 {
		wg.Add(1)
		go func() {
			defer wg.Done()
			p, err := auth.Refresh(f.ctx, first.Refresh, d.ID)
			results <- struct {
				pair account.DevicePair
				err  error
			}{p, err}
		}()
	}
	wg.Wait()
	close(results)
	success := 0
	var winner account.DevicePair
	for result := range results {
		if result.err == nil {
			success++
			winner = result.pair
		} else if !errors.Is(result.err, postgres.ErrNotFound) {
			t.Fatal(result.err)
		}
	}
	if success != 1 {
		t.Fatal("refresh consumed more than once")
	}
	if _, err := auth.Session(f.ctx, winner.Access); !errors.Is(err, postgres.ErrNotFound) {
		t.Fatal("concurrent replay did not revoke winner")
	}
}

func TestDeviceRotationRollbackAndExpiryIntegration(t *testing.T) {
	f := setup(t)
	f.identity(t, "user")
	auth, _ := f.authentication()
	d := device()
	p := loginDevice(t, f, auth, d)
	f.exec(t, `CREATE FUNCTION reject_device_access() RETURNS trigger LANGUAGE plpgsql AS $$ BEGIN RAISE EXCEPTION 'private fixture'; END $$;
		CREATE TRIGGER reject_device_access BEFORE INSERT ON account_access_sessions FOR EACH ROW EXECUTE FUNCTION reject_device_access()`)
	if _, err := auth.Refresh(f.ctx, p.Refresh, d.ID); err == nil || errors.Is(err, postgres.ErrNotFound) {
		t.Fatal("failed mint looked consumed")
	}
	if _, err := auth.Session(f.ctx, p.Access); err != nil {
		t.Fatal("rotation failure lost old authority", err)
	}
	f.exec(t, `DROP TRIGGER reject_device_access ON account_access_sessions`)
	current, err := auth.Refresh(f.ctx, p.Refresh, d.ID)
	if err != nil {
		t.Fatal(err)
	}
	f.exec(t, `UPDATE account_device_sessions SET created_at=created_at-interval '31 days',expires_at=expires_at-interval '31 days'`)
	if _, err := auth.Session(f.ctx, current.Access); !errors.Is(err, postgres.ErrNotFound) {
		t.Fatal("expired family authorized")
	}
	if _, err := auth.Refresh(f.ctx, current.Refresh, d.ID); !errors.Is(err, postgres.ErrNotFound) {
		t.Fatal("expired family refreshed")
	}
	loginDevice(t, f, auth, device())
	var count int
	if err := f.pool.QueryRow(f.ctx, `SELECT count(*) FROM account_refresh_tokens WHERE session_id=$1`, p.ID).Scan(&count); err != nil || count != 0 {
		t.Fatal("expired history not pruned", err)
	}
}
