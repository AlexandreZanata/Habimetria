package config

import (
	"strings"
	"testing"
)

// Dev defaults stay bootable; live mode requires a real HMAC key.
func TestValidate(t *testing.T) {
	dev := Config{PaymentsMode: "off", HMACKey: "dev-only-change-me"}
	if err := dev.Validate(); err != nil {
		t.Fatalf("dev config should validate: %v", err)
	}
	liveBad := Config{PaymentsMode: "live", HMACKey: "dev-only-change-me"}
	if err := liveBad.Validate(); err == nil {
		t.Fatal("live config with dev HMAC_KEY should fail")
	}
	liveEmpty := Config{PaymentsMode: "live", HMACKey: ""}
	if err := liveEmpty.Validate(); err == nil {
		t.Fatal("live config with empty HMAC_KEY should fail")
	}
	liveOK := Config{PaymentsMode: "live", HMACKey: "live-test-hmac-key-not-a-secret"}
	if err := liveOK.Validate(); err != nil {
		t.Fatalf("live config with real key should validate: %v", err)
	}
}

func TestHTTPSPanelRequiresProductionHMAC(t *testing.T) {
	cfg := Config{PanelOrigin: "https://panel.example.test", HMACKey: devHMAC}
	if err := cfg.Validate(); err == nil {
		t.Fatal("HTTPS panel accepted the development signing key")
	}
	cfg.HMACKey = strings.Repeat("x", 32)
	if err := cfg.Validate(); err != nil {
		t.Fatalf("HTTPS panel rejected a configured signing key: %v", err)
	}
}
