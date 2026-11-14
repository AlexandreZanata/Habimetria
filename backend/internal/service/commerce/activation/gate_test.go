package activation_test

import (
	"context"
	"encoding/json"
	"errors"
	"fmt"
	"io"
	"os"
	"path/filepath"
	"strings"
	"testing"

	"vault/internal/config"
	"vault/internal/service/commerce/activation"
)

const secretFixture = "synthetic-sensitive-credential-do-not-log"

func fixture(t *testing.T) (config.Config, activation.StoreContext) {
	t.Helper()
	cfg := config.Config{Environment: "development", VerifyBaseURL: "http://localhost:8080", HMACKey: "development-fixture"}
	cfg.Commerce = config.CommerceConfig{Environment: config.Sandbox, NewPurchasesEnabled: "true", SecretsDirectory: t.TempDir(),
		Google: config.GooglePlayConfig{Enabled: "true", PackageName: "test.synthetic.habimetria", SecretReference: "sandbox/google_play/fixture.json"},
		Apple:  config.AppStoreConfig{Enabled: "true", BundleID: "test.synthetic.habimetria", AppID: "1234567890", IssuerID: "123e4567-e89b-12d3-a456-426614174000", KeyID: "TESTKEY123", SecretReference: "sandbox/app_store/fixture.p8"},
	}
	for _, store := range []string{config.GooglePlay, config.AppStore} {
		path := filepath.Join(cfg.Commerce.SecretsDirectory, cfg.Commerce.SecretReference(store))
		if err := os.MkdirAll(filepath.Dir(path), 0700); err != nil {
			t.Fatal(err)
		}
		if err := os.WriteFile(path, []byte(secretFixture), 0600); err != nil {
			t.Fatal(err)
		}
	}
	return cfg, activation.StoreContext{Store: config.GooglePlay, Environment: config.Sandbox, AppID: cfg.Commerce.Google.PackageName}
}

func prepare(t *testing.T, cfg config.Config, op activation.Operation, binding activation.StoreContext) activation.Prepared {
	t.Helper()
	gate, err := activation.New(cfg)
	if err != nil {
		t.Fatal(err)
	}
	p, err := gate.Prepare(context.Background(), op, binding)
	if err != nil {
		t.Fatal(err)
	}
	return p
}

func consume(t *testing.T, p activation.Prepared) string {
	t.Helper()
	bytes, err := io.ReadAll(p.Credential)
	if err != nil {
		t.Fatal(err)
	}
	if err := p.Credential.Close(); err != nil {
		t.Fatal(err)
	}
	return string(bytes)
}

func TestStorePreparationAndRedaction(t *testing.T) {
	cfg, binding := fixture(t)
	p := prepare(t, cfg, activation.NewPurchase, binding)
	if p.Context != binding || p.IssuerID != "" || p.KeyID != "" {
		t.Fatal("wrong Google setup context")
	}
	for _, value := range []any{p, p.Credential} {
		for _, format := range []string{"%v", "%+v", "%#v"} {
			if strings.Contains(fmt.Sprintf(format, value), secretFixture) {
				t.Fatal("formatted setup leaked credential")
			}
		}
		bytes, err := json.Marshal(value)
		if err != nil {
			t.Fatal(err)
		}
		if strings.Contains(string(bytes), secretFixture) {
			t.Fatal("serialized setup leaked credential")
		}
	}
	if consume(t, p) != secretFixture {
		t.Fatal("credential stream did not deliver exact configured bytes")
	}
	binding.Store, binding.AppID, binding.AppleAppID = config.AppStore, cfg.Commerce.Apple.BundleID, cfg.Commerce.Apple.AppID
	p = prepare(t, cfg, activation.Restore, binding)
	if p.IssuerID != cfg.Commerce.Apple.IssuerID || p.KeyID != cfg.Commerce.Apple.KeyID {
		t.Fatal("wrong Apple key context")
	}
	if consume(t, p) != secretFixture {
		t.Fatal("wrong Apple credential bytes")
	}
}

