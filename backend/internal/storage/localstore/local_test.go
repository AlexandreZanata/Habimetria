package localstore

import (
	"context"
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
	got, err := os.ReadFile(filepath.Join(s.dir, "originals", "i1", "e1"))
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
		if err := s.Delete(ctx, key); err == nil {
			t.Fatalf("delete %q must fail", key)
		}
	}
	entries, err := os.ReadDir(root)
	if err != nil || len(entries) != 0 {
		t.Fatalf("root must stay empty: %v %v", entries, err)
	}
}
