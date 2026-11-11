package postgres_test

import (
	"slices"
	"testing"
	"testing/fstest"

	"vault/internal/authz"
	"vault/internal/store/postgres"
	"vault/internal/testutil"
	"vault/migrations"
)

func TestPanelCapabilitiesMigrationIntegration(t *testing.T) {
	ctx, pool := testutil.Database(t)
	entries, err := migrations.FS.ReadDir(".")
	if err != nil {
		t.Fatal(err)
	}
	prefix := fstest.MapFS{}
	for _, entry := range entries {
		if entry.Name() < "0007" {
			data, err := migrations.FS.ReadFile(entry.Name())
			if err != nil {
				t.Fatal(err)
			}
			prefix[entry.Name()] = &fstest.MapFile{Data: data}
		}
	}
	if err := postgres.Apply(ctx, pool, prefix); err != nil {
		t.Fatal(err)
	}
	var before, after string
	read := `SELECT jsonb_agg(to_jsonb(p) ORDER BY role)::text FROM panel_role_permissions p`
	if err := pool.QueryRow(ctx, read).Scan(&before); err != nil {
		t.Fatal(err)
	}
	for range 2 {
		if err := postgres.Apply(ctx, pool, migrations.FS); err != nil {
			t.Fatal(err)
		}
	}
	if err := pool.QueryRow(ctx, read).Scan(&after); err != nil || after != before {
		t.Fatalf("migration changed grants: %v", err)
	}
	for _, role := range []string{"admin", "support"} {
		for _, capability := range authz.Registry() {
			grant := []string{capability.ID}
			if capability.ID == "accounts.write" {
				grant = append(grant, "accounts.read")
			}
			_, err := pool.Exec(ctx, `UPDATE panel_role_permissions SET permissions=$2 WHERE role=$1`, role, grant)
			if (err == nil) != authz.Assignable(role, capability.ID) {
				t.Fatalf("schema/registry drift for %s/%s: %v", role, capability.ID, err)
			}
		}
	}
	for _, grant := range [][]string{{"invented.write"}, {"accounts.write"}, {"commerce.read", "commerce.read"}, {"commerce.read", ""}, {""}} {
		if _, err := pool.Exec(ctx, `UPDATE panel_role_permissions SET permissions=$1 WHERE role='admin'`, grant); err == nil {
			t.Fatalf("database accepted invalid grant: %v", grant)
		}
	}
	var granted []string
	if err := pool.QueryRow(ctx, `SELECT permissions FROM panel_role_permissions WHERE role='support'`).Scan(&granted); err != nil {
		t.Fatal(err)
	}
	if slices.Contains(granted, "operations.write") {
		t.Fatal("support escalated")
	}
}
