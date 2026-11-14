package localstore

import (
	"context"
	"io"
	"os"
	"path/filepath"
	"strings"
	"testing"
)

// Roundtrip on a temp root: exact bytes, URL marker, delete, missing ok.
func TestLocalRoundtrip(t *testing.T) {
	s := New(t.TempDir())
	ctx := context.Background()
	if err := s.PutOriginal(ctx, "originals/i1/e1", strings.NewReader("bytes-1"), "image/jpeg"); err != nil {
		t.Fatalf("put: %v", err)
	}
	reader, err := s.OpenOriginal(ctx, "originals/i1/e1")
	if err != nil {
		t.Fatalf("open: %v", err)
	}
	got, err := io.ReadAll(reader)
	_ = reader.Close()
	if err != nil || string(got) != "bytes-1" {
		t.Fatalf("stored bytes mismatch: %q %v", got, err)
	}
	url, err := s.SignedGetURL(ctx, "originals/i1/e1", 0)
	if err != nil || !strings.HasSuffix(url, filepath.Join("originals", "i1", "e1")) {
		t.Fatalf("bad url marker: %q %v", url, err)
	}
	if err := s.Delete(ctx, "originals/i1/e1"); err != nil {
		t.Fatalf("delete: %v", err)
	}
	if err := s.Delete(ctx, "originals/i1/e1"); err != nil {
		t.Fatalf("missing delete must succeed: %v", err)
	}
	if _, err := s.OpenOriginal(ctx, "originals/i1/e1"); err == nil {
		t.Fatal("open accepted a missing original")
	}
}

// Escape attempts are rejected and never touch the parent directory.
func TestLocalRejectsTraversal(t *testing.T) {
	root := t.TempDir()
	s := New(root)
	ctx := context.Background()
	for _, key := range []string{"", "../escape", "a/../../escape", "/abs", ".."} {
		if err := s.PutOriginal(ctx, key, strings.NewReader("x"), "image/jpeg"); err == nil {
			t.Fatalf("put %q must fail", key)
		}
		if _, err := s.SignedGetURL(ctx, key, 0); err == nil {
			t.Fatalf("url %q must fail", key)
		}
		if _, err := s.OpenOriginal(ctx, key); err == nil {
			t.Fatalf("open %q must fail", key)
		}
		if err := s.Delete(ctx, key); err == nil {
			t.Fatalf("delete %q must fail", key)
		}
	}
	entries, err := os.ReadDir(root)
	if err != nil || len(entries) != 0 {
		t.Fatalf("root must stay empty: %v %v", entries, err)
	}
}
