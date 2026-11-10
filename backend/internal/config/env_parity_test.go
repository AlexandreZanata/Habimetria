package config

import (
	"os"
	"path/filepath"
	"strings"
	"testing"
)

// infra/.env.example must match FromEnv defaults so a host run with
// `cp infra/.env.example infra/.env` behaves like unset env.
// Compose may override service addresses via ${VAR:-container-default};
// this test pins the host-run contract only.
func TestEnvExampleMatchesFromEnv(t *testing.T) {
	keys := []string{
		"PORT", "DATABASE_URL", "VERIFY_BASE_URL", "PANEL_ORIGIN",
		"STORAGE_BACKEND", "S3_ENDPOINT", "S3_REGION", "S3_BUCKET",
		"S3_ACCESS_KEY", "S3_SECRET_KEY", "S3_PATH_STYLE", "LOCAL_STORAGE_DIR",
		"HMAC_KEY", "PAYMENTS_MODE", "PAYMENTS_PROVIDER",
		"STRIPE_SECRET_KEY", "STRIPE_API_BASE", "ASAAS_API_KEY", "ASAAS_API_BASE",
		"SEAL_PRICE_CENTS", "SEAL_CURRENCY",
	}
	saved := make(map[string]string, len(keys))
	for _, k := range keys {
		if v, ok := os.LookupEnv(k); ok {
			saved[k] = v
		}
		if err := os.Unsetenv(k); err != nil {
			t.Fatalf("unset %s: %v", k, err)
		}
	}
	t.Cleanup(func() {
		for _, k := range keys {
			if v, ok := saved[k]; ok {
				_ = os.Setenv(k, v)
			} else {
				_ = os.Unsetenv(k)
			}
		}
	})

	raw, err := os.ReadFile(filepath.Join("..", "..", "..", "infra", ".env.example"))
	if err != nil {
		t.Fatalf("read .env.example: %v", err)
	}
	file := map[string]string{}
	for _, line := range strings.Split(string(raw), "\n") {
		line = strings.TrimSpace(line)
		if line == "" || strings.HasPrefix(line, "#") || !strings.Contains(line, "=") {
			continue
		}
		k, v, _ := strings.Cut(line, "=")
		file[strings.TrimSpace(k)] = strings.TrimSpace(v)
	}

	got := FromEnv()
	want := map[string]string{
		"PORT": got.Port, "DATABASE_URL": got.DatabaseURL,
		"VERIFY_BASE_URL": got.VerifyBaseURL, "STORAGE_BACKEND": got.StorageBackend,
		"PANEL_ORIGIN": got.PanelOrigin,
		"S3_ENDPOINT":  got.S3Endpoint, "S3_REGION": got.S3Region,
		"S3_BUCKET": got.S3Bucket, "S3_ACCESS_KEY": got.S3AccessKey,
		"S3_SECRET_KEY":     got.S3SecretKey,
		"S3_PATH_STYLE":     map[bool]string{true: "true", false: "false"}[got.S3PathStyle],
		"LOCAL_STORAGE_DIR": got.LocalDir, "HMAC_KEY": got.HMACKey,
		"PAYMENTS_MODE": got.PaymentsMode, "PAYMENTS_PROVIDER": got.PaymentsProvider,
		"STRIPE_SECRET_KEY": got.StripeSecretKey, "STRIPE_API_BASE": got.StripeAPIBase,
		"ASAAS_API_KEY": got.AsaasAPIKey, "ASAAS_API_BASE": got.AsaasAPIBase,
		"SEAL_PRICE_CENTS": got.SealPriceCents, "SEAL_CURRENCY": got.SealCurrency,
	}
	for _, k := range keys {
		fv, ok := file[k]
		if !ok {
			t.Errorf(".env.example missing key %s (FromEnv default %q)", k, want[k])
			continue
		}
		if fv != want[k] {
			t.Errorf(".env.example %s=%q, FromEnv default %q", k, fv, want[k])
		}
	}
}
