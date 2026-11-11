package account

import (
	"context"
	"errors"
	"fmt"
	"io"
	"net/mail"
	"net/netip"
	"strings"

	"vault/internal/accountproof"
	"vault/internal/store/postgres"

	"github.com/google/uuid"
)

var ErrInput = errors.New("invalid account registration input")

type unavailable struct{ cause error }

func (e unavailable) Error() string                  { return "account registration unavailable" }
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
	email = strings.ToLower(strings.TrimSpace(email))
	a, err := mail.ParseAddress(email)
	ip, ipErr := netip.ParseAddr(source)
	if err != nil || a.Address != email || len(email) > 254 || ipErr != nil || strings.ContainsAny(email, "\r\n") {
		return ErrInput
	}
	for _, r := range email {
		if r > 127 {
			return ErrInput
		}
	}
	err = s.store.Register(ctx, email, s.key.Throttle("source", ip.Unmap().String()), s.key.Throttle("address", email), func(user string) postgres.AccountChallenge {
		operation := uuid.NewString()
		digest, ciphertext := s.key.Issue(user, operation)
		return postgres.AccountChallenge{Operation: operation, Digest: digest, Ciphertext: ciphertext}
	})
	if err != nil {
		return unavailable{err}
	}
	return nil
}
