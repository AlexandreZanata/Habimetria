package localstore

import (
	"context"
	"errors"
	"fmt"
	"io"
	"os"
	"path/filepath"
	"strings"
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

// resolveKey confines every key inside the store root. Absolute paths and
// any `..` escape are rejected instead of being written outside the root.
func (s *Store) resolveKey(key string) (string, error) {
	if key == "" || filepath.IsAbs(key) {
		return "", fmt.Errorf("key %q: %w", key, errEscape)
	}
	clean := filepath.Clean(filepath.FromSlash(key))
	if clean == "." || clean == ".." || strings.HasPrefix(clean, ".."+string(filepath.Separator)) {
		return "", fmt.Errorf("key %q: %w", key, errEscape)
	}
	return filepath.Join(s.dir, clean), nil
}

var errEscape = errors.New("storage key escapes store root")

// PutOriginal writes bytes atomically (tmp + rename).
func (s *Store) PutOriginal(_ context.Context, key string, r io.Reader, _ string) error {
	dst, err := s.resolveKey(key)
	if err != nil {
		return err
	}
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
	dst, err := s.resolveKey(key)
	if err != nil {
		return "", err
	}
	return "file://" + dst, nil
}

// Delete removes a stored object.
func (s *Store) Delete(_ context.Context, key string) error {
	dst, err := s.resolveKey(key)
	if err != nil {
		return err
	}
	if err := os.Remove(dst); err != nil && !os.IsNotExist(err) {
		return fmt.Errorf("remove %s: %w", key, err)
	}
	return nil
}
