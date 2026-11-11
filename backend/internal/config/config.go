package config

import "os"

// Config is env-only. No config files in MVP.
type Config struct {
	Environment string // development | production; independent of billing
	Port        string
	DatabaseURL string
	S3Endpoint  string
	HMACKey     string

	StorageBackend string // s3 | local
	S3Region       string
	S3Bucket       string
	S3AccessKey    string
	S3SecretKey    string
	S3PathStyle    bool
	LocalDir       string

	VerifyBaseURL string // public origin embedded in QR codes
	PanelOrigin   string // optional origin for the authenticated web panel
}

func FromEnv() Config {
	return Config{
		Environment: envOr("APP_ENV", "development"),
		Port:        envOr("PORT", "8080"),
		DatabaseURL: envOr("DATABASE_URL", "postgres://vault:vault@localhost:5432/vault?sslmode=disable"),
		S3Endpoint:  envOr("S3_ENDPOINT", "http://localhost:9000"),
		HMACKey:     envOr("HMAC_KEY", "dev-only-change-me"),

		StorageBackend: envOr("STORAGE_BACKEND", "s3"),
		S3Region:       envOr("S3_REGION", "us-east-1"),
		S3Bucket:       envOr("S3_BUCKET", "vault"),
		S3AccessKey:    envOr("S3_ACCESS_KEY", "minio"),
		S3SecretKey:    envOr("S3_SECRET_KEY", "minio12345"),
		S3PathStyle:    envOr("S3_PATH_STYLE", "true") == "true",
		LocalDir:       envOr("LOCAL_STORAGE_DIR", "/tmp/vault-storage"),

		VerifyBaseURL: envOr("VERIFY_BASE_URL", "http://localhost:8080"),
		PanelOrigin:   envOr("PANEL_ORIGIN", ""),
	}
}

func envOr(k, d string) string {
	if v := os.Getenv(k); v != "" {
		return v
	}
	return d
}
