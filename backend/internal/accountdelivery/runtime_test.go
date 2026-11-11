package accountdelivery

import (
	"context"
	"errors"
	"fmt"
	"os"
	"path/filepath"
	"strings"
	"testing"

	"vault/internal/config"
	"vault/internal/store/postgres"
	"vault/internal/testutil"
	"vault/migrations"
)

func TestPrivateAccountSecrets(t *testing.T) {
	directory := t.TempDir()
	if err := os.Chmod(directory, 0700); err != nil {
		t.Fatal(err)
	}
	path := filepath.Join(directory, "private-value")
	value := strings.Repeat("k", 32)
	if err := os.WriteFile(path, []byte(value), 0600); err != nil {
		t.Fatal(err)
	}
	for _, mode := range []os.FileMode{0400, 0600} {
		if err := os.Chmod(path, mode); err != nil {
			t.Fatal(err)
		}
		read, err := readSecret(path, 32)
		if err != nil || string(read) != value {
			t.Fatal("private file not read exactly")
		}
	}
	alias := filepath.Join(directory, "alias")
	if err := os.Symlink(path, alias); err != nil {
		t.Fatal(err)
	}
	rootAlias := filepath.Join(t.TempDir(), "private-root-alias")
	if err := os.Symlink(directory, rootAlias); err != nil {
		t.Fatal(err)
	}
	if _, err := readSecret(filepath.Join(rootAlias, "private-value"), 32); err == nil {
		t.Fatal("aliased secret root accepted")
	}
	for _, candidate := range []string{alias, filepath.Join(directory, "missing"), directory, "relative"} {
		if _, err := readSecret(candidate, 32); err == nil {
			t.Fatal("invalid secret source accepted")
		}
	}
	for _, mode := range []os.FileMode{0644, 0666} {
		if err := os.Chmod(path, mode); err != nil {
			t.Fatal(err)
		}
		_, err := readSecret(path, 32)
		if err == nil {
			t.Fatal("exposed secret accepted")
		}
		for _, format := range []string{"%v", "%+v", "%#v"} {
			if text := fmt.Sprintf(format, err); strings.Contains(text, directory) || strings.Contains(text, value) {
				t.Fatal("secret path/content leaked")
			}
		}
	}
	if err := os.Chmod(path, 0600); err != nil {
		t.Fatal(err)
	}
	if _, err := readSecret(path, 31); err == nil {
		t.Fatal("oversized secret accepted")
	}
	if _, err := readSecret(path, 0); err == nil {
		t.Fatal("invalid secret limit accepted")
	}
	if err := os.WriteFile(path, nil, 0600); err != nil {
		t.Fatal(err)
	}
	if _, err := readSecret(path, 32); err == nil {
		t.Fatal("empty secret accepted")
	}
	if err := os.Chmod(directory, 0777); err != nil {
		t.Fatal(err)
	}
	if _, err := readSecret(path, 32); err == nil {
		t.Fatal("writable secret root accepted")
	}
	if err := os.Chmod(directory, 0700); err != nil {
		t.Fatal(err)
	}
}

func TestAccountRuntimeActivationIntegration(t *testing.T) {
	if _, err := OpenRequests(config.AccountMailConfig{}, "development", nil); !errors.Is(err, ErrDisabled) {
		t.Fatal("disabled requests loaded secrets")
	}
	if _, err := OpenRegistration(config.AccountMailConfig{}, "development", nil); !errors.Is(err, ErrDisabled) {
		t.Fatal("disabled API required secrets")
	}
	if _, err := Open(config.AccountMailConfig{}, "development", nil); !errors.Is(err, ErrDisabled) {
		t.Fatal("disabled runtime required resources")
	}
	ctx, pool := testutil.Database(t)
	if err := postgres.Apply(ctx, pool, migrations.FS); err != nil {
		t.Fatal(err)
	}
	directory := t.TempDir()
	if err := os.Chmod(directory, 0700); err != nil {
		t.Fatal(err)
	}
	path := filepath.Join(directory, "proof-key")
	if err := os.WriteFile(path, []byte(strings.Repeat("m", 32)), 0600); err != nil {
		t.Fatal(err)
	}
	settings := config.AccountMailConfig{Enabled: "true", Origin: "http://localhost:8080", Address: "127.0.0.1:1025", Mode: "plain", From: "sender@example.test", ProofKeyFile: path}
	runtime, err := Open(settings, "development", pool)
	if err != nil {
		t.Fatal(err)
	}
	if err := runtime.Registration.Register(ctx, "runtime@example.test", "192.0.2.31"); err != nil {
		t.Fatal(err)
	}
	if _, err := Open(settings, "development", nil); err == nil {
		t.Fatal("missing runtime DB accepted")
	}
	invalid := settings
	invalid.Enabled = "bad"
	if _, err := Open(invalid, "development", pool); err == nil {
		t.Fatal("invalid runtime config accepted")
	}
	invalid = settings
	invalid.ProofKeyFile = filepath.Join(t.TempDir(), "missing")
	if _, err := Open(invalid, "development", pool); err == nil {
		t.Fatal("missing master accepted")
	}
	if err := os.WriteFile(path, []byte("short"), 0600); err != nil {
		t.Fatal(err)
	}
	if _, err := Open(settings, "development", pool); err == nil {
		t.Fatal("short master accepted")
	}
	if err := os.WriteFile(path, []byte(strings.Repeat("m", 32)), 0600); err != nil {
		t.Fatal(err)
	}
	password := filepath.Join(directory, "smtp-password")
	settings.Mode = "tls"
	settings.Username = "fixture"
	settings.PasswordFile = password
	requests, err := OpenRequests(settings, "development", pool)
	if err != nil || requests.Registration == nil || requests.Recovery == nil {
		t.Fatal("request runtime required SMTP credentials", err)
	}
	registration, err := OpenRegistration(settings, "development", pool)
	if err != nil {
		t.Fatal("API read the missing SMTP credential", err)
	}
	if err := registration.Register(ctx, "api-only@example.test", "192.0.2.32"); err != nil {
		t.Fatal(err)
	}
	if _, err := Open(settings, "development", pool); err == nil {
		t.Fatal("missing SMTP credential accepted")
	}
	if err := os.WriteFile(password, []byte("private-fixture-password"), 0600); err != nil {
		t.Fatal(err)
	}
	if _, err := Open(settings, "development", pool); err != nil {
		t.Fatal(err)
	}
	if err := os.WriteFile(password, []byte("invalid\npassword"), 0600); err != nil {
		t.Fatal(err)
	}
	if _, err := Open(settings, "development", pool); err == nil {
		t.Fatal("invalid SMTP credential accepted")
	}
	ctx, cancel := context.WithCancel(context.Background())
	cancel()
	if _, err := runtime.Delivery.Drain(ctx, 1); !errors.Is(err, context.Canceled) {
		t.Fatal("cancelled runtime did work")
	}
}
