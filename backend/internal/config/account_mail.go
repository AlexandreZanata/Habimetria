package config

import (
	"errors"
	"path/filepath"
	"strings"

	"vault/internal/mail"
)

type AccountMailConfig struct{ Enabled, Origin, Address, From, Mode, Username, PasswordFile, ProofKeyFile string }

func accountMailFromEnv() AccountMailConfig {
	return AccountMailConfig{Enabled: envOr("ACCOUNT_MAIL_ENABLED", "false"), Origin: envOr("ACCOUNT_MAIL_ORIGIN", ""),
		Address: envOr("ACCOUNT_MAIL_SMTP_ADDRESS", ""), From: envOr("ACCOUNT_MAIL_FROM", ""), Mode: envOr("ACCOUNT_MAIL_SMTP_MODE", "starttls"),
		Username: envOr("ACCOUNT_MAIL_SMTP_USERNAME", ""), PasswordFile: envOr("ACCOUNT_MAIL_SMTP_PASSWORD_FILE", ""), ProofKeyFile: envOr("ACCOUNT_MAIL_PROOF_KEY_FILE", "")}
}

func (c AccountMailConfig) SMTPSettings() mail.Settings {
	return mail.Settings{Address: c.Address, Mode: c.Mode, From: c.From, Username: c.Username, Origin: c.Origin}
}

func privatePath(path string) bool {
	return filepath.IsAbs(path) && filepath.Clean(path) == path && path != "/" && !strings.ContainsRune(path, 0)
}

func (c AccountMailConfig) Validate(environment string) error {
	if c.Enabled != "" && c.Enabled != "false" && c.Enabled != "true" {
		return errors.New("ACCOUNT_MAIL_ENABLED must be true or false")
	}
	if c.Enabled != "true" {
		return nil
	}
	if c.SMTPSettings().Validate() != nil || !privatePath(c.ProofKeyFile) ||
		((c.Username == "") != (c.PasswordFile == "")) || (c.PasswordFile != "" && !privatePath(c.PasswordFile)) {
		return errors.New("enabled account mail requires valid SMTP identity, origin and private file references")
	}
	if environment == "production" && (c.Mode == "plain" || !strings.HasPrefix(c.Origin, "https://") || c.Username == "") {
		return errors.New("production account mail requires HTTPS, TLS and SMTP authentication")
	}
	return nil
}
