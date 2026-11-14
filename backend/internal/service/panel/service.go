package panel

import (
	"context"
	"crypto/rand"
	"crypto/sha256"
	"encoding/base64"
	"errors"
	"fmt"
	"net/mail"
	"strings"
	"unicode/utf8"

	"vault/internal/store/postgres"
)

var ErrInput = errors.New("invalid panel input")

type Service struct{ store *postgres.PanelStore }

func New(store *postgres.PanelStore) *Service { return &Service{store: store} }

func token() string {
	var bytes [32]byte
	rand.Read(bytes[:])
	return base64.RawURLEncoding.EncodeToString(bytes[:])
}

func tokenHash(value string) ([]byte, error) {
	bytes, err := base64.RawURLEncoding.Strict().DecodeString(value)
	if err != nil || len(bytes) != 32 {
		return nil, ErrInput
	}
	digest := sha256.Sum256(bytes)
	return digest[:], nil
}

func (s *Service) Session(ctx context.Context, session string) (postgres.PanelActor, error) {
	hash, err := tokenHash(session)
	if err != nil {
		return postgres.PanelActor{}, postgres.ErrNotFound
	}
	return s.store.Session(ctx, hash)
}

func (s *Service) Logout(ctx context.Context, session string) error {
	hash, err := tokenHash(session)
	if err != nil {
		return nil
	}
	return s.store.Logout(ctx, hash)
}

func cleanEmail(value string) (string, error) {
	value = strings.TrimSpace(value)
	address, err := mail.ParseAddress(value)
	if err != nil || len(value) > 254 || address.Address != value {
		return "", fmt.Errorf("email: %w", ErrInput)
	}
	return value, nil
}

func cleanText(value string, min, max int) (string, error) {
	value = strings.TrimSpace(value)
	size := utf8.RuneCountInString(value)
	if size < min || size > max || strings.ContainsRune(value, 0) {
		return "", ErrInput
	}
	return value, nil
}

func withAccessLink(issue func([]byte) error) (string, error) {
	code := token()
	hash, _ := tokenHash(code)
	if err := issue(hash); err != nil {
		return "", err
	}
	return code, nil
}

func (s *Service) Bootstrap(ctx context.Context, email, name string) (string, error) {
	email, err := cleanEmail(email)
	if err != nil {
		return "", err
	}
	name, err = cleanText(name, 2, 100)
	if err != nil {
		return "", err
	}
	return withAccessLink(func(hash []byte) error {
		return s.store.Bootstrap(ctx, email, name, hash)
	})
}

func (s *Service) Invite(ctx context.Context, actor, email, name, role string) (string, error) {
	email, err := cleanEmail(email)
	if err != nil {
		return "", err
	}
	name, err = cleanText(name, 2, 100)
	if err != nil {
		return "", err
	}
	if !validRole(role) {
		return "", ErrInput
	}
	return withAccessLink(func(hash []byte) error {
		return s.store.Invite(ctx, actor, email, name, role, hash)
	})
}

func (s *Service) Reissue(ctx context.Context, actor, user string) (string, error) {
	return withAccessLink(func(hash []byte) error {
		return s.store.Reissue(ctx, actor, user, hash)
	})
}

func validRole(role string) bool {
	switch role {
	case "owner", "admin", "support", "user":
		return true
	default:
		return false
	}
}
