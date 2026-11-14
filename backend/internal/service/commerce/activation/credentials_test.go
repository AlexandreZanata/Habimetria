package activation_test

import (
	"context"
	"errors"
	"io"
	"os"
	"path/filepath"
	"strings"
	"testing"

	"vault/internal/service/commerce/activation"
)

func TestCredentialFileBoundsAndNamespaceIsolation(t *testing.T) {
	for _, scenario := range []string{"missing-root", "root-link", "missing-namespace", "namespace-link", "store-link", "file-link", "empty", "oversized", "public-mode", "directory", "missing-file"} {
		t.Run(scenario, func(t *testing.T) {
			cfg, binding := fixture(t)
			root := cfg.Commerce.SecretsDirectory
			path := filepath.Join(root, cfg.Commerce.Google.SecretReference)
			switch scenario {
			case "missing-root":
				if err := os.RemoveAll(root); err != nil {
					t.Fatal(err)
				}
			case "root-link":
				alias := filepath.Join(t.TempDir(), "secret-alias")
				if err := os.Symlink(root, alias); err != nil {
					t.Fatal(err)
				}
				cfg.Commerce.SecretsDirectory = alias
			case "missing-namespace":
				if err := os.RemoveAll(filepath.Join(root, "sandbox")); err != nil {
					t.Fatal(err)
				}
			case "namespace-link":
				if err := os.Rename(filepath.Join(root, "sandbox"), filepath.Join(root, "production")); err != nil {
					t.Fatal(err)
				}
				if err := os.Symlink("production", filepath.Join(root, "sandbox")); err != nil {
					t.Fatal(err)
				}
			case "store-link":
				if err := os.RemoveAll(filepath.Join(root, "sandbox", "google_play")); err != nil {
					t.Fatal(err)
				}
				if err := os.Symlink("app_store", filepath.Join(root, "sandbox", "google_play")); err != nil {
					t.Fatal(err)
				}
			case "file-link":
				if err := os.Remove(path); err != nil {
					t.Fatal(err)
				}
				if err := os.Symlink("../app_store/fixture.p8", path); err != nil {
					t.Fatal(err)
				}
			case "empty":
				if err := os.WriteFile(path, nil, 0600); err != nil {
					t.Fatal(err)
				}
			case "oversized":
				if err := os.WriteFile(path, make([]byte, activation.MaxCredentialBytes+1), 0600); err != nil {
					t.Fatal(err)
				}
			case "public-mode":
				if err := os.Chmod(path, 0644); err != nil {
					t.Fatal(err)
				}
			case "directory":
				if err := os.Remove(path); err != nil {
					t.Fatal(err)
				}
				if err := os.Mkdir(path, 0700); err != nil {
					t.Fatal(err)
				}
			case "missing-file":
				if err := os.Remove(path); err != nil {
					t.Fatal(err)
				}
			}
			gate, err := activation.New(cfg)
			if err != nil {
				t.Fatal(err)
			}
			if _, err := gate.Prepare(context.Background(), activation.Delivery, binding); !errors.Is(err, activation.ErrCredentialUnavailable) {
				t.Fatalf("unsafe credential became available: %v", err)
			} else if strings.Contains(err.Error(), root) || strings.Contains(err.Error(), secretFixture) {
				t.Fatal("credential failure leaked deployment data")
			}
		})
	}
}

func TestCredentialReadIsBoundedAndAtomicReplacementIsIsolated(t *testing.T) {
	cfg, binding := fixture(t)
	path := filepath.Join(cfg.Commerce.SecretsDirectory, cfg.Commerce.Google.SecretReference)
	if err := os.Chmod(path, 0400); err != nil {
		t.Fatal(err)
	}
	old := prepare(t, cfg, activation.Delivery, binding)
	staged := path + ".new"
	if err := os.WriteFile(staged, []byte("rotated-synthetic-credential"), 0600); err != nil {
		t.Fatal(err)
	}
	if err := os.Rename(staged, path); err != nil {
		t.Fatal(err)
	}
	if got := consume(t, old); got != secretFixture {
		t.Fatal("atomic replacement changed an open credential object")
	}
	if got := consume(t, prepare(t, cfg, activation.Delivery, binding)); got != "rotated-synthetic-credential" {
		t.Fatal("new preparation reused stale credentials")
	}
	bounded := prepare(t, cfg, activation.Delivery, binding)
	file, err := os.OpenFile(path, os.O_APPEND|os.O_WRONLY, 0600)
	if err != nil {
		t.Fatal(err)
	}
	if _, err := file.Write(make([]byte, activation.MaxCredentialBytes)); err != nil {
		t.Fatal(err)
	}
	if err := file.Close(); err != nil {
		t.Fatal(err)
	}
	if consume(t, bounded) != "rotated-synthetic-credential" {
		t.Fatal("credential reader exceeded inspected size")
	}
}

func TestClosedCredentialErrorsRemainRedacted(t *testing.T) {
	cfg, binding := fixture(t)
	p := prepare(t, cfg, activation.Delivery, binding)
	if err := p.Credential.Close(); err != nil {
		t.Fatal(err)
	}
	if _, err := io.ReadAll(p.Credential); !errors.Is(err, activation.ErrCredentialUnavailable) || strings.Contains(err.Error(), cfg.Commerce.SecretsDirectory) {
		t.Fatal("closed credential read was not safely reported")
	}
	if err := p.Credential.Close(); !errors.Is(err, activation.ErrCredentialUnavailable) {
		t.Fatal("close failure was discarded")
	}
}
