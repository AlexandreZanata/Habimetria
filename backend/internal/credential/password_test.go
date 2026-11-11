package credential

import (
	"bytes"
	"crypto/pbkdf2"
	"crypto/sha256"
	"errors"
	"strings"
	"testing"
)

func TestCredentialCompatibility(t *testing.T) {
	password := "legacy fixture password"
	legacy := append([]byte{1}, bytes.Repeat([]byte{5}, 16)...)
	key, err := pbkdf2.Key(sha256.New, password, legacy[1:], 600_000, 32)
	if err != nil {
		t.Fatal(err)
	}
	legacy = append(legacy, key...)
	if !Match(password, legacy) || Match(password+"x", legacy) {
		t.Fatal("legacy encoding changed")
	}
	a, err := Hash(password)
	if err != nil {
		t.Fatal(err)
	}
	b, err := Hash(password)
	if err != nil || bytes.Equal(a, b) || !Match(password, a) || !Match(password, b) {
		t.Fatal("salt or verification failure")
	}
	for _, malformed := range [][]byte{nil, {1}, bytes.Repeat([]byte{2}, 49), append(a, 0)} {
		if Match(password, malformed) {
			t.Fatal("malformed credential accepted")
		}
	}
}

func TestPasswordPolicy(t *testing.T) {
	for _, password := range []string{"short", strings.Repeat("a", 1025), string([]byte{255}) + strings.Repeat("a", 15)} {
		if _, err := Hash(password); !errors.Is(err, ErrPolicy) || Match(password, nil) {
			t.Fatal("invalid password accepted")
		}
	}
	for _, password := range []string{strings.Repeat("é", 15), strings.Repeat("a", 1024)} {
		if !Valid(password) {
			t.Fatal("valid Unicode/long password rejected")
		}
	}
}
