package config

import (
	"fmt"
	"path/filepath"
	"regexp"
	"strconv"
	"strings"

	"github.com/google/uuid"
)

var androidIdentity = regexp.MustCompile(`^[A-Za-z][A-Za-z0-9_]*(\.[A-Za-z][A-Za-z0-9_]*)+$`)
var appleIdentity = regexp.MustCompile(`^[A-Za-z0-9][A-Za-z0-9-]*(\.[A-Za-z0-9][A-Za-z0-9-]*)+$`)
var appleKeyID = regexp.MustCompile(`^[A-Z0-9]{10}$`)
var secretFilename = regexp.MustCompile(`^[A-Za-z0-9][A-Za-z0-9_.-]{0,127}$`)

func (c CommerceConfig) Validate(serverEnvironment string) error {
	for name, value := range map[string]string{"COMMERCE_NEW_PURCHASES_ENABLED": c.NewPurchasesEnabled, "GOOGLE_PLAY_ENABLED": c.Google.Enabled, "APP_STORE_ENABLED": c.Apple.Enabled} {
		if value != "" && value != "false" && value != "true" {
			return fmt.Errorf("%s must be true or false", name)
		}
	}
	google, apple := c.StoreEnabled(GooglePlay), c.StoreEnabled(AppStore)
	if c.NewPurchasesEnabled == "true" && !google && !apple {
		return fmt.Errorf("new purchases require an enabled store")
	}
	if !google && !apple {
		return nil
	}
	if c.Environment != Sandbox && c.Environment != Production {
		return fmt.Errorf("COMMERCE_ENV must select sandbox or production")
	}
	if c.Environment == Production && serverEnvironment != Production {
		return fmt.Errorf("production commerce requires APP_ENV=production")
	}
	if !filepath.IsAbs(c.SecretsDirectory) || filepath.Clean(c.SecretsDirectory) != c.SecretsDirectory || c.SecretsDirectory == "/" || strings.ContainsRune(c.SecretsDirectory, 0) {
		return fmt.Errorf("COMMERCE_SECRETS_DIR must be an absolute normalized credential directory")
	}
	if google {
		if len(c.Google.PackageName) > 255 || !androidIdentity.MatchString(c.Google.PackageName) {
			return fmt.Errorf("GOOGLE_PLAY_PACKAGE_NAME is required and must identify the configured app")
		}
		if err := c.ValidateReference(GooglePlay, c.Google.SecretReference); err != nil {
			return err
		}
	}
	if apple {
		if len(c.Apple.BundleID) > 255 || !appleIdentity.MatchString(c.Apple.BundleID) {
			return fmt.Errorf("APP_STORE_BUNDLE_ID is required and must identify the configured app")
		}
		app, err := strconv.ParseUint(c.Apple.AppID, 10, 64)
		if err != nil || app == 0 || strconv.FormatUint(app, 10) != c.Apple.AppID {
			return fmt.Errorf("APP_STORE_APP_ID must be a configured positive numeric app ID")
		}
		issuer, err := uuid.Parse(c.Apple.IssuerID)
		if err != nil || issuer == uuid.Nil || !strings.EqualFold(issuer.String(), c.Apple.IssuerID) {
			return fmt.Errorf("APP_STORE_ISSUER_ID must be a configured issuer UUID")
		}
		if !appleKeyID.MatchString(c.Apple.KeyID) {
			return fmt.Errorf("APP_STORE_KEY_ID must be the configured signing key identifier")
		}
		if err := c.ValidateReference(AppStore, c.Apple.SecretReference); err != nil {
			return err
		}
	}
	return nil
}

func (c CommerceConfig) ValidateReference(store, reference string) error {
	if (c.Environment != Sandbox && c.Environment != Production) || (store != GooglePlay && store != AppStore) {
		return fmt.Errorf("unknown credential namespace")
	}
	parts := strings.Split(reference, "/")
	if len(parts) != 3 || parts[0] != c.Environment || parts[1] != store || !secretFilename.MatchString(parts[2]) {
		return fmt.Errorf("%s credential reference must match configured environment/store", store)
	}
	return nil
}
