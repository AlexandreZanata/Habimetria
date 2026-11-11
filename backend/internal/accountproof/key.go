package accountproof

import (
	"crypto/aes"
	"crypto/cipher"
	"crypto/hkdf"
	"crypto/hmac"
	"crypto/rand"
	"crypto/sha256"
	"encoding/base64"
	"errors"
	"fmt"
	"io"
)

var ErrProof = errors.New("invalid account proof")

type Key struct {
	aead  cipher.AEAD
	limit []byte
}

func New(master []byte) (*Key, error) {
	if len(master) != 32 {
		return nil, ErrProof
	}
	encryption, err := hkdf.Key(sha256.New, master, nil, "habimetria/account/mail/v1", 32)
	if err != nil {
		return nil, ErrProof
	}
	block, err := aes.NewCipher(encryption)
	if err != nil {
		return nil, ErrProof
	}
	aead, err := cipher.NewGCM(block)
	if err != nil {
		return nil, ErrProof
	}
	limit, err := hkdf.Key(sha256.New, master, nil, "habimetria/account/throttle/v1", 32)
	if err != nil {
		return nil, ErrProof
	}
	return &Key{aead, limit}, nil
}

func (k *Key) Format(state fmt.State, _ rune) { _, _ = io.WriteString(state, "[account key redacted]") }
func (k *Key) MarshalJSON() ([]byte, error)   { return []byte(`"[account key redacted]"`), nil }

func binding(user, operation string) []byte {
	return []byte(fmt.Sprintf("email_verification:%d:%s:%s", len(user), user, operation))
}

func (k *Key) Issue(user, operation string) (digest, ciphertext []byte) {
	var raw [32]byte
	rand.Read(raw[:])
	hash := sha256.Sum256(raw[:])
	nonce := make([]byte, k.aead.NonceSize())
	rand.Read(nonce)
	envelope := append([]byte{1}, nonce...)
	return hash[:], k.aead.Seal(envelope, nonce, raw[:], binding(user, operation))
}

func (k *Key) Open(user, operation string, envelope []byte) (string, error) {
	if len(envelope) != 61 || envelope[0] != 1 {
		return "", ErrProof
	}
	raw, err := k.aead.Open(nil, envelope[1:13], envelope[13:], binding(user, operation))
	if err != nil || len(raw) != 32 {
		return "", ErrProof
	}
	return base64.RawURLEncoding.EncodeToString(raw), nil
}

func Digest(token string) ([]byte, error) {
	raw, err := base64.RawURLEncoding.Strict().DecodeString(token)
	if err != nil || len(raw) != 32 {
		return nil, ErrProof
	}
	digest := sha256.Sum256(raw)
	return digest[:], nil
}

func (k *Key) Throttle(domain, value string) []byte {
	mac := hmac.New(sha256.New, k.limit)
	mac.Write([]byte(domain + ":" + value))
	return mac.Sum(nil)
}
