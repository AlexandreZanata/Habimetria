package activation

import (
	"errors"
	"io"
	"os"
	"strings"
)

const MaxCredentialBytes = 1 << 20

type credentialError struct {
	operation string
	cause     error
}

func (e *credentialError) Error() string                  { return "store credential " + e.operation + " failed" }
func (e *credentialError) Unwrap() []error                { return []error{ErrCredentialUnavailable, e.cause} }
func credentialFailure(operation string, err error) error { return &credentialError{operation, err} }

// Roots are pinned directory handles. A symbolic-link alias cannot switch a
// configured environment/store to another credential namespace.
func openCredential(directory, reference string) (credential io.ReadCloser, err error) {
	info, err := os.Lstat(directory)
	if err != nil {
		return nil, credentialFailure("inspect root", err)
	}
	if !info.IsDir() || info.Mode()&os.ModeSymlink != 0 {
		return nil, credentialFailure("root type", os.ErrPermission)
	}
	root, err := os.OpenRoot(directory)
	if err != nil {
		return nil, credentialFailure("open root", err)
	}
	defer func() {
		if closeErr := root.Close(); closeErr != nil {
			if credential != nil {
				closeErr = errors.Join(closeErr, credential.Close())
				credential = nil
			}
			err = errors.Join(err, credentialFailure("close root", closeErr))
		}
	}()
	actual, err := root.Stat(".")
	if err != nil {
		return nil, credentialFailure("stat root", err)
	}
	if !os.SameFile(info, actual) {
		return nil, credentialFailure("root identity", os.ErrPermission)
	}
	parts := strings.Split(reference, "/") // Already validated as exactly namespace/store/filename.
	for _, part := range parts[:2] {
		child, openErr := credentialDirectory(root, part)
		if openErr != nil {
			return nil, openErr
		}
		parent := root
		root = child
		if closeErr := parent.Close(); closeErr != nil {
			return nil, credentialFailure("close parent", closeErr)
		}
	}
	info, err = root.Lstat(parts[2])
	if err != nil {
		return nil, credentialFailure("inspect file", err)
	}
	if !privateCredential(info) {
		return nil, credentialFailure("file policy", os.ErrPermission)
	}
	file, err := root.Open(parts[2])
	if err != nil {
		return nil, credentialFailure("open file", err)
	}
	actual, err = file.Stat()
	if err != nil || !os.SameFile(info, actual) || !privateCredential(actual) {
		return nil, credentialFailure("opened file policy", errors.Join(err, os.ErrPermission, file.Close()))
	}
	return &credentialReader{reader: io.NewSectionReader(file, 0, actual.Size()), file: file}, nil
}

func credentialDirectory(parent *os.Root, name string) (*os.Root, error) {
	info, err := parent.Lstat(name)
	if err != nil {
		return nil, credentialFailure("inspect namespace", err)
	}
	if !info.IsDir() || info.Mode()&os.ModeSymlink != 0 {
		return nil, credentialFailure("namespace type", os.ErrPermission)
	}
	child, err := parent.OpenRoot(name)
	if err != nil {
		return nil, credentialFailure("open namespace", err)
	}
	actual, err := child.Stat(".")
	if err != nil || !os.SameFile(info, actual) {
		return nil, credentialFailure("namespace identity", errors.Join(err, os.ErrPermission, child.Close()))
	}
	return child, nil
}

func privateCredential(info os.FileInfo) bool {
	return info.Mode().IsRegular() && (info.Mode().Perm() == 0400 || info.Mode().Perm() == 0600) && info.Size() > 0 && info.Size() <= MaxCredentialBytes
}

type credentialReader struct {
	reader *io.SectionReader
	file   *os.File
}

func (r *credentialReader) Read(bytes []byte) (int, error) {
	n, err := r.reader.Read(bytes)
	if err != nil && err != io.EOF {
		return n, credentialFailure("read", err)
	}
	return n, err
}
func (r *credentialReader) Close() error {
	if err := r.file.Close(); err != nil {
		return credentialFailure("close file", err)
	}
	return nil
}
func (*credentialReader) String() string   { return "[REDACTED store credential]" }
func (*credentialReader) GoString() string { return "[REDACTED store credential]" }
func (*credentialReader) MarshalJSON() ([]byte, error) {
	return []byte(`"[REDACTED store credential]"`), nil
}
