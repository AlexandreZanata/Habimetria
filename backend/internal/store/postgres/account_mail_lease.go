package postgres

import (
	"context"
	"errors"
	"fmt"
	"io"

	"github.com/jackc/pgx/v5"
)

var ErrAccountMailLease = errors.New("account mail lease unavailable")

type AccountMailLease struct{ ID, Lease string }
type AccountMailPayload struct {
	User, Recipient    string
	Digest, Ciphertext []byte
	Current, Unexpired bool
}

func (p AccountMailPayload) Format(state fmt.State, _ rune) {
	_, _ = io.WriteString(state, "[account mail payload redacted]")
}
func (p AccountMailPayload) MarshalJSON() ([]byte, error) {
	return []byte(`"[account mail payload redacted]"`), nil
}

func (s *AccountStore) ClaimMail(ctx context.Context) (*AccountMailLease, error) {
	_, err := s.pool.Exec(ctx, `WITH exhausted AS (SELECT id FROM account_mail_outbox WHERE attempts>=5 AND
		((state='pending' AND next_attempt_at<=now()) OR (state='sending' AND lease_until<=now()))
		ORDER BY next_attempt_at,id FOR UPDATE SKIP LOCKED LIMIT 20)
		UPDATE account_mail_outbox o SET state='failed',failure_code='attempt_limit',proof_ciphertext=NULL,
		completed_at=now(),lease_id=NULL,lease_until=NULL FROM exhausted e WHERE o.id=e.id`)
	if err != nil {
		return nil, fmt.Errorf("expire exhausted account mail: %w", err)
	}
	var lease AccountMailLease
	err = s.pool.QueryRow(ctx, `WITH candidate AS (SELECT id FROM account_mail_outbox WHERE attempts<5 AND
		((state='pending' AND next_attempt_at<=now()) OR (state='sending' AND lease_until<=now()))
		ORDER BY next_attempt_at,id FOR UPDATE SKIP LOCKED LIMIT 1)
		UPDATE account_mail_outbox o SET state='sending',attempts=attempts+1,lease_id=gen_random_uuid(),lease_until=now()+interval '60 seconds'
		FROM candidate c WHERE o.id=c.id RETURNING o.id::text,o.lease_id::text`).Scan(&lease.ID, &lease.Lease)
	if errors.Is(err, pgx.ErrNoRows) {
		return nil, nil
	}
	if err != nil {
		return nil, fmt.Errorf("claim account mail: %w", err)
	}
	return &lease, nil
}

func (s *AccountStore) MailPayload(ctx context.Context, lease AccountMailLease) (AccountMailPayload, error) {
	var payload AccountMailPayload
	err := s.pool.QueryRow(ctx, `SELECT u.id::text,u.email::text,o.token_hash,o.proof_ciphertext,
		u.registration_started_at IS NOT NULL AND u.email_verified_at IS NULL AND NOT u.panel_active
		AND u.panel_role='user' AND u.panel_password IS NULL AND u.panel_managed AND v.token_hash IS NOT NULL,
		COALESCE(v.expires_at>clock_timestamp(),false)
		FROM account_mail_outbox o JOIN users u ON u.id=o.user_id LEFT JOIN account_verifications v ON v.user_id=o.user_id AND v.token_hash=o.token_hash
		WHERE o.id=$1 AND o.lease_id=$2 AND o.state='sending' AND o.lease_until>clock_timestamp()`, lease.ID, lease.Lease).
		Scan(&payload.User, &payload.Recipient, &payload.Digest, &payload.Ciphertext, &payload.Current, &payload.Unexpired)
	if errors.Is(err, pgx.ErrNoRows) {
		return payload, ErrAccountMailLease
	}
	if err != nil {
		return payload, fmt.Errorf("read leased account mail: %w", err)
	}
	return payload, nil
}
