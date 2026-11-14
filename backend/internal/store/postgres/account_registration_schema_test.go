package postgres_test

import (
	"testing"
	"testing/fstest"

	"vault/internal/store/postgres"
	"vault/internal/testutil"
	"vault/migrations"
)

func TestAccountRegistrationMigrationIntegration(t *testing.T) {
	ctx, pool := testutil.Database(t)
	prefix := fstest.MapFS{}
	entries, err := migrations.FS.ReadDir(".")
	if err != nil {
		t.Fatal(err)
	}
	for _, entry := range entries {
		if entry.Name() < "0009" {
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
	if _, err := pool.Exec(ctx, `INSERT INTO users(email,panel_managed,panel_password) VALUES('legacy@example.test',true,$1)`, []byte("synthetic-existing-digest")); err != nil {
		t.Fatal(err)
	}
	var before, after string
	if err := pool.QueryRow(ctx, `SELECT to_jsonb(u)::text FROM users u`).Scan(&before); err != nil {
		t.Fatal(err)
	}
	for range 2 {
		if err := postgres.Apply(ctx, pool, migrations.FS); err != nil {
			t.Fatal(err)
		}
	}
	if err := pool.QueryRow(ctx, `SELECT (to_jsonb(u)-ARRAY['registration_started_at','email_verified_at'])::text FROM users u`).Scan(&after); err != nil || before != after {
		t.Fatal("migration rewrote existing identity")
	}
	var fabricated bool
	if err := pool.QueryRow(ctx, `SELECT registration_started_at IS NOT NULL OR email_verified_at IS NOT NULL FROM users`).Scan(&fabricated); err != nil || fabricated {
		t.Fatal("legacy verification fabricated")
	}
	var user string
	if err := pool.QueryRow(ctx, `INSERT INTO users(email,panel_managed,panel_active,registration_started_at) VALUES('pending@example.test',true,false,now()) RETURNING id::text`).Scan(&user); err != nil {
		t.Fatal(err)
	}
	for _, statement := range []string{`UPDATE users SET panel_active=true WHERE id=$1`, `UPDATE users SET panel_role='admin' WHERE id=$1`, `UPDATE users SET panel_password='\x01' WHERE id=$1`} {
		if _, err := pool.Exec(ctx, statement, user); err == nil {
			t.Fatal("pending signup acquired authority before proof")
		}
	}
	for _, statement := range []string{
		`INSERT INTO account_verifications(user_id,token_hash,purpose) VALUES($1,decode(repeat('01',32),'hex'),'password_reset')`,
		`INSERT INTO account_verifications(user_id,token_hash) VALUES($1,'\x01')`,
		`INSERT INTO account_verifications(user_id,token_hash,expires_at) VALUES($1,decode(repeat('01',32),'hex'),now()+interval '31 minutes')`,
		`INSERT INTO account_mail_outbox(id,user_id,token_hash,proof_ciphertext) VALUES(gen_random_uuid(),$1,decode(repeat('01',32),'hex'),'\x01')`,
	} {
		if _, err := pool.Exec(ctx, statement, user); err == nil {
			t.Fatal("invalid verification/queue constraint accepted")
		}
	}
}
