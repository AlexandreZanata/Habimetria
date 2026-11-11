package config

import (
	"errors"
	"fmt"
	"net"
	"net/url"
	"strings"
)

// devHMAC is the committed dev-only default. It must never sign
// manifests in a shared/beta environment.
const devHMAC = "dev-only-change-me"

const (
	LegacyOwned                = "owned"
	LegacyUnclaimedDevelopment = "unclaimed-development"
)

// Validate protects signing and the legacy ownership boundary independently of
// billing. Only local HTTP development may use the development key/exception.
func (c Config) Validate() error {
	if c.Environment != "" && c.Environment != "development" && c.Environment != "production" {
		return errors.New("APP_ENV must be development or production")
	}
	if c.LegacyAPIMode != "" && c.LegacyAPIMode != LegacyOwned && c.LegacyAPIMode != LegacyUnclaimedDevelopment {
		return errors.New("LEGACY_API_MODE must be owned or unclaimed-development")
	}
	protected := c.Environment == "production"
	for name, origin := range map[string]string{"VERIFY_BASE_URL": c.VerifyBaseURL, "PANEL_ORIGIN": c.PanelOrigin} {
		if origin == "" {
			continue
		}
		u, err := url.Parse(origin)
		if err != nil || (u.Scheme != "http" && u.Scheme != "https") || u.Hostname() == "" || u.User != nil {
			return fmt.Errorf("%s must be an HTTP(S) URL without credentials", name)
		}
		local := strings.EqualFold(u.Hostname(), "localhost")
		if ip := net.ParseIP(u.Hostname()); ip != nil {
			local = ip.IsLoopback()
		}
		protected = protected || u.Scheme == "https" || !local
	}
	if protected && (len(c.HMACKey) < 32 || c.HMACKey == devHMAC || strings.TrimSpace(c.HMACKey) == "") {
		return errors.New("HMAC_KEY must be a random key of at least 32 bytes for production or non-local/HTTPS origins")
	}
	if c.LegacyAPIMode == LegacyUnclaimedDevelopment &&
		(c.Environment != "development" || protected || c.VerifyBaseURL == "" || c.PanelOrigin != "") {
		return errors.New("unclaimed-development requires APP_ENV=development, a local HTTP VERIFY_BASE_URL and no PANEL_ORIGIN")
	}
	return c.Commerce.Validate(c.Environment)
}
