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

// Validate protects server signing independently of billing. Only development
// with local HTTP origins may use the committed development key.
func (c Config) Validate() error {
	if c.Environment != "" && c.Environment != "development" && c.Environment != "production" {
		return errors.New("APP_ENV must be development or production")
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
	return nil
}
