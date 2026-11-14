package accountdelivery

import (
	"errors"
	"fmt"
	"io"
	"os"
	"path/filepath"
)

type failure struct {
	operation string
	cause     error
}

func (e *failure) Error() string                  { return "account delivery " + e.operation + " unavailable" }
func (e *failure) Unwrap() error                  { return e.cause }
func (e *failure) Format(state fmt.State, _ rune) { _, _ = io.WriteString(state, e.Error()) }
func Unavailable(operation string, cause error) error {
	switch operation {
	case "configuration", "proof key", "SMTP password", "database", "status", "output":
	default:
		operation = "operation"
	}
	return &failure{operation, cause}
}

func readSecret(path string, maximum int) (data []byte, err error) {
	defer func() {
		if err != nil {
			err = Unavailable("configuration", err)
			data = nil
		}
	}()
	if !filepath.IsAbs(path) || filepath.Clean(path) != path || maximum < 1 || maximum > 4096 {
		return nil, os.ErrPermission
	}
	directory, name := filepath.Dir(path), filepath.Base(path)
	info, err := os.Lstat(directory)
	if err != nil {
		return nil, err
	}
	if !info.IsDir() || info.Mode()&os.ModeSymlink != 0 || info.Mode().Perm()&0022 != 0 {
		return nil, os.ErrPermission
	}
	root, err := os.OpenRoot(directory)
	if err != nil {
		return nil, err
	}
	defer func() {
		if closeErr := root.Close(); closeErr != nil {
			err = errors.Join(err, closeErr)
		}
	}()
	actual, err := root.Stat(".")
	if err != nil {
		return nil, err
	}
	if !os.SameFile(info, actual) {
		return nil, os.ErrPermission
	}
	info, err = root.Lstat(name)
	if err != nil {
		return nil, err
	}
	private := func(info os.FileInfo) bool {
		return info.Mode().IsRegular() && (info.Mode().Perm() == 0400 || info.Mode().Perm() == 0600) && info.Size() > 0 && info.Size() <= int64(maximum)
	}
	if !private(info) {
		return nil, os.ErrPermission
	}
	file, err := root.Open(name)
	if err != nil {
		return nil, err
	}
	defer func() {
		if closeErr := file.Close(); closeErr != nil {
			err = errors.Join(err, closeErr)
		}
	}()
	actual, err = file.Stat()
	if err != nil {
		return nil, err
	}
	if !private(actual) || !os.SameFile(info, actual) {
		return nil, os.ErrPermission
	}
	data, err = io.ReadAll(io.LimitReader(file, int64(maximum)+1))
	if err != nil {
		return nil, err
	}
	if len(data) == 0 || len(data) > maximum {
		return nil, os.ErrPermission
	}
	return data, nil
}
