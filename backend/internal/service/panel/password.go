package panel

import (
	"context"
	"crypto/pbkdf2"
	"crypto/rand"
	"crypto/sha256"
	"crypto/subtle"
	"errors"
	"strings"
	"unicode/utf8"

	"vault/internal/store/postgres"
)

const passwordIterations = 600_000
const credentialLength = 1 + 16 + 32

var ErrPassword = errors.New("password must contain at least 15 characters")

func (s *Service) Activate(ctx context.Context, access, password string) (string, error) {
	link, err := tokenHash(strings.TrimSpace(access))
	if err != nil {
		return "", postgres.ErrNotFound
	}
	if !validPassword(password) {
		return "", ErrPassword
	}
	credential := make([]byte, credentialLength)
	credential[0] = 1
	rand.Read(credential[1:17])
	key, err := derive(password, credential[1:17])
	if err != nil {
		return "", err
	}
	copy(credential[17:], key)
	return withAccessLink(func(session []byte) error {
		return s.store.Activate(ctx, link, credential, session)
	})
}

func (s *Service) Login(ctx context.Context, email, password string) (string, error) {
	email, err := cleanEmail(email)
	if err != nil || !validPassword(password) {
		return "", postgres.ErrNotFound
	}
	user, credential, err := s.store.Credential(ctx, email)
	if err != nil && !errors.Is(err, postgres.ErrNotFound) {
		return "", err
	}
	found := err == nil && len(credential) == credentialLength && credential[0] == 1
	if !found {
		credential = make([]byte, credentialLength)
	}
	key, err := derive(password, credential[1:17])
	if err != nil {
		return "", err
	}
	if subtle.ConstantTimeCompare(key, credential[17:]) != 1 || !found {
		return "", postgres.ErrNotFound
	}
	return withAccessLink(func(session []byte) error {
		return s.store.StartSession(ctx, user, credential, session)
	})
}

func (s *Service) AllowAuthentication(ctx context.Context, ip, email string) error {
	ipHash := sha256.Sum256([]byte("ip:" + ip))
	if err := s.store.AllowLogin(ctx, ipHash[:], 150); err != nil {
		return err
	}
	email, err := cleanEmail(email)
	if err != nil {
		return nil
	}
	emailHash := sha256.Sum256([]byte("email:" + strings.ToLower(email)))
	return s.store.AllowLogin(ctx, emailHash[:], 10)
}

func validPassword(password string) bool {
	return len(password) <= 1024 && utf8.ValidString(password) && utf8.RuneCountInString(password) >= 15
}

func derive(password string, salt []byte) ([]byte, error) {
	return pbkdf2.Key(sha256.New, password, salt, passwordIterations, 32)
}
