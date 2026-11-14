package config

import (
	"strings"
	"testing"
)

func commerceFixture() CommerceConfig {
	return CommerceConfig{Environment: Sandbox, NewPurchasesEnabled: "true", SecretsDirectory: "/run/secrets/habimetria",
		Google: GooglePlayConfig{Enabled: "true", PackageName: "test.synthetic.habimetria", SecretReference: "sandbox/google_play/fixture.json"},
		Apple: AppStoreConfig{Enabled: "true", BundleID: "test.synthetic.habimetria", AppID: "1234567890",
			IssuerID: "123e4567-e89b-12d3-a456-426614174000", KeyID: "TESTKEY123", SecretReference: "sandbox/app_store/fixture.p8"},
	}
}

func TestCommerceConditionalActivation(t *testing.T) {
	if err := (CommerceConfig{}).Validate("development"); err != nil {
		t.Fatal(err)
	}
	unused := commerceFixture()
	unused.Google.Enabled, unused.Apple.Enabled, unused.NewPurchasesEnabled = "false", "false", "false"
	unused.Environment = "unused"
	unused.SecretsDirectory = "missing"
	unused.Apple.IssuerID = "incomplete"
	if err := unused.Validate("development"); err != nil {
		t.Fatalf("inactive integration blocked startup: %v", err)
	}
	valid := commerceFixture()
	for _, appEnv := range []string{"development", "production"} {
		if err := valid.Validate(appEnv); err != nil {
			t.Fatal(err)
		}
	}
	googleOnly := valid
	googleOnly.Apple = AppStoreConfig{}
	if err := googleOnly.Validate("development"); err != nil {
		t.Fatalf("inactive Apple configuration required: %v", err)
	}
	appleOnly := valid
	appleOnly.Google = GooglePlayConfig{}
	if err := appleOnly.Validate("development"); err != nil {
		t.Fatalf("inactive Google configuration required: %v", err)
	}
	production := valid
	production.Environment = Production
	production.Google.SecretReference = "production/google_play/fixture.json"
	production.Apple.SecretReference = "production/app_store/fixture.p8"
	if err := production.Validate("production"); err != nil {
		t.Fatal(err)
	}
	if err := production.Validate("development"); err == nil {
		t.Fatal("development enabled production commerce")
	}
	if valid.AppIdentity(GooglePlay) != valid.Google.PackageName || valid.AppIdentity(AppStore) != valid.Apple.BundleID || valid.AppIdentity("unknown") != "" {
		t.Fatal("incorrect app namespace")
	}
	if valid.SecretReference(GooglePlay) != valid.Google.SecretReference || valid.SecretReference(AppStore) != valid.Apple.SecretReference || valid.SecretReference("unknown") != "" || valid.StoreEnabled("unknown") {
		t.Fatal("unknown provider inherited configuration")
	}
}

func TestCommerceInvalidConfigurationIsRedacted(t *testing.T) {
	const sensitive = "do-not-echo-fixture-value"
	for _, change := range []func(*CommerceConfig){
		func(c *CommerceConfig) { c.NewPurchasesEnabled = sensitive },
		func(c *CommerceConfig) { c.Google.Enabled = sensitive },
		func(c *CommerceConfig) { c.Apple.Enabled = sensitive },
		func(c *CommerceConfig) { c.Google.Enabled = "false"; c.Apple.Enabled = "false" },
		func(c *CommerceConfig) { c.Environment = sensitive },
		func(c *CommerceConfig) { c.SecretsDirectory = "" },
		func(c *CommerceConfig) { c.SecretsDirectory = "/" },
		func(c *CommerceConfig) { c.SecretsDirectory = "relative" },
		func(c *CommerceConfig) { c.SecretsDirectory = "/run/../secrets" },
		func(c *CommerceConfig) { c.SecretsDirectory = "/run/secret\x00name" },
		func(c *CommerceConfig) { c.Google.PackageName = sensitive },
		func(c *CommerceConfig) { c.Google.PackageName = strings.Repeat("a", 256) },
		func(c *CommerceConfig) { c.Google.SecretReference = sensitive },
		func(c *CommerceConfig) { c.Google.SecretReference = "production/google_play/fixture.json" },
		func(c *CommerceConfig) { c.Google.SecretReference = "sandbox/app_store/fixture.json" },
		func(c *CommerceConfig) { c.Google.SecretReference = "sandbox/google_play/../fixture.json" },
		func(c *CommerceConfig) { c.Google.SecretReference = "sandbox/google_play/." },
		func(c *CommerceConfig) { c.Apple.BundleID = sensitive },
		func(c *CommerceConfig) { c.Apple.BundleID = strings.Repeat("a", 256) },
		func(c *CommerceConfig) { c.Apple.AppID = sensitive },
		func(c *CommerceConfig) { c.Apple.AppID = "0" },
		func(c *CommerceConfig) { c.Apple.AppID = "001" },
		func(c *CommerceConfig) { c.Apple.IssuerID = sensitive },
		func(c *CommerceConfig) { c.Apple.IssuerID = "00000000-0000-0000-0000-000000000000" },
		func(c *CommerceConfig) { c.Apple.KeyID = sensitive },
		func(c *CommerceConfig) { c.Apple.SecretReference = "production/app_store/fixture.p8" },
	} {
		c := commerceFixture()
		change(&c)
		err := c.Validate("development")
		if err == nil {
			t.Fatal("invalid activated configuration accepted")
		}
		if strings.Contains(err.Error(), sensitive) {
			t.Fatal("validation echoed supplied configuration")
		}
	}
	if err := (CommerceConfig{}).ValidateReference("unknown", "inline-credential"); err == nil {
		t.Fatal("unknown namespace accepted")
	}
	if err := (CommerceConfig{Environment: Sandbox}).ValidateReference("unknown", "inline-credential"); err == nil {
		t.Fatal("unknown store accepted")
	}
}
