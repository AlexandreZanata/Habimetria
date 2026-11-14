package main

import (
	"bytes"
	"os"
	"path/filepath"
	"testing"

	"vault/internal/accountproof"
	"vault/internal/config"
	"vault/internal/service/panel"
	"vault/internal/store/postgres"
	"vault/internal/testutil"
	"vault/migrations"
)

func TestAccountAvailabilityIntegration(t *testing.T) {
	ctx, pool := testutil.Database(t)
	if err := postgres.Apply(ctx, pool, migrations.FS); err != nil {
		t.Fatal(err)
	}
	directory := t.TempDir()
	if err := os.Chmod(directory, 0700); err != nil {
		t.Fatal(err)
	}
	path := filepath.Join(directory, "proof-key")
	master := bytes.Repeat([]byte{63}, 32)
	if err := os.WriteFile(path, master, 0600); err != nil {
		t.Fatal(err)
	}
	cfg := config.Config{Environment: "development", PanelOrigin: "http://localhost:8080",
		AccountMail: config.AccountMailConfig{Enabled: "true", Origin: "http://localhost:8080", Address: "127.0.0.1:1025", Mode: "plain", From: "sender@example.test", ProofKeyFile: path}}
	p := panel.New(postgres.NewPanelStore(pool))
	accounts := connectedAccount(cfg, pool, p)
	if accounts.Registration == nil || accounts.Recovery == nil || accounts.PasswordReset == nil {
		t.Fatal("valid configuration did not enable signup")
	}
	if err := accounts.Registration.Register(ctx, "runtime@example.test", "192.0.2.4"); err != nil {
		t.Fatal(err)
	}
	var user, operation string
	var ciphertext []byte
	if err := pool.QueryRow(ctx, `SELECT user_id::text,id::text,proof_ciphertext FROM account_mail_outbox`).Scan(&user, &operation, &ciphertext); err != nil {
		t.Fatal(err)
	}
	key, err := accountproof.New(master)
	if err != nil {
		t.Fatal(err)
	}
	proof, err := key.Open(user, operation, ciphertext)
	if err != nil {
		t.Fatal(err)
	}
	for _, mode := range []string{"disabled", "origin", "missing"} {
		changed := cfg
		switch mode {
		case "disabled":
			changed.AccountMail.Enabled = "false"
		case "origin":
			changed.AccountMail.Origin = "http://localhost:8081"
		case "missing":
			changed.AccountMail.ProofKeyFile = filepath.Join(directory, "missing")
		}
		h := connectedAccount(changed, pool, p)
		if h.Registration != nil || h.Recovery != nil || h.PasswordReset == nil || h.Authentication == nil || h.Verification == nil || h.Credentials == nil {
			t.Fatal("signup rollback disabled account controls", mode)
		}
	}
	cfg.AccountMail.ProofKeyFile = filepath.Join(directory, "missing")
	h := connectedAccount(cfg, pool, p)
	const password = "synthetic runtime fixture password"
	if err := h.Verification.Verify(ctx, proof, password); err != nil {
		t.Fatal("missing mail key blocked delivered proof", err)
	}
	if _, err := h.Authentication.Login(ctx, "runtime@example.test", password); err != nil {
		t.Fatal("missing mail key blocked login", err)
	}
	if err := accounts.Recovery.Request(ctx, "runtime@example.test", "192.0.2.5"); err != nil {
		t.Fatal(err)
	}
	if err := pool.QueryRow(ctx, `SELECT user_id::text,id::text,proof_ciphertext FROM account_mail_outbox
		WHERE template='password_recovery'`).Scan(&user, &operation, &ciphertext); err != nil {
		t.Fatal(err)
	}
	recovery, err := key.OpenRecovery(user, operation, ciphertext)
	if err != nil {
		t.Fatal(err)
	}
	const replacement = "recovered runtime fixture password"
	if err := h.PasswordReset.Reset(ctx, recovery, replacement, replacement); err != nil {
		t.Fatal("missing mail key blocked recovery consumption", err)
	}
}
