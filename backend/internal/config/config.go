package config

import "os"

// Config is env-only. No config files in MVP.
type Config struct {
	Port        string
	DatabaseURL string
	S3Endpoint  string
	HMACKey     string
}

func FromEnv() Config {
	return Config{
		Port:        envOr("PORT", "8080"),
		DatabaseURL: envOr("DATABASE_URL", "postgres://vault:vault@postgres:5432/vault?sslmode=disable"),
		S3Endpoint:  envOr("S3_ENDPOINT", "http://minio:9000"),
		HMACKey:     envOr("HMAC_KEY", "dev-only-change-me"),
	}
}

func envOr(k, d string) string {
	if v := os.Getenv(k); v != "" {
		return v
	}
	return d
}
