package config

import (
	"strings"
	"testing"
)

// Local development stays bootable; production cannot use weak signing keys.
func TestValidate(t *testing.T) {
	dev := Config{Environment: "development", VerifyBaseURL: "http://localhost:8080", HMACKey: devHMAC}
	if err := dev.Validate(); err != nil {
		t.Fatalf("dev config should validate: %v", err)
	}
	for _, key := range []string{"", devHMAC, "too-short", strings.Repeat(" ", 32)} {
		cfg := Config{Environment: "production", HMACKey: key}
		if err := cfg.Validate(); err == nil {
			t.Fatal("production accepted an unsafe signing key")
		}
	}
	if err := (Config{Environment: "production", HMACKey: strings.Repeat("x", 32)}).Validate(); err != nil {
		t.Fatalf("production rejected configured key: %v", err)
	}
	if err := (Config{Environment: "prodution"}).Validate(); err == nil {
		t.Fatal("unknown environment silently fell back to development")
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

func TestAdvertisedOriginsRequireSafeSigning(t *testing.T) {
	for _, origin := range []string{"http://api.example.test", "https://localhost:8080", "https://api.example.test"} {
		cfg := Config{VerifyBaseURL: origin, HMACKey: devHMAC}
		if err := cfg.Validate(); err == nil {
			t.Fatalf("unsafe signing accepted for %s", origin)
		}
		cfg.HMACKey = strings.Repeat("x", 32)
		if err := cfg.Validate(); err != nil {
			t.Fatalf("configured signing rejected for %s: %v", origin, err)
		}
	}
	for _, origin := range []string{"http://127.0.0.1:8080", "http://[::1]:8080"} {
		if err := (Config{VerifyBaseURL: origin, HMACKey: devHMAC}).Validate(); err != nil {
			t.Fatalf("loopback development rejected: %v", err)
		}
	}
	for _, origin := range []string{"broken", "https://", "https://user:pass@example.test", "file:///tmp/test"} {
		if err := (Config{VerifyBaseURL: origin, HMACKey: strings.Repeat("x", 32)}).Validate(); err == nil {
			t.Fatalf("invalid advertised origin accepted: %s", origin)
		}
	}
}
