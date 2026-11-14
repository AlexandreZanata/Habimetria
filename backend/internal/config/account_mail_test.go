package config

import (
	"strings"
	"testing"
)

func mailEnvironmentValues(c AccountMailConfig) map[string]string {
	return map[string]string{"ACCOUNT_MAIL_ENABLED": c.Enabled, "ACCOUNT_MAIL_ORIGIN": c.Origin, "ACCOUNT_MAIL_SMTP_ADDRESS": c.Address,
		"ACCOUNT_MAIL_FROM": c.From, "ACCOUNT_MAIL_SMTP_MODE": c.Mode, "ACCOUNT_MAIL_SMTP_USERNAME": c.Username,
		"ACCOUNT_MAIL_SMTP_PASSWORD_FILE": c.PasswordFile, "ACCOUNT_MAIL_PROOF_KEY_FILE": c.ProofKeyFile}
}

func TestAccountMailConfiguration(t *testing.T) {
	valid := AccountMailConfig{Enabled: "true", Origin: "https://accounts.example.test", Address: "smtp.example.test:465", From: "accounts@example.test", Mode: "tls",
		Username: "smtp-fixture", PasswordFile: "/run/secrets/account-mail/password", ProofKeyFile: "/run/secrets/account-mail/proof-key"}
	if err := valid.Validate("production"); err != nil {
		t.Fatal(err)
	}
	for _, edit := range []func(*AccountMailConfig){
		func(c *AccountMailConfig) { c.Enabled = "TRUE" }, func(c *AccountMailConfig) { c.Origin = "https://user:secret@example.test" },
		func(c *AccountMailConfig) { c.Origin = "http://remote.example.test" }, func(c *AccountMailConfig) { c.Origin += "/path" },
		func(c *AccountMailConfig) { c.Address = "smtp://inline:secret@example.test" }, func(c *AccountMailConfig) { c.ProofKeyFile = "relative" },
		func(c *AccountMailConfig) { c.ProofKeyFile = "/run/../secret" }, func(c *AccountMailConfig) { c.PasswordFile = "inline-secret" },
		func(c *AccountMailConfig) { c.Username = "" }, func(c *AccountMailConfig) { c.PasswordFile = "" }, func(c *AccountMailConfig) { c.Mode = "optional" },
		func(c *AccountMailConfig) { c.Mode = "plain" }, func(c *AccountMailConfig) { c.ProofKeyFile = "/" },
	} {
		c := valid
		edit(&c)
		if err := c.Validate("production"); err == nil || strings.Contains(err.Error(), "inline-secret") {
			t.Fatal("invalid mail configuration accepted or echoed")
		}
	}
	local := AccountMailConfig{Enabled: "true", Origin: "http://localhost:8080", Address: "127.0.0.1:1025", From: "fixture@example.test", Mode: "plain", ProofKeyFile: "/tmp/fixture-key"}
	if err := local.Validate("development"); err != nil {
		t.Fatal(err)
	}
	if err := local.Validate("production"); err == nil {
		t.Fatal("plain SMTP enabled in production")
	}
	local.Mode = "tls"
	local.Origin = "https://localhost:8080"
	if err := local.Validate("production"); err == nil {
		t.Fatal("production unauthenticated SMTP accepted")
	}
	valid.Enabled = "false"
	valid.Address = "inactive-incomplete"
	if err := valid.Validate("production"); err != nil {
		t.Fatal("disabled mail blocked startup")
	}
	for key, value := range mailEnvironmentValues(local) {
		t.Setenv(key, value)
	}
	if got := FromEnv().AccountMail; got != local {
		t.Fatal("mail environment not forwarded exactly")
	}
	for key := range mailEnvironmentValues(local) {
		t.Setenv(key, "")
	}
	defaults := FromEnv().AccountMail
	if defaults.Enabled != "false" || defaults.Mode != "starttls" || defaults.ProofKeyFile != "" {
		t.Fatal("mail default enabled or invented a secret")
	}
	c := FromEnv()
	c.AccountMail.Enabled = "bad"
	if err := c.Validate(); err == nil {
		t.Fatal("top-level validation skipped mail")
	}
}
