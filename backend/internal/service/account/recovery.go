package account

import (
	"context"
	"errors"
	"time"

	"vault/internal/accountproof"
	"vault/internal/credential"
	"vault/internal/store/postgres"

	"github.com/google/uuid"
)

var ErrConfirmation = errors.New("password confirmation does not match")

type RecoveryRequest struct {
	store *postgres.AccountStore
	key   *accountproof.Key
}
type PasswordReset struct{ store *postgres.AccountStore }

func NewRecoveryRequest(store *postgres.AccountStore, key *accountproof.Key) (*RecoveryRequest, error) {
	if store == nil || key == nil {
		return nil, ErrInput
	}
	return &RecoveryRequest{store, key}, nil
}
func NewPasswordReset(store *postgres.AccountStore) *PasswordReset { return &PasswordReset{store} }

func (s *RecoveryRequest) Request(ctx context.Context, email, source string) error {
	email, source, err := requestAddress(email, source)
	if err != nil {
		return err
	}
	defer responseFloor(ctx, time.Now())
	err = s.store.RequestRecovery(ctx, email, s.key.Throttle("recovery-source", source), s.key.Throttle("recovery-address", email), func(user string) postgres.AccountChallenge {
		operation := uuid.NewString()
		digest, ciphertext := s.key.IssueRecovery(user, operation)
		return postgres.AccountChallenge{Operation: operation, Digest: digest, Ciphertext: ciphertext}
	})
	if err != nil {
		return unavailable{err}
	}
	return nil
}

func (s *PasswordReset) Reset(ctx context.Context, token, password, confirmation string) error {
	if !credential.Valid(password) {
		return credential.ErrPolicy
	}
	if password != confirmation {
		return ErrConfirmation
	}
	digest, err := accountproof.Digest(token)
	if err != nil {
		return postgres.ErrNotFound
	}
	encoded, err := credential.Hash(password)
	if err != nil {
		return unavailable{err}
	}
	err = s.store.ResetPassword(ctx, digest, encoded)
	if err != nil && !errors.Is(err, postgres.ErrNotFound) {
		return unavailable{err}
	}
	return err
}
