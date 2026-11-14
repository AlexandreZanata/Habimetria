package account_test

import (
	"testing"

	"vault/internal/service/account"
	"vault/internal/store/postgres"
)

const replacement = "replacement fixture password"

func (f fixture) recovery(t *testing.T) (*account.RecoveryRequest, *account.PasswordReset) {
	t.Helper()
	store := postgres.NewAccountStore(f.pool)
	request, err := account.NewRecoveryRequest(store, f.key)
	if err != nil {
		t.Fatal(err)
	}
	return request, account.NewPasswordReset(store)
}

func (f fixture) recoveryProof(t *testing.T) string {
	t.Helper()
	var user, operation string
	var encrypted []byte
	if err := f.pool.QueryRow(f.ctx, `SELECT user_id::text,id::text,proof_ciphertext FROM account_mail_outbox
		WHERE template='password_recovery' AND state IN ('pending','sending')`).Scan(&user, &operation, &encrypted); err != nil {
		t.Fatal(err)
	}
	token, err := f.key.OpenRecovery(user, operation, encrypted)
	if err != nil {
		t.Fatal(err)
	}
	return token
}

func (f fixture) exec(t *testing.T, sql string) {
	t.Helper()
	if _, err := f.pool.Exec(f.ctx, sql); err != nil {
		t.Fatal(err)
	}
}
