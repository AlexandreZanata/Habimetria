package privacy

import (
	"context"
	"os"
	"strings"
	"testing"
	"time"

	"vault/internal/store/postgres"
	"vault/migrations"
)

// personalPatterns matches column names that carry personal data or point
// at it. Any schema column matching one of these must be registered above.
var personalPatterns = []string{
	"email", "cpf", "cnpj", "phone", "contact", "address",
	"note", "title", "body", "label", "nickname", "complement",
	"street", "neighborhood", "ip_hash", "storage_key", "pdf_key",
	"owner_user_id", "latitude", "longitude", "captured_at_device", "token",
}

// TestInventoryMatchesSchema queries the live schema: every personal-data
// column must be registered, and every table needs a retention class.
// Skips unless TEST_DATABASE_URL/DATABASE_URL is set.
func TestInventoryMatchesSchema(t *testing.T) {
	dsn := os.Getenv("TEST_DATABASE_URL")
	if dsn == "" {
		dsn = os.Getenv("DATABASE_URL")
	}
	if dsn == "" {
		t.Skip("TEST_DATABASE_URL/DATABASE_URL unset: db integration unverified")
	}

	ctx, cancel := context.WithTimeout(context.Background(), 30*time.Second)
	defer cancel()

	pool, err := postgres.Open(ctx, dsn)
	if err != nil {
		t.Fatalf("open postgres: %v", err)
	}
	defer pool.Close()

	if err := postgres.Apply(ctx, pool, migrations.FS); err != nil {
		t.Fatalf("apply migrations: %v", err)
	}

	rows, err := pool.Query(ctx,
		`SELECT table_name, column_name FROM information_schema.columns
		 WHERE table_schema='public'`)
	if err != nil {
		t.Fatalf("inspect schema: %v", err)
	}
	defer rows.Close()

	registered := map[string]bool{}
	for _, f := range Registry() {
		registered[f.Table+"."+f.Column] = true
	}
	classes := RetentionClasses()
	tables := map[string]bool{}
	for rows.Next() {
		var table, column string
		if err := rows.Scan(&table, &column); err != nil {
			t.Fatalf("scan schema: %v", err)
		}
		tables[table] = true
		lower := strings.ToLower(column)
		personal := false
		for _, pattern := range personalPatterns {
			if strings.Contains(lower, pattern) {
				personal = true
				break
			}
		}
		if personal && !registered[table+"."+column] {
			t.Errorf("unregistered personal-data column %s.%s: register it in privacy.Registry with a purpose", table, column)
		}
	}
	if err := rows.Err(); err != nil {
		t.Fatalf("read schema: %v", err)
	}
	for table := range tables {
		if _, ok := classes[table]; !ok {
			t.Errorf("table %s lacks a retention class", table)
		}
	}
}
