package hasher

import (
	"crypto/sha256"
	"encoding/hex"
	"io"
)

// StreamSHA256 hashes r without loading it fully in memory.
// Used on upload path; never trust client-provided hashes.
func StreamSHA256(r io.Reader) (string, int64, error) {
	h := sha256.New()
	n, err := io.Copy(h, r)
	if err != nil {
		return "", 0, err
	}
	return hex.EncodeToString(h.Sum(nil)), n, nil
}
