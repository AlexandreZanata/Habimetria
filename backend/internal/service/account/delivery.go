package account

import (
	"context"
	"crypto/subtle"
	"errors"

	"vault/internal/accountproof"
	"vault/internal/mail"
	"vault/internal/store/postgres"
)

type Sender interface {
	SendVerification(context.Context, string, string, string) error
}
type Delivery struct {
	store  *postgres.AccountStore
	key    *accountproof.Key
	sender Sender
}
type DeliveryResult struct{ Claimed, Sent, Retried, Failed, Lost int }

func NewDelivery(store *postgres.AccountStore, key *accountproof.Key, sender Sender) (*Delivery, error) {
	if store == nil || key == nil || sender == nil {
		return nil, ErrInput
	}
	return &Delivery{store, key, sender}, nil
}

func (s *Delivery) Drain(ctx context.Context, limit int) (DeliveryResult, error) {
	var result DeliveryResult
	if limit < 1 || limit > 100 {
		return result, ErrInput
	}
	for range limit {
		if err := ctx.Err(); err != nil {
			return result, err
		}
		lease, err := s.store.ClaimMail(ctx)
		if err != nil {
			return result, unavailable{err}
		}
		if lease == nil {
			break
		}
		result.Claimed++
		payload, err := s.store.MailPayload(ctx, *lease)
		if errors.Is(err, postgres.ErrAccountMailLease) {
			result.Lost++
			continue
		}
		if err != nil {
			return result, unavailable{err}
		}
		code, retry := "", false
		switch {
		case !payload.Current:
			code = "ineligible"
		case !payload.Unexpired:
			code = "expired"
		default:
			token, err := s.key.Open(payload.User, lease.ID, payload.Ciphertext)
			digest, digestErr := accountproof.Digest(token)
			if err != nil || digestErr != nil || subtle.ConstantTimeCompare(digest, payload.Digest) != 1 {
				code = "invalid_proof"
			} else if err := s.sender.SendVerification(ctx, payload.Recipient, token, lease.ID); err != nil {
				code, retry = mail.Failure(err)
			}
		}
		if err := ctx.Err(); err != nil {
			return result, err
		} // Leave the lease recoverable after interrupted transport.
		state, err := s.store.CompleteMail(ctx, *lease, code, retry)
		if errors.Is(err, postgres.ErrAccountMailLease) {
			result.Lost++
			continue
		}
		if err != nil {
			return result, unavailable{err}
		}
		switch state {
		case "sent":
			result.Sent++
		case "pending":
			result.Retried++
		case "failed":
			result.Failed++
		}
	}
	if err := s.store.PruneRegistrationProofs(ctx); err != nil {
		return result, unavailable{err}
	}
	return result, nil
}
