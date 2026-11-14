// Package credential owns the versioned password policy shared by all transports.
package credential

import (
	"crypto/pbkdf2"
	"crypto/rand"
	"crypto/sha256"
	"crypto/subtle"
	"errors"
	"unicode/utf8"
)

const length = 49 // version + 16-byte salt + 32-byte PBKDF2 digest

var ErrPolicy = errors.New("password must contain at least 15 characters")

func Valid(password string) bool {
	return len(password) <= 1024 && utf8.ValidString(password) && utf8.RuneCountInString(password) >= 15
}

func Hash(password string) ([]byte, error) {
	if !Valid(password) {
		return nil, ErrPolicy
	}
	encoded := make([]byte, length)
	encoded[0] = 1
	rand.Read(encoded[1:17])
	key, err := derive(password, encoded[1:17])
	if err != nil {
		return nil, err
	}
	copy(encoded[17:], key)
	return encoded, nil
}

// Match performs the same derivation for absent/invalid stored credentials.
func Match(password string, encoded []byte) bool {
	if !Valid(password) {
		return false
	}
	found := len(encoded) == length && encoded[0] == 1
	if !found {
		encoded = make([]byte, length)
	}
	key, err := derive(password, encoded[1:17])
	return err == nil && subtle.ConstantTimeCompare(key, encoded[17:]) == 1 && found
}

func derive(password string, salt []byte) ([]byte, error) {
	return pbkdf2.Key(sha256.New, password, salt, 600_000, 32)
}
