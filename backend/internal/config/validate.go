package config

import (
	"errors"
	"strings"
)

// devHMAC is the committed dev-only default. It must never sign
// manifests in a shared/beta environment.
const devHMAC = "dev-only-change-me"

// Validate fails fast on unsafe trust configuration. Dev defaults stay
// valid for local runs (PAYMENTS_MODE=off); live mode requires a real
// HMAC key. Call it at boot before listening.
func (c Config) Validate() error {
	if strings.HasPrefix(c.PanelOrigin, "https://") && (len(c.HMACKey) < 32 || c.HMACKey == devHMAC) {
		return errors.New("HMAC_KEY must be a random key of at least 32 bytes for an HTTPS panel")
	}
	if c.PaymentsMode == "live" {
		if c.HMACKey == "" || c.HMACKey == devHMAC {
			return errors.New("HMAC_KEY must be set to a random value when PAYMENTS_MODE=live")
		}
	}
	return nil
}
