package config

import "errors"

// devHMAC is the committed dev-only default. It must never sign
// manifests in a shared/beta environment.
const devHMAC = "dev-only-change-me"

// Validate fails fast on unsafe trust configuration. Dev defaults stay
// valid for local runs (PAYMENTS_MODE=off); live mode requires a real
// HMAC key. Call it at boot before listening.
func (c Config) Validate() error {
	if c.PaymentsMode == "live" {
		if c.HMACKey == "" || c.HMACKey == devHMAC {
			return errors.New("HMAC_KEY must be set to a random value when PAYMENTS_MODE=live")
		}
	}
	return nil
}
