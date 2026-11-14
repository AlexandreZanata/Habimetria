package postgres_test

import (
	"testing"
	"testing/fstest"

	"vault/internal/authz"
	"vault/internal/store/postgres"
	"vault/internal/testutil"
	"vault/migrations"
)

func TestPanelAuditSchemaIntegration(t *testing.T) {
	ctx, pool := testutil.Database(t)
	prefix := fstest.MapFS{}
	entries, err := migrations.FS.ReadDir(".")
	if err != nil {
		t.Fatal(err)
	}
	for _, entry := range entries {
		if entry.Name() < "0008" {
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
	var actor string
	if err := pool.QueryRow(ctx, `INSERT INTO users(email,panel_role,panel_primary) VALUES('audit@example.test','owner',true) RETURNING id::text`).Scan(&actor); err != nil {
		t.Fatal(err)
	}
	if _, err := pool.Exec(ctx, `INSERT INTO panel_audit(actor_id,event,subject_id) VALUES($1,'account_created',$1)`, actor); err != nil {
		t.Fatal(err)
	}
	var before, after string
	read := `SELECT jsonb_build_array(actor_id,event,subject_id,occurred_at)::text FROM panel_audit WHERE id=1`
	if err := pool.QueryRow(ctx, read).Scan(&before); err != nil {
		t.Fatal(err)
	}
	for range 2 {
		if err := postgres.Apply(ctx, pool, migrations.FS); err != nil {
			t.Fatal(err)
		}
	}
	if err := pool.QueryRow(ctx, read).Scan(&after); err != nil || before != after {
		t.Fatalf("historical event rewritten: %v", err)
	}
	var honest bool
	if err := pool.QueryRow(ctx, `SELECT operation_id IS NULL AND reason_code IS NULL AND before_role IS NULL AND after_role IS NULL FROM panel_audit WHERE id=1`).Scan(&honest); err != nil || !honest {
		t.Fatalf("historical audit metadata fabricated: %v", err)
	}
	insert := `INSERT INTO panel_audit(actor_id,event,operation_id,reason_code,before_role,after_role,before_permissions,after_permissions)
		VALUES($1,'permissions_updated',gen_random_uuid(),$2,$3,$3,'{}',$4)`
	for _, role := range []string{"admin", "support"} {
		for _, capability := range authz.Registry() {
			permissions := []string{capability.ID}
			if capability.ID == "accounts.write" {
				permissions = append(permissions, "accounts.read")
			}
			_, err := pool.Exec(ctx, insert, actor, "access_review", role, permissions)
			if (err == nil) != authz.Assignable(role, capability.ID) {
				t.Fatalf("audit registry drift for %s/%s: %v", role, capability.ID, err)
			}
		}
	}
	for _, invalid := range []struct {
		reason, role string
		permissions  []string
	}{
		{"audit@example.test", "admin", []string{"finance.read"}},
		{"access_review", "person@example.test", []string{}},
		{"access_review", "admin", []string{"secret value"}},
		{"access_review", "admin", []string{"accounts.write"}},
		{"access_review", "support", []string{"commerce.read", "commerce.read"}},
	} {
		if _, err := pool.Exec(ctx, insert, actor, invalid.reason, invalid.role, invalid.permissions); err == nil {
			t.Fatal("audit accepted unbounded or invalid policy data")
		}
	}
	for _, statement := range []string{
		`UPDATE panel_audit SET event='profile_updated'`, `DELETE FROM panel_audit`, `TRUNCATE panel_audit`,
		`INSERT INTO panel_audit(actor_id,event) SELECT actor_id,'account_updated' FROM panel_audit LIMIT 1`,
		`INSERT INTO panel_audit(actor_id,event) SELECT actor_id,'secret value' FROM panel_audit LIMIT 1`,
		`INSERT INTO panel_audit(actor_id,event,operation_id,reason_code,before_role,after_role,before_permissions,after_permissions)
		 SELECT actor_id,event,operation_id,reason_code,before_role,after_role,before_permissions,after_permissions FROM panel_audit WHERE operation_id IS NOT NULL LIMIT 1`,
	} {
		if _, err := pool.Exec(ctx, statement); err == nil {
			t.Fatalf("audit integrity bypass: %s", statement)
		}
	}
}
