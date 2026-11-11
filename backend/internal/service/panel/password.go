package panel

import (
	"context"
	"crypto/sha256"
	"errors"
	"strings"

	"vault/internal/credential"
	"vault/internal/store/postgres"
)

var ErrPassword = credential.ErrPolicy

func (s *Service) Activate(ctx context.Context, access, password string) (string, error) {
	link, err := tokenHash(strings.TrimSpace(access))
	if err != nil {
		return "", postgres.ErrNotFound
	}
	encoded, err := credential.Hash(password)
	if err != nil {
		return "", err
	}
	return withAccessLink(func(session []byte) error {
		return s.store.Activate(ctx, link, encoded, session)
	})
}

func (s *Service) Login(ctx context.Context, email, password string) (string, error) {
	user, encoded, err := s.Authenticate(ctx, email, password)
	if err != nil {
		return "", err
	}
	return withAccessLink(func(session []byte) error {
		return s.store.StartSession(ctx, user, encoded, session)
	})
}

// Authenticate shares credential verification without issuing a browser session.
// The caller must atomically recheck the credential and its transport's authority.
func (s *Service) Authenticate(ctx context.Context, email, password string) (string, []byte, error) {
	email, err := cleanEmail(email)
	if err != nil || !credential.Valid(password) {
		return "", nil, postgres.ErrNotFound
	}
	user, encoded, err := s.store.Credential(ctx, email)
	if err != nil && !errors.Is(err, postgres.ErrNotFound) {
		return "", nil, err
	}
	if !credential.Match(password, encoded) || err != nil {
		return "", nil, postgres.ErrNotFound
	}
	return user, encoded, nil
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
