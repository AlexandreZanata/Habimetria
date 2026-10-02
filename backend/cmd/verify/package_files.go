package main

import (
	"io"
	"os"
	"path/filepath"
	"strings"
)

// Bounded reads confined to the user-selected package; symlinked escapes
// and malformed/oversized files fail closed without loading arbitrary data.
func readFile(dir, name string) ([]byte, bool) {
	root, err := filepath.EvalSymlinks(dir)
	if err != nil {
		return nil, false
	}
	root, err = filepath.Abs(root)
	if err != nil {
		return nil, false
	}
	resolved, err := filepath.EvalSymlinks(filepath.Join(root, name))
	if err != nil || !strings.HasPrefix(resolved, root+string(filepath.Separator)) {
		return nil, false
	}
	file, err := os.Open(resolved)
	if err != nil {
		return nil, false
	}
	defer file.Close()
	info, err := file.Stat()
	limit := int64(50 * 1024 * 1024)
	if strings.HasSuffix(name, ".json") {
		limit = 8 * 1024 * 1024
	}
	if err != nil || !info.Mode().IsRegular() || info.Size() <= 0 || info.Size() > limit {
		return nil, false
	}
	data, err := io.ReadAll(io.LimitReader(file, limit+1))
	return data, err == nil && int64(len(data)) <= limit
}
