package s3store

import (
	"context"
	"net/http"
	"net/http/httptest"
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

// Mock endpoint keeps the S3 adapter's unit coverage independent from a
// live MinIO/S3 service, so the coverage ratchet stays honest in CI.
func TestStoreLifecycleAgainstMockEndpoint(t *testing.T) {
	var bucketCreated bool
	var calls []string
	srv := httptest.NewServer(http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
		calls = append(calls, r.Method+" "+r.URL.Path)
		switch {
		case r.Method == http.MethodHead && r.URL.Path == "/vault-test" && !bucketCreated:
			w.WriteHeader(http.StatusNotFound)
		case r.Method == http.MethodPut && r.URL.Path == "/vault-test":
			bucketCreated = true
			w.WriteHeader(http.StatusOK)
		case r.Method == http.MethodPut:
			w.WriteHeader(http.StatusOK)
		case r.Method == http.MethodDelete:
			w.WriteHeader(http.StatusNoContent)
		default:
			w.WriteHeader(http.StatusOK)
		}
	}))
	defer srv.Close()

	s := New(Config{
		Endpoint: srv.URL, Region: "us-east-1", Bucket: "vault-test",
		AccessKey: "minio", SecretKey: "minio12345", UsePathStyle: true,
	})
	ctx := context.Background()
	if err := s.EnsureBucket(ctx); err != nil {
		t.Fatalf("ensure bucket: %v", err)
	}
	if !bucketCreated {
		t.Fatal("expected bucket creation on missing HEAD")
	}
	if err := s.EnsureBucket(ctx); err != nil {
		t.Fatalf("ensure existing bucket: %v", err)
	}
	key := "originals/i1/e1"
	if err := s.PutOriginal(ctx, key, strings.NewReader("bytes-1"), "image/jpeg"); err != nil {
		t.Fatalf("put: %v", err)
	}
	url, err := s.SignedGetURL(ctx, key, time.Hour)
	if err != nil {
		t.Fatalf("presign: %v", err)
	}
	if !strings.Contains(url, "vault-test") || !strings.Contains(url, key) || !strings.Contains(url, "X-Amz-Signature") {
		t.Fatalf("bad presigned url: %q", url)
	}
	if err := s.Delete(ctx, key); err != nil {
		t.Fatalf("delete: %v", err)
	}
	want := []string{
		"HEAD /vault-test",
		"PUT /vault-test",
		"HEAD /vault-test",
		"PUT /vault-test/originals/i1/e1",
		"DELETE /vault-test/originals/i1/e1",
	}
	if strings.Join(calls, "\n") != strings.Join(want, "\n") {
		t.Fatalf("unexpected calls:\n%s", strings.Join(calls, "\n"))
	}
}
