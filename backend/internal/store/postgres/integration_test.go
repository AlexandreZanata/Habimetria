package postgres

import (
	"context"
	"os"
	"testing"
	"time"

	"vault/migrations"
)

// TestPostgresIntegration runs migrations and a property roundtrip
// against a real PostgreSQL. It skips unless TEST_DATABASE_URL or
// DATABASE_URL is set, so an unconfigured suite never claims DB
// integration. See docs/06-roadmap/local_validation_acceptance.md.
func TestPostgresIntegration(t *testing.T) {
	dsn := os.Getenv("TEST_DATABASE_URL")
	if dsn == "" {
		dsn = os.Getenv("DATABASE_URL")
	}
	if dsn == "" {
		t.Skip("TEST_DATABASE_URL/DATABASE_URL unset: db integration unverified")
	}

	ctx, cancel := context.WithTimeout(context.Background(), 30*time.Second)
	defer cancel()

	pool, err := Open(ctx, dsn)
	if err != nil {
		t.Fatalf("open postgres: %v", err)
	}
	defer pool.Close()

	if err := Apply(ctx, pool, migrations.FS); err != nil {
		t.Fatalf("apply migrations: %v", err)
	}

	var applied int
	if err := pool.QueryRow(ctx, `SELECT count(*) FROM schema_migrations`).Scan(&applied); err != nil {
		t.Fatalf("count migrations: %v", err)
	}
	if applied < 4 {
		t.Fatalf("expected >=4 migrations, got %d", applied)
	}

	p, err := CreateProperty(ctx, pool, "integration-test address")
	if err != nil {
		t.Fatalf("create property: %v", err)
	}
	if p.ID == "" || p.Address != "integration-test address" {
		t.Fatalf("unexpected property: %+v", p)
	}
}
