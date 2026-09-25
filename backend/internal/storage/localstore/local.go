package localstore

import (
	"context"
	"fmt"
	"io"
	"os"
	"path/filepath"
	"time"
)

// Store implements storage.Port on local disk.
// Dev/test only — production uses S3-compatible storage.
type Store struct {
	dir string
}

// New builds a disk store rooted at dir.
func New(dir string) *Store {
	return &Store{dir: dir}
}

func (s *Store) path(key string) string {
	return filepath.Join(s.dir, filepath.FromSlash(key))
}

// PutOriginal writes bytes atomically (tmp + rename).
func (s *Store) PutOriginal(_ context.Context, key string, r io.Reader, _ string) error {
	dst := s.path(key)
	if err := os.MkdirAll(filepath.Dir(dst), 0o755); err != nil {
		return fmt.Errorf("mkdir: %w", err)
	}
	tmp, err := os.CreateTemp(filepath.Dir(dst), ".upload-*")
	if err != nil {
		return fmt.Errorf("tmp: %w", err)
	}
	tmpName := tmp.Name()
	defer os.Remove(tmpName)
	if _, err := io.Copy(tmp, r); err != nil {
		tmp.Close()
		return fmt.Errorf("write: %w", err)
	}
	if err := tmp.Close(); err != nil {
		return fmt.Errorf("close: %w", err)
	}
	if err := os.Rename(tmpName, dst); err != nil {
		return fmt.Errorf("rename: %w", err)
	}
	return nil
}

// SignedGetURL returns a file path marker. Handlers never expose it raw;
// the API serves bytes after re-checking scope in production S3 mode.
func (s *Store) SignedGetURL(_ context.Context, key string, _ time.Duration) (string, error) {
	return "file://" + s.path(key), nil
}

// Delete removes a stored object.
func (s *Store) Delete(_ context.Context, key string) error {
	if err := os.Remove(s.path(key)); err != nil && !os.IsNotExist(err) {
		return fmt.Errorf("remove %s: %w", key, err)
	}
	return nil
}
