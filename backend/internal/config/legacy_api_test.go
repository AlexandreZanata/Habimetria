package config

import (
	"strings"
	"testing"
)

func TestLegacyAPIConfiguration(t *testing.T) {
	t.Setenv("LEGACY_API_MODE", "")
	if got := FromEnv().LegacyAPIMode; got != LegacyOwned {
		t.Fatalf("default legacy API mode = %q", got)
	}
	base := Config{Environment: "development", VerifyBaseURL: "http://localhost:8080", HMACKey: strings.Repeat("x", 32)}
	for _, mode := range []string{"", LegacyOwned, LegacyUnclaimedDevelopment} {
		cfg := base
		cfg.LegacyAPIMode = mode
		if err := cfg.Validate(); err != nil {
			t.Fatalf("local mode %q: %v", mode, err)
		}
	}
	for _, tc := range []struct{ env, verify, panel, mode string }{
		{"production", base.VerifyBaseURL, "", LegacyUnclaimedDevelopment},
		{"", base.VerifyBaseURL, "", LegacyUnclaimedDevelopment},
		{"development", "https://localhost:8080", "", LegacyUnclaimedDevelopment},
		{"development", "http://api.example.test", "", LegacyUnclaimedDevelopment},
		{"development", "", "", LegacyUnclaimedDevelopment},
		{"development", base.VerifyBaseURL, base.VerifyBaseURL, LegacyUnclaimedDevelopment},
		{"development", base.VerifyBaseURL, "", "ownedd"},
	} {
		cfg := Config{Environment: tc.env, VerifyBaseURL: tc.verify, PanelOrigin: tc.panel, LegacyAPIMode: tc.mode, HMACKey: base.HMACKey}
		if err := cfg.Validate(); err == nil {
			t.Fatalf("accepted unsafe/unknown legacy configuration: %+v", tc)
		}
	}
	for _, verify := range []string{"http://127.0.0.1:8080", "http://[::1]:8080"} {
		cfg := base
		cfg.VerifyBaseURL, cfg.LegacyAPIMode = verify, LegacyUnclaimedDevelopment
		if err := cfg.Validate(); err != nil {
			t.Fatalf("loopback rejected: %v", err)
		}
	}
	base.Environment, base.LegacyAPIMode = "production", LegacyOwned
	if err := base.Validate(); err != nil {
		t.Fatal(err)
	}
}
