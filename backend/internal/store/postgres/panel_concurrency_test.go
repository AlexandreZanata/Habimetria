package postgres_test

import (
	"errors"
	"testing"

	"vault/internal/store/postgres"
)

func TestPolicyAuthorityAfterQueuedRevocationIntegration(t *testing.T) {
	for _, change := range []string{"capability", "owner-role"} {
		t.Run(change, func(t *testing.T) {
			f := newPolicyFixture(t)
			block, err := f.pool.Begin(f.ctx)
			if err != nil {
				t.Fatal(err)
			}
			defer block.Rollback(f.ctx)
			if _, err := block.Exec(f.ctx, `LOCK TABLE users IN SHARE ROW EXCLUSIVE MODE`); err != nil {
				t.Fatal(err)
			}
			first, second := make(chan error, 1), make(chan error, 1)
			go func() {
				if change == "capability" {
					first <- f.store.SetRole(f.ctx, f.ids["primary"], "admin", []string{"accounts.read"}, "access_review")
				} else {
					first <- f.store.UpdateAccount(f.ctx, f.ids["primary"], f.ids["owner"], "support", true, "security_response")
				}
			}()
			f.waitWriters(t, 1)
			go func() {
				if change == "capability" {
					second <- f.store.UpdateAccount(f.ctx, f.ids["admin"], f.ids["user"], "user", false, "access_review")
				} else {
					second <- f.store.SetRole(f.ctx, f.ids["owner"], "support", []string{"commerce.read"}, "access_review")
				}
			}()
			f.waitWriters(t, 2)
			if err := block.Commit(f.ctx); err != nil {
				t.Fatal(err)
			}
			if err := <-first; err != nil {
				t.Fatalf("revocation failed: %v", err)
			}
			if err := <-second; !errors.Is(err, postgres.ErrPanelDenied) {
				t.Fatalf("stale administrative authority retained: %v", err)
			}
			var active, primary bool
			var changes int
			if err := f.pool.QueryRow(f.ctx, `SELECT
				(SELECT panel_active FROM users WHERE id=$1),
				(SELECT panel_active AND panel_primary AND panel_role='owner' FROM users WHERE id=$2),
				(SELECT count(*) FROM panel_audit WHERE operation_id IS NOT NULL)`, f.ids["user"], f.ids["primary"]).Scan(&active, &primary, &changes); err != nil {
				t.Fatal(err)
			}
			if !active || !primary || changes != 1 {
				t.Fatal("revoked write changed target, primary owner or success audit")
			}
		})
	}
}

func TestConcurrentPrimaryOwnerProtectionIntegration(t *testing.T) {
	f := newPolicyFixture(t)
	results := make(chan error, 2)
	for _, active := range []bool{true, false} {
		go func() {
			results <- f.store.UpdateAccount(f.ctx, f.ids["owner"], f.ids["primary"], "user", active, "access_review")
		}()
	}
	for range 2 {
		if err := <-results; !errors.Is(err, postgres.ErrPanelPrimary) {
			t.Fatalf("primary owner protection failed: %v", err)
		}
	}
	if _, err := f.pool.Exec(f.ctx, `UPDATE users SET panel_role='user',panel_active=false WHERE panel_primary`); err == nil {
		t.Fatal("direct SQL bypassed primary-owner constraint")
	}
}
