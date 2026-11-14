package httpapi

import (
	"bytes"
	"context"
	"net/http"
	"net/http/httptest"
	"strings"
	"testing"

	"vault/internal/accountproof"
	"vault/internal/service/account"
	"vault/internal/service/panel"
	"vault/internal/store/postgres"
	"vault/internal/testutil"
	"vault/migrations"

	"github.com/jackc/pgx/v5/pgxpool"
)

type accountFixture struct {
	t       *testing.T
	ctx     context.Context
	pool    *pgxpool.Pool
	key     *accountproof.Key
	handler http.Handler
}

const accountPassword = "synthetic API fixture password"

func accountSetup(t *testing.T) accountFixture {
	t.Helper()
	ctx, pool := testutil.Database(t)
	if err := postgres.Apply(ctx, pool, migrations.FS); err != nil {
		t.Fatal(err)
	}
	key, err := accountproof.New(bytes.Repeat([]byte{61}, 32))
	if err != nil {
		t.Fatal(err)
	}
	store := postgres.NewAccountStore(pool)
	p := panel.New(postgres.NewPanelStore(pool))
	registration, err := account.NewRegistration(store, key)
	if err != nil {
		t.Fatal(err)
	}
	recovery, err := account.NewRecoveryRequest(store, key)
	if err != nil {
		t.Fatal(err)
	}
	h := &AccountHandler{Registration: registration, Verification: account.NewVerification(store), Authentication: account.NewAuthentication(store, p), Credentials: p,
		Recovery: recovery, PasswordReset: account.NewPasswordReset(store)}
	return accountFixture{t, ctx, pool, key, NewRouter(Deps{Account: h})}
}

func (f accountFixture) call(method, path, body, token string, status int) *httptest.ResponseRecorder {
	f.t.Helper()
	r := httptest.NewRequest(method, "http://localhost/api/v1/account/"+path, strings.NewReader(body)).WithContext(f.ctx)
	r.Header.Set("Content-Type", "application/json")
	if token != "" {
		r.Header.Set("Authorization", "Bearer "+token)
	}
	out := httptest.NewRecorder()
	f.handler.ServeHTTP(out, r)
	if out.Code != status || out.Header().Get("Cache-Control") != "no-store" {
		f.t.Fatalf("%s %s returned %d instead of %d", method, path, out.Code, status)
	}
	return out
}

func (f accountFixture) sql(query string) {
	f.t.Helper()
	if _, err := f.pool.Exec(f.ctx, query); err != nil {
		f.t.Fatal(err)
	}
}

func (f accountFixture) proof() string {
	f.t.Helper()
	var user, id string
	var envelope []byte
	if err := f.pool.QueryRow(f.ctx, `SELECT user_id::text,id::text,proof_ciphertext FROM account_mail_outbox WHERE state='pending'`).Scan(&user, &id, &envelope); err != nil {
		f.t.Fatal(err)
	}
	proof, err := f.key.Open(user, id, envelope)
	if err != nil {
		f.t.Fatal(err)
	}
	return proof
}
