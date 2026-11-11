package postgres_test

import (
	"context"
	"testing"
	"time"

	"vault/internal/store/postgres"
	"vault/internal/testutil"
	"vault/migrations"

	"github.com/jackc/pgx/v5/pgxpool"
)

type policyFixture struct {
	ctx   context.Context
	pool  *pgxpool.Pool
	store *postgres.PanelStore
	ids   map[string]string
}

func newPolicyFixture(t *testing.T) policyFixture {
	t.Helper()
	ctx, pool := testutil.Database(t)
	if err := postgres.Apply(ctx, pool, migrations.FS); err != nil {
		t.Fatal(err)
	}
	f := policyFixture{ctx, pool, postgres.NewPanelStore(pool), map[string]string{}}
	for _, key := range []string{"primary", "owner", "admin", "user"} {
		role := key
		if role == "primary" {
			role = "owner"
		}
		var id string
		if err := pool.QueryRow(ctx, `INSERT INTO users(email,panel_role,panel_primary) VALUES($1,$2,$3) RETURNING id::text`,
			key+"@example.test", role, key == "primary").Scan(&id); err != nil {
			t.Fatal(err)
		}
		f.ids[key] = id
	}
	return f
}

// Observe lock queues instead of relying on scheduler order or fixed delays.
func (f policyFixture) waitWriters(t *testing.T, want int) {
	t.Helper()
	deadline := time.Now().Add(5 * time.Second)
	for time.Now().Before(deadline) {
		var waiting int
		if err := f.pool.QueryRow(f.ctx, `SELECT count(*) FROM pg_locks WHERE relation='users'::regclass
			AND database=(SELECT oid FROM pg_database WHERE datname=current_database())
			AND mode='ShareRowExclusiveLock' AND NOT granted`).Scan(&waiting); err != nil {
			t.Fatal(err)
		}
		if waiting == want {
			return
		}
		time.Sleep(10 * time.Millisecond)
	}
	t.Fatal("administrative writes did not enter expected database lock queue")
}
