package config

import "testing"

func commerceEnvironmentValues(c CommerceConfig) map[string]string {
	return map[string]string{
		"COMMERCE_ENV": c.Environment, "COMMERCE_NEW_PURCHASES_ENABLED": c.NewPurchasesEnabled, "COMMERCE_SECRETS_DIR": c.SecretsDirectory,
		"GOOGLE_PLAY_ENABLED": c.Google.Enabled, "GOOGLE_PLAY_PACKAGE_NAME": c.Google.PackageName, "GOOGLE_PLAY_SECRET_REF": c.Google.SecretReference,
		"APP_STORE_ENABLED": c.Apple.Enabled, "APP_STORE_BUNDLE_ID": c.Apple.BundleID, "APP_STORE_APP_ID": c.Apple.AppID,
		"APP_STORE_ISSUER_ID": c.Apple.IssuerID, "APP_STORE_KEY_ID": c.Apple.KeyID, "APP_STORE_SECRET_REF": c.Apple.SecretReference,
	}
}

func TestCommerceEnvironmentOverrides(t *testing.T) {
	expected := commerceFixture()
	for key, value := range commerceEnvironmentValues(expected) {
		t.Setenv(key, value)
	}
	got := FromEnv().Commerce
	if got != expected {
		t.Fatal("commercial environment override not loaded exactly")
	}
	if err := got.Validate("development"); err != nil {
		t.Fatal(err)
	}
}

func TestCommerceFlagsDefaultDisabled(t *testing.T) {
	for key := range commerceEnvironmentValues(CommerceConfig{}) {
		t.Setenv(key, "")
	}
	c := FromEnv()
	if c.Commerce.Google.Enabled != "false" || c.Commerce.Apple.Enabled != "false" || c.Commerce.NewPurchasesEnabled != "false" {
		t.Fatal("commercial flag enabled by default")
	}
	if c.Commerce.Environment != "" || c.Commerce.SecretsDirectory != "/run/secrets/habimetria" {
		t.Fatal("commercial defaults invented an environment or secret")
	}
	if err := c.Commerce.Validate(c.Environment); err != nil {
		t.Fatal(err)
	}
}

func TestConfigValidatesActiveCommerce(t *testing.T) {
	c := Config{Environment: "development", VerifyBaseURL: "http://localhost:8080", HMACKey: devHMAC}
	if err := c.Validate(); err != nil {
		t.Fatal(err)
	}
	c.Commerce = commerceFixture()
	c.Commerce.Apple.SecretReference = "production/app_store/fixture.p8"
	if err := c.Validate(); err == nil {
		t.Fatal("server startup accepted mixed credential namespaces")
	}
}
