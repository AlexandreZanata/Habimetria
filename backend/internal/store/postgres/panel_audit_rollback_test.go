package postgres_test

import "testing"

func TestPolicyAuditFailureRollbackIntegration(t *testing.T) {
	f := newPolicyFixture(t)
	if _, err := f.pool.Exec(f.ctx, `INSERT INTO panel_sessions(token_hash,user_id,expires_at) VALUES($1,$2,now()+interval '1 hour')`, []byte("synthetic-session-digest"), f.ids["user"]); err != nil {
		t.Fatal(err)
	}
	if _, err := f.pool.Exec(f.ctx, `CREATE FUNCTION fixture_audit_failure() RETURNS TRIGGER LANGUAGE plpgsql AS $$
		BEGIN RAISE EXCEPTION 'synthetic audit outage'; END $$;
		CREATE TRIGGER fixture_audit_failure BEFORE INSERT ON panel_audit
		FOR EACH ROW EXECUTE FUNCTION fixture_audit_failure()`); err != nil {
		t.Fatal(err)
	}
	if err := f.store.UpdateAccount(f.ctx, f.ids["primary"], f.ids["user"], "user", false, "security_response"); err == nil {
		t.Fatal("audit outage swallowed")
	}
	if err := f.store.SetRole(f.ctx, f.ids["primary"], "support", []string{"commerce.read"}, "access_review"); err == nil {
		t.Fatal("policy audit outage swallowed")
	}
	var active bool
	var sessions, audits int
	var permissions []string
	if err := f.pool.QueryRow(f.ctx, `SELECT panel_active,
		(SELECT count(*) FROM panel_sessions WHERE user_id=$1), (SELECT count(*) FROM panel_audit),
		(SELECT permissions FROM panel_role_permissions WHERE role='support') FROM users WHERE id=$1`, f.ids["user"]).Scan(&active, &sessions, &audits, &permissions); err != nil {
		t.Fatal(err)
	}
	if !active || sessions != 1 || audits != 0 || len(permissions) != 2 || permissions[0] != "accounts.read" {
		t.Fatal("failed audit left a partial mutation or session revocation")
	}
}
