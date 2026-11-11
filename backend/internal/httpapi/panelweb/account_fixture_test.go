package panelweb_test

import (
	"bytes"
	"net/http"
	"net/http/httptest"
	"strings"
	"testing"

	"vault/internal/accountproof"
	"vault/internal/httpapi"
	"vault/internal/service/account"
	"vault/internal/service/panel"
	"vault/internal/store/postgres"

	"github.com/jackc/pgx/v5/pgxpool"
)

func accountKey(t *testing.T) *accountproof.Key {
	t.Helper()
	key, err := accountproof.New(bytes.Repeat([]byte{57}, 32))
	require(t, err)
	return key
}

func accountServices(t *testing.T, pool *pgxpool.Pool, p *panel.Service) *httpapi.AccountHandler {
	t.Helper()
	store := postgres.NewAccountStore(pool)
	registration, err := account.NewRegistration(store, accountKey(t))
	require(t, err)
	return &httpapi.AccountHandler{Registration: registration, Verification: account.NewVerification(store),
		Authentication: account.NewAuthentication(store, p), Credentials: p}
}

func (h *harness) proof(email string) string {
	h.t.Helper()
	var user, operation string
	var encrypted []byte
	require(h.t, h.pool.QueryRow(h.ctx, `SELECT u.id::text,o.id::text,o.proof_ciphertext FROM users u
		JOIN account_mail_outbox o ON o.user_id=u.id WHERE u.email=$1 AND o.state='pending'`, email).Scan(&user, &operation, &encrypted))
	token, err := accountKey(h.t).Open(user, operation, encrypted)
	require(h.t, err)
	return token
}

func (h *harness) json(method, path, body, token string, status int, headers ...http.Header) *httptest.ResponseRecorder {
	h.t.Helper()
	r := httptest.NewRequest(method, testOrigin+path, strings.NewReader(body)).WithContext(h.ctx)
	r.Header.Set("Content-Type", "application/json")
	if token != "" {
		r.Header.Set("Authorization", "Bearer "+token)
	}
	for _, values := range headers {
		for key, values := range values {
			r.Header[key] = values
		}
	}
	out := httptest.NewRecorder()
	h.handler.ServeHTTP(out, r)
	if out.Code != status {
		h.t.Fatalf("%s %s status %d instead of %d", method, path, out.Code, status)
	}
	return out
}
