package account

import (
	"context"
	"crypto/rand"
	"encoding/base64"

	"vault/internal/accountproof"
	"vault/internal/service/panel"
	"vault/internal/store/postgres"
)

const AccessSeconds = 15 * 60

type Authentication struct {
	store       *postgres.AccountStore
	credentials *panel.Service
}

func NewAuthentication(store *postgres.AccountStore, credentials *panel.Service) *Authentication {
	return &Authentication{store, credentials}
}

func (s *Authentication) Login(ctx context.Context, email, password string) (string, error) {
	user, encoded, err := s.credentials.Authenticate(ctx, email, password)
	if err != nil {
		return "", err
	}
	var raw [32]byte
	rand.Read(raw[:])
	token := base64.RawURLEncoding.EncodeToString(raw[:])
	digest, _ := accountproof.Digest(token)
	if err := s.store.StartAccess(ctx, user, encoded, digest); err != nil {
		return "", err
	}
	return token, nil
}

func (s *Authentication) Session(ctx context.Context, token string) (postgres.PanelActor, error) {
	digest, err := accountproof.Digest(token)
	if err != nil {
		return postgres.PanelActor{}, postgres.ErrNotFound
	}
	return s.store.AccessActor(ctx, digest)
}

func (s *Authentication) Logout(ctx context.Context, token string) error {
	digest, err := accountproof.Digest(token)
	if err != nil {
		return nil
	}
	return s.store.EndAccess(ctx, digest)
}
