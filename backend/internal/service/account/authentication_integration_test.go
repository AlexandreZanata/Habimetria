package account_test

import (
	"bytes"
	"errors"
	"testing"

	"vault/internal/accountproof"
	"vault/internal/credential"
	"vault/internal/service/account"
	"vault/internal/service/panel"
	"vault/internal/store/postgres"
)

func (f fixture) authentication() (*account.Authentication, *panel.Service) {
	p := panel.New(postgres.NewPanelStore(f.pool))
	return account.NewAuthentication(postgres.NewAccountStore(f.pool), p), p
}

func (f fixture) identity(t *testing.T, role string) (string, []byte) {
	t.Helper()
	encoded, err := credential.Hash(password)
	if err != nil {
		t.Fatal(err)
	}
	var id string
	if err = f.pool.QueryRow(f.ctx, `INSERT INTO users(email,panel_role,panel_managed,panel_password)
		VALUES($1,$2,true,$3) RETURNING id::text`, role+"@example.test", role, encoded).Scan(&id); err != nil {
		t.Fatal(err)
	}
	return id, encoded
}

func TestAccessSessionIntegration(t *testing.T) {
	f := setup(t)
	id, _ := f.identity(t, "user")
	auth, p := f.authentication()
	api, err := auth.Login(f.ctx, " USER@example.test ", password)
	if err != nil {
		t.Fatal(err)
	}
	actor, err := auth.Session(f.ctx, api)
	if err != nil || actor.ID != id || len(actor.Permissions) != 0 || actor.Can("accounts.write") {
		t.Fatal("wrong API authority", err)
	}
	if _, err = p.Session(f.ctx, api); !errors.Is(err, postgres.ErrNotFound) {
		t.Fatal("API token became browser session", err)
	}
	browser, err := p.Login(f.ctx, "user@example.test", password)
	if err != nil {
		t.Fatal(err)
	}
	if _, err = auth.Session(f.ctx, browser); !errors.Is(err, postgres.ErrNotFound) {
		t.Fatal("browser session became API token", err)
	}
	digest, _ := accountproof.Digest(api)
	var stored []byte
	var ttl, raw bool
	if err = f.pool.QueryRow(f.ctx, `SELECT token_hash,expires_at-created_at=interval '15 minutes',
		position($1 in row_to_json(s)::text)>0 FROM account_access_sessions s`, api).Scan(&stored, &ttl, &raw); err != nil || !ttl || raw || !bytes.Equal(stored, digest) {
		t.Fatal("unsafe token persistence", err)
	}
	if err = auth.Logout(f.ctx, api); err != nil {
		t.Fatal(err)
	}
	if err = auth.Logout(f.ctx, api); err != nil {
		t.Fatal("logout not idempotent", err)
	}
	if err = auth.Logout(f.ctx, "malformed"); err != nil {
		t.Fatal(err)
	}
	if _, err = auth.Session(f.ctx, api); !errors.Is(err, postgres.ErrNotFound) {
		t.Fatal("revoked access survived", err)
	}
	if _, err = auth.Session(f.ctx, "malformed"); !errors.Is(err, postgres.ErrNotFound) {
		t.Fatal(err)
	}
}

func TestAccessRoleAndCredentialIntegration(t *testing.T) {
	f := setup(t)
	for _, role := range []string{"owner", "admin", "support", "user"} {
		f.identity(t, role)
	}
	auth, _ := f.authentication()
	for _, email := range []string{"owner@example.test", "admin@example.test", "support@example.test", "absent@example.test", "malformed"} {
		if _, err := auth.Login(f.ctx, email, password); !errors.Is(err, postgres.ErrNotFound) {
			t.Fatal("staff/absent native login differed", err)
		}
	}
	if _, err := auth.Login(f.ctx, "user@example.test", password+"x"); !errors.Is(err, postgres.ErrNotFound) {
		t.Fatal(err)
	}
	token, err := auth.Login(f.ctx, "user@example.test", password)
	if err != nil {
		t.Fatal(err)
	}
	if _, err = f.pool.Exec(f.ctx, `UPDATE account_access_sessions SET created_at=created_at-interval '16 minutes',expires_at=expires_at-interval '16 minutes'`); err != nil {
		t.Fatal(err)
	}
	if _, err = auth.Session(f.ctx, token); !errors.Is(err, postgres.ErrNotFound) {
		t.Fatal("expired access accepted", err)
	}
	for _, mutation := range []string{"panel_active=false", "panel_role='support'", "panel_password=NULL"} {
		var old []byte
		var id string
		if err = f.pool.QueryRow(f.ctx, `SELECT id::text,panel_password FROM users WHERE email='user@example.test'`).Scan(&id, &old); err != nil {
			t.Fatal(err)
		}
		token, err = auth.Login(f.ctx, "user@example.test", password)
		if err != nil {
			t.Fatal(err)
		}
		if _, err = f.pool.Exec(f.ctx, `UPDATE users SET `+mutation+` WHERE id=$1`, id); err != nil {
			t.Fatal(err)
		}
		if err = postgres.NewAccountStore(f.pool).StartAccess(f.ctx, id, old, bytes.Repeat([]byte{8}, 32)); !errors.Is(err, postgres.ErrNotFound) {
			t.Fatal("stale login issued access", err)
		}
		if _, err = auth.Session(f.ctx, token); !errors.Is(err, postgres.ErrNotFound) {
			t.Fatal("changed account retained access", err)
		}
		if _, err = f.pool.Exec(f.ctx, `UPDATE users SET panel_active=true,panel_role='user',panel_password=$2 WHERE id=$1`, id, old); err != nil {
			t.Fatal(err)
		}
		if _, err = auth.Session(f.ctx, token); !errors.Is(err, postgres.ErrNotFound) {
			t.Fatal("old authority resurrected", err)
		}
	}
}