func TestNewSaleRollbackPreservesRecovery(t *testing.T) {
	cfg, binding := fixture(t)
	gate, err := activation.New(cfg)
	if err != nil {
		t.Fatal(err)
	}
	cfg.Commerce.NewPurchasesEnabled = "false"
	// Mutating a caller's copy cannot rewrite a running configuration snapshot.
	p, err := gate.Prepare(context.Background(), activation.NewPurchase, binding)
	if err != nil {
		t.Fatal(err)
	}
	consume(t, p)
	paused, err := activation.New(cfg)
	if err != nil {
		t.Fatal(err)
	}
	if _, err := paused.Prepare(context.Background(), activation.NewPurchase, binding); !errors.Is(err, activation.ErrNewPurchasesDisabled) {
		t.Fatal("new purchase remained exposed")
	}
	for _, op := range []activation.Operation{activation.Delivery, activation.Restore, activation.Reconcile, activation.Notification} {
		p, err := paused.Prepare(context.Background(), op, binding)
		if err != nil {
			t.Fatalf("new-sale rollback stopped %s: %v", op, err)
		}
		consume(t, p)
	}
	cfg.Commerce.Google.Enabled = "false"
	disabled, err := activation.New(cfg)
	if err != nil {
		t.Fatal(err)
	}
	if _, err := disabled.Prepare(context.Background(), activation.Restore, binding); !errors.Is(err, activation.ErrStoreDisabled) {
		t.Fatal("provider disable was not isolated")
	}
	// The other enabled provider still prepares with the same paused-sale policy.
	binding.Store, binding.AppleAppID = config.AppStore, cfg.Commerce.Apple.AppID
	p, err = disabled.Prepare(context.Background(), activation.Delivery, binding)
	if err != nil {
		t.Fatal(err)
	}
	consume(t, p)
}

func TestBindingAndInactiveConfigurationFailBeforeCredentials(t *testing.T) {
	cfg, binding := fixture(t)
	if err := os.RemoveAll(cfg.Commerce.SecretsDirectory); err != nil {
		t.Fatal(err)
	}
	gate, err := activation.New(cfg)
	if err != nil {
		t.Fatalf("construction eagerly consumed credentials: %v", err)
	}
	for _, mutate := range []func(*activation.StoreContext){
		func(c *activation.StoreContext) { c.Environment = config.Production }, func(c *activation.StoreContext) { c.AppID = "another.app" },
		func(c *activation.StoreContext) { c.AppleAppID = "123" },
	} {
		bad := binding
		mutate(&bad)
		if _, err := gate.Prepare(context.Background(), activation.Delivery, bad); !errors.Is(err, activation.ErrContext) {
			t.Fatal("namespace mismatch opened credentials")
		}
	}
	apple := binding
	apple.Store = config.AppStore
	if _, err := gate.Prepare(context.Background(), activation.Restore, apple); !errors.Is(err, activation.ErrContext) {
		t.Fatal("Apple numeric app ID was ignored")
	}
	if _, err := gate.Prepare(context.Background(), activation.Operation("local_pdf"), binding); !errors.Is(err, activation.ErrOperation) {
		t.Fatal("store gate accepted local operation")
	}
	unknown := binding
	unknown.Store = "other-store"
	if _, err := gate.Prepare(context.Background(), activation.Restore, unknown); !errors.Is(err, activation.ErrStoreDisabled) {
		t.Fatal("unknown store inherited active configuration")
	}
	ctx, cancel := context.WithCancel(context.Background())
	cancel()
	if _, err := gate.Prepare(ctx, activation.Delivery, binding); !errors.Is(err, context.Canceled) {
		t.Fatal("cancellation ignored")
	}
	if _, err := activation.New(config.Config{Environment: "production"}); err == nil {
		t.Fatal("activation bypassed server security configuration")
	}
	inactive, err := activation.New(config.Config{})
	if err != nil {
		t.Fatal(err)
	}
	if _, err := inactive.Prepare(context.Background(), activation.NewPurchase, binding); !errors.Is(err, activation.ErrStoreDisabled) {
		t.Fatal("default store became active")
	}
}
