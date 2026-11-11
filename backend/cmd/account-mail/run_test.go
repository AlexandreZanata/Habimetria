package main

import (
	"bytes"
	"context"
	"encoding/json"
	"io"
	"os"
	"path/filepath"
	"strings"
	"testing"
	"time"

	"vault/internal/accountdelivery"
	"vault/internal/config"
	"vault/internal/store/postgres"
	"vault/internal/testutil"
	"vault/internal/testutil/mailfixture"
	"vault/migrations"
)

func TestAccountMailStatusIntegration(t *testing.T) {
	ctx, pool := testutil.Database(t)
	if err := postgres.Apply(ctx, pool, migrations.FS); err != nil {
		t.Fatal(err)
	}
	t.Setenv("DATABASE_URL", testutil.DatabaseDSN(pool))
	t.Setenv("APP_ENV", "development")
	t.Setenv("ACCOUNT_MAIL_ENABLED", "false")
	t.Setenv("GOOGLE_PLAY_ENABLED", "true")
	t.Setenv("COMMERCE_ENV", "unconfigured-store-fixture") // Required account controls do not depend on billing setup.
	var output bytes.Buffer
	if err := run(ctx, []string{"--status"}, &output); err != nil {
		t.Fatal(err)
	}
	var status postgres.AccountMailStatus
	if err := json.Unmarshal(output.Bytes(), &status); err != nil || status.Pending != 0 {
		t.Fatal("safe queue status unavailable without SMTP")
	}
	for _, args := range [][]string{{"--limit", "0"}, {"--unknown"}, {"extra"}, {"--once", "--status"}} {
		if err := run(ctx, args, io.Discard); err == nil {
			t.Fatal("invalid worker options accepted")
		}
	}
	if err := run(ctx, []string{"--once"}, io.Discard); err == nil {
		t.Fatal("disabled sender was activated")
	}
	if err := run(ctx, []string{"--status"}, failedOutput{}); err == nil {
		t.Fatal("worker hid failed counter output")
	}
	t.Setenv("DATABASE_URL", "postgres://private-user:private-secret@localhost:bad/database")
	if err := run(ctx, []string{"--status"}, io.Discard); err == nil || strings.Contains(err.Error(), "private-secret") {
		t.Fatal("database failure hidden or DSN exposed")
	}
	t.Setenv("APP_ENV", "bad")
	if err := run(ctx, []string{"--status"}, io.Discard); err == nil {
		t.Fatal("invalid runtime environment accepted")
	}
}

func TestAccountMailWorkerDeliveryIntegration(t *testing.T) {
	capture := mailfixture.New(t)
	ctx, pool := testutil.Database(t)
	if err := postgres.Apply(ctx, pool, migrations.FS); err != nil {
		t.Fatal(err)
	}
	directory := t.TempDir()
	if err := os.Chmod(directory, 0700); err != nil {
		t.Fatal(err)
	}
	path := filepath.Join(directory, "proof-key")
	if err := os.WriteFile(path, []byte(strings.Repeat("q", 32)), 0600); err != nil {
		t.Fatal(err)
	}
	settings := config.AccountMailConfig{Enabled: "true", Origin: "http://localhost:8080", Address: capture.SMTP, Mode: "plain", From: "sender@example.test", ProofKeyFile: path}
	runtime, err := accountdelivery.Open(settings, "development", pool)
	if err != nil {
		t.Fatal(err)
	}
	if err := runtime.Registration.Register(ctx, "worker@example.test", "192.0.2.41"); err != nil {
		t.Fatal(err)
	}
	t.Setenv("DATABASE_URL", testutil.DatabaseDSN(pool))
	t.Setenv("APP_ENV", "development")
	for key, value := range map[string]string{"ACCOUNT_MAIL_ENABLED": "true", "ACCOUNT_MAIL_ORIGIN": settings.Origin, "ACCOUNT_MAIL_SMTP_ADDRESS": settings.Address,
		"ACCOUNT_MAIL_FROM": settings.From, "ACCOUNT_MAIL_SMTP_MODE": "plain", "ACCOUNT_MAIL_SMTP_USERNAME": "", "ACCOUNT_MAIL_SMTP_PASSWORD_FILE": "", "ACCOUNT_MAIL_PROOF_KEY_FILE": path} {
		t.Setenv(key, value)
	}
	var output bytes.Buffer
	if err := run(ctx, []string{"--once", "--limit", "1"}, &output); err != nil {
		t.Fatal(err)
	}
	if !strings.Contains(output.String(), `"Sent":1`) || strings.Contains(output.String(), "worker@example.test") || strings.Contains(output.String(), "verificar") {
		t.Fatal("worker counters are missing or secret-rich")
	}
	var operation string
	if err := pool.QueryRow(ctx, `SELECT id::text FROM account_mail_outbox WHERE state='sent'`).Scan(&operation); err != nil {
		t.Fatal(err)
	}
	capture.Find(t, operation, "localhost")
	daemonCtx, stop := context.WithTimeout(ctx, 500*time.Millisecond)
	if err := run(daemonCtx, nil, io.Discard); err != nil {
		t.Fatal("idle worker did not shut down gracefully")
	}
	stop()
	ctx, cancel := context.WithCancel(ctx)
	cancel()
	if err := run(ctx, []string{"--once"}, io.Discard); err == nil {
		t.Fatal("cancelled DB startup falsely accepted")
	}
}

type failedOutput struct{}

func (failedOutput) Write([]byte) (int, error) { return 0, io.ErrClosedPipe }
