package s3store

import (
	"context"
	"net/http"
	"os"
	"strings"
	"testing"
	"time"
)

// Roundtrip against a real S3-compatible endpoint (MinIO in CI, S3Mock
// locally): idempotent bucket setup, put, presigned URL shape, delete.
// Skips unless S3_TEST_ENDPOINT answers, so an unconfigured suite never
// claims S3 integration.
func TestS3Roundtrip(t *testing.T) {
	endpoint := os.Getenv("S3_TEST_ENDPOINT")
	if endpoint == "" {
		endpoint = "http://localhost:9090"
	}
	client := &http.Client{Timeout: 3 * time.Second}
	resp, err := client.Get(endpoint + "/")
	if err != nil {
		t.Skipf("S3 endpoint %s unreachable: S3 integration unverified", endpoint)
	}
	resp.Body.Close()

	ctx, cancel := context.WithTimeout(context.Background(), 30*time.Second)
	defer cancel()

	s := New(Config{
		Endpoint: endpoint, Region: "us-east-1", Bucket: "vault-test",
		AccessKey: "minio", SecretKey: "minio12345", UsePathStyle: true,
	})
	if err := s.EnsureBucket(ctx); err != nil {
		t.Fatalf("ensure bucket: %v", err)
	}
	if err := s.EnsureBucket(ctx); err != nil {
		t.Fatalf("ensure bucket must be idempotent: %v", err)
	}
	key := "originals/i1/e1"
	if err := s.PutOriginal(ctx, key, strings.NewReader("bytes-1"), "image/jpeg"); err != nil {
		t.Fatalf("put: %v", err)
	}
	url, err := s.SignedGetURL(ctx, key, time.Hour)
	if err != nil || !strings.Contains(url, "vault-test") || !strings.Contains(url, key) {
		t.Fatalf("bad presigned url: %q %v", url, err)
	}
	if err := s.Delete(ctx, key); err != nil {
		t.Fatalf("delete: %v", err)
	}
}
