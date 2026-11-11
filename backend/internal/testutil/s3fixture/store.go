package s3fixture

import (
	"net/url"
	"os"
	"strconv"
	"strings"
	"testing"

	"vault/internal/storage/s3store"
)

func Config(t *testing.T) s3store.Config {
	t.Helper()
	endpoint := os.Getenv("S3_TEST_ENDPOINT")
	if endpoint == "" {
		t.Skip("S3_TEST_ENDPOINT unset: private object integration unverified")
	}
	u, err := url.Parse(endpoint)
	if err != nil || u.Scheme != "http" || u.Hostname() != "127.0.0.1" || u.User != nil || u.Path != "" || u.RawQuery != "" || u.Fragment != "" {
		t.Fatal("object integration requires an explicit loopback fixture endpoint")
	}
	port, err := strconv.Atoi(u.Port())
	if err != nil || port < 1 || port > 65535 {
		t.Fatal("invalid object fixture port")
	}
	cfg := s3store.Config{Endpoint: endpoint, Region: os.Getenv("S3_TEST_REGION"), Bucket: os.Getenv("S3_TEST_BUCKET"),
		AccessKey: os.Getenv("S3_TEST_ACCESS_KEY"), SecretKey: os.Getenv("S3_TEST_SECRET_KEY"), UsePathStyle: true}
	if cfg.Region == "" || !strings.HasPrefix(cfg.Bucket, "fixture-") || cfg.AccessKey == "" || cfg.SecretKey == "" {
		t.Fatal("explicit synthetic S3 fixture configuration is incomplete")
	}
	return cfg
}

func Store(t *testing.T) *s3store.Store {
	t.Helper()
	return s3store.New(Config(t))
}
