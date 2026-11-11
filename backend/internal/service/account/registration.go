package account

import (
	"context"
	"errors"
	"fmt"
	"io"
	"time"

	"vault/internal/accountproof"
	"vault/internal/store/postgres"

	"github.com/google/uuid"
)

var ErrInput = errors.New("invalid account registration input")

type unavailable struct{ cause error }

func (e unavailable) Error() string                  { return "account service unavailable" }
func (e unavailable) Unwrap() error                  { return e.cause }
func (e unavailable) Format(state fmt.State, _ rune) { _, _ = io.WriteString(state, e.Error()) }

type Registration struct {
	store *postgres.AccountStore
	key   *accountproof.Key
}

func NewRegistration(store *postgres.AccountStore, key *accountproof.Key) (*Registration, error) {
	if store == nil || key == nil {
		return nil, ErrInput
	}
	return &Registration{store, key}, nil
}

func (s *Registration) Register(ctx context.Context, email, source string) error {
	email, source, err := requestAddress(email, source)
	if err != nil {
		return err
	}
	defer responseFloor(ctx, time.Now())
	err = s.store.Register(ctx, email, s.key.Throttle("source", source), s.key.Throttle("address", email), func(user string) postgres.AccountChallenge {
		operation := uuid.NewString()
		digest, ciphertext := s.key.Issue(user, operation)
		return postgres.AccountChallenge{Operation: operation, Digest: digest, Ciphertext: ciphertext}
	})
	if err != nil {
		return unavailable{err}
	}
	return nil
}
