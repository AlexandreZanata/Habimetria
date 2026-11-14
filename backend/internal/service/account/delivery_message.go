package account

import (
	"context"
	"crypto/subtle"

	"vault/internal/accountproof"
	"vault/internal/mail"
	"vault/internal/store/postgres"
)

func (s *Delivery) send(ctx context.Context, p postgres.AccountMailPayload, operation string) (string, bool) {
	m := mail.AccountMessage{Template: p.Template, Recipient: p.Recipient, Operation: operation}
	if p.Template != "password_changed" {
		var err error
		switch p.Template {
		case "email_verification":
			m.Token, err = s.key.Open(p.User, operation, p.Ciphertext)
		case "password_recovery":
			m.Token, err = s.key.OpenRecovery(p.User, operation, p.Ciphertext)
		default:
			return "invalid_proof", false
		}
		digest, digestErr := accountproof.Digest(m.Token)
		if err != nil || digestErr != nil || subtle.ConstantTimeCompare(digest, p.Digest) != 1 {
			return "invalid_proof", false
		}
	}
	if err := s.sender.SendAccount(ctx, m); err != nil {
		return mail.Failure(err)
	}
	return "", false
}
