package s3store_test

import (
	"context"
	"io"
	"net/http"
	"net/url"
	"strings"
	"testing"
	"time"

	"vault/internal/storage/s3store"
	"vault/internal/testutil/s3fixture"

	"github.com/google/uuid"
)

func TestPrivateS3RoundtripIntegration(t *testing.T) {
	cfg := s3fixture.Config(t)
	store := s3store.New(cfg)
	ctx, cancel := context.WithTimeout(context.Background(), 30*time.Second)
	defer cancel()
	for range 2 {
		if err := store.EnsureBucket(ctx); err != nil {
			t.Fatalf("ensure private bucket: %v", err)
		}
	}
	key := "originals/fixture/" + uuid.NewString()
	t.Cleanup(func() {
		cleanup, cancel := context.WithTimeout(context.Background(), 10*time.Second)
		defer cancel()
		if err := store.Delete(cleanup, key); err != nil {
			t.Errorf("clean synthetic object: %v", err)
		}
	})
	const bytes = "Synthetic object bytes; no customer photograph."
	if err := store.PutOriginal(ctx, key, strings.NewReader(bytes), "text/plain"); err != nil {
		t.Fatalf("put private object: %v", err)
	}
	reader, err := store.OpenOriginal(ctx, key)
	if err != nil {
		t.Fatal(err)
	}
	actual, err := io.ReadAll(io.LimitReader(reader, 1024))
	closeErr := reader.Close()
	if err != nil || closeErr != nil || string(actual) != bytes {
		t.Fatalf("stored bytes differ: %v/%v", err, closeErr)
	}
	signed, err := store.SignedGetURL(ctx, key, time.Minute)
	if err != nil {
		t.Fatal(err)
	}
	client := &http.Client{Timeout: 5 * time.Second, Transport: &http.Transport{Proxy: nil}}
	t.Cleanup(client.CloseIdleConnections)
	check := func(address string, want int, content string) {
		t.Helper()
		response, err := client.Get(address)
		if err != nil {
			t.Fatal("object fixture request failed")
		}
		defer response.Body.Close()
		body, err := io.ReadAll(io.LimitReader(response.Body, 4096))
		if err != nil || response.StatusCode != want || (content != "" && string(body) != content) {
			t.Fatalf("private object access status %d, expected %d; byte match %v", response.StatusCode, want, string(body) == content)
		}
	}
	check(signed, 200, bytes)
	check(cfg.Endpoint+"/"+cfg.Bucket+"/"+key, 403, "")
	tampered, err := url.Parse(signed)
	if err != nil {
		t.Fatal("invalid presigned fixture URL")
	}
	query := tampered.Query()
	query.Set("X-Amz-Signature", strings.Repeat("0", 64))
	tampered.RawQuery = query.Encode()
	check(tampered.String(), 403, "")
	if err := store.Delete(ctx, key); err != nil {
		t.Fatal(err)
	}
	check(signed, 404, "")
}
