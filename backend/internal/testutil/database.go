package testutil

import (
	"context"
	"net/url"
	"os"
	"strings"
	"testing"
	"time"

	"github.com/google/uuid"
	"github.com/jackc/pgx/v5/pgxpool"
)

// Database creates an isolated database, never falling back to DATABASE_URL.
// Migrations are explicit at the call site so upgrade tests can choose a prefix.
func Database(t *testing.T) (context.Context, *pgxpool.Pool) {
	t.Helper()
	dsn := os.Getenv("TEST_DATABASE_URL")
	if dsn == "" {
		t.Skip("TEST_DATABASE_URL unset: database integration unverified")
	}
	ctx, cancel := context.WithTimeout(context.Background(), 90*time.Second)
	t.Cleanup(cancel)
	admin, err := pgxpool.New(ctx, dsn)
	if err != nil {
		t.Fatal(err)
	}
	t.Cleanup(admin.Close)
	name := "catalog_" + strings.ReplaceAll(uuid.NewString(), "-", "")
	if _, err := admin.Exec(ctx, "CREATE DATABASE "+name); err != nil {
		t.Fatal(err)
	}
	t.Cleanup(func() {
		cleanup, stop := context.WithTimeout(context.Background(), 10*time.Second)
		defer stop()
		if _, err := admin.Exec(cleanup, "DROP DATABASE "+name+" WITH (FORCE)"); err != nil {
			t.Errorf("drop fixture database: %v", err)
		}
	})
	cfg := admin.Config().Copy()
	cfg.ConnConfig.Database = name
	pool, err := pgxpool.NewWithConfig(ctx, cfg)
	if err != nil {
		t.Fatal(err)
	}
	t.Cleanup(pool.Close)
	return ctx, pool
}

// DatabaseDSN preserves the isolated database when handing a pool to a command.
// pgx Config.ConnString retains its original parsed string after Database edits.
func DatabaseDSN(pool *pgxpool.Pool) string {
	dsn := os.Getenv("TEST_DATABASE_URL")
	database := pool.Config().ConnConfig.Database
	if strings.HasPrefix(dsn, "postgres://") || strings.HasPrefix(dsn, "postgresql://") {
		parsed, err := url.Parse(dsn)
		if err == nil {
			parsed.Path = "/" + database
			parsed.RawPath = ""
			query := parsed.Query()
			query.Del("dbname")
			query.Del("database")
			parsed.RawQuery = query.Encode()
			return parsed.String()
		}
	}
	return dsn + " dbname=" + database
}
