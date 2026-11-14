package account

import (
	"context"

	"vault/internal/accountproof"
	"vault/internal/credential"
	"vault/internal/store/postgres"
)

type Verification struct{ store *postgres.AccountStore }

func NewVerification(store *postgres.AccountStore) *Verification { return &Verification{store} }

// Verify establishes ownership and a credential together. It never signs in.
func (s *Verification) Verify(ctx context.Context, token, password string) error {
	if !credential.Valid(password) {
		return credential.ErrPolicy
	}
	digest, err := accountproof.Digest(token)
	if err != nil {
		return postgres.ErrNotFound
	}
	encoded, err := credential.Hash(password)
	if err != nil {
		return unavailable{err}
	}
	if err := s.store.VerifyAccount(ctx, digest, encoded); err != nil {
		if err == postgres.ErrNotFound {
			return err
		}
		return unavailable{err}
	}
	return nil
}
