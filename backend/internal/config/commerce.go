package config

const (
	GooglePlay = "google_play"
	AppStore   = "app_store"
	Sandbox    = "sandbox"
	Production = "production"
)

type GooglePlayConfig struct{ Enabled, PackageName, SecretReference string }
type AppStoreConfig struct{ Enabled, BundleID, AppID, IssuerID, KeyID, SecretReference string }

// Values are references/identifiers, never provider credentials. Comparable
// value structs make each deployment snapshot independent of later env edits.
type CommerceConfig struct {
	Environment         string
	NewPurchasesEnabled string
	SecretsDirectory    string
	Google              GooglePlayConfig
	Apple               AppStoreConfig
}

func commerceFromEnv() CommerceConfig {
	return CommerceConfig{
		Environment:         envOr("COMMERCE_ENV", ""),
		NewPurchasesEnabled: envOr("COMMERCE_NEW_PURCHASES_ENABLED", "false"),
		SecretsDirectory:    envOr("COMMERCE_SECRETS_DIR", "/run/secrets/habimetria"),
		Google: GooglePlayConfig{Enabled: envOr("GOOGLE_PLAY_ENABLED", "false"),
			PackageName: envOr("GOOGLE_PLAY_PACKAGE_NAME", ""), SecretReference: envOr("GOOGLE_PLAY_SECRET_REF", "")},
		Apple: AppStoreConfig{Enabled: envOr("APP_STORE_ENABLED", "false"), BundleID: envOr("APP_STORE_BUNDLE_ID", ""),
			AppID: envOr("APP_STORE_APP_ID", ""), IssuerID: envOr("APP_STORE_ISSUER_ID", ""),
			KeyID: envOr("APP_STORE_KEY_ID", ""), SecretReference: envOr("APP_STORE_SECRET_REF", "")},
	}
}

func (c CommerceConfig) StoreEnabled(store string) bool {
	switch store {
	case GooglePlay:
		return c.Google.Enabled == "true"
	case AppStore:
		return c.Apple.Enabled == "true"
	}
	return false
}

func (c CommerceConfig) AppIdentity(store string) string {
	switch store {
	case GooglePlay:
		return c.Google.PackageName
	case AppStore:
		return c.Apple.BundleID
	}
	return ""
}

func (c CommerceConfig) SecretReference(store string) string {
	switch store {
	case GooglePlay:
		return c.Google.SecretReference
	case AppStore:
		return c.Apple.SecretReference
	}
	return ""
}
