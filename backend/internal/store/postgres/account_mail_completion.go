package postgres

import (
	"context"
	"errors"
	"fmt"

	"github.com/jackc/pgx/v5"
)

func (s *AccountStore) CompleteMail(ctx context.Context, lease AccountMailLease, code string, retry bool) (string, error) {
	switch code {
	case "", "transport", "rejected", "tls", "invalid_proof", "expired", "ineligible":
	default:
		return "", ErrAccountMailLease
	}
	if retry && code != "transport" {
		return "", ErrAccountMailLease
	}
	var state string
	err := s.pool.QueryRow(ctx, `UPDATE account_mail_outbox SET
		state=CASE WHEN $3='' THEN 'sent' WHEN $4 AND attempts<5 THEN 'pending' ELSE 'failed' END,
		failure_code=NULLIF($3,''),
		proof_ciphertext=CASE WHEN $4 AND attempts<5 THEN proof_ciphertext ELSE NULL END,
		completed_at=CASE WHEN $4 AND attempts<5 THEN NULL ELSE clock_timestamp() END,
		next_attempt_at=CASE WHEN $4 AND attempts<5 THEN now()+make_interval(secs=>LEAST(300,30*power(2,attempts-1))+random()*5) ELSE next_attempt_at END,
		lease_id=NULL,lease_until=NULL
		WHERE id=$1 AND lease_id=$2 AND state='sending' AND lease_until>clock_timestamp() RETURNING state`, lease.ID, lease.Lease, code, retry).Scan(&state)
	if err != nil {
		return "", fmt.Errorf("complete account mail: %w", mailLeaseError(err))
	}
	return state, nil
}

type AccountMailStatus struct{ Pending, Sending, Sent, Failed, Cancelled int }

func (s *AccountStore) MailStatus(ctx context.Context) (AccountMailStatus, error) {
	var status AccountMailStatus
	err := s.pool.QueryRow(ctx, `SELECT count(*) FILTER(WHERE state='pending'),count(*) FILTER(WHERE state='sending'),
		count(*) FILTER(WHERE state='sent'),count(*) FILTER(WHERE state='failed'),count(*) FILTER(WHERE state='cancelled') FROM account_mail_outbox`).
		Scan(&status.Pending, &status.Sending, &status.Sent, &status.Failed, &status.Cancelled)
	return status, err
}

func (s *AccountStore) PruneRegistrationProofs(ctx context.Context) error {
	_, err := s.pool.Exec(ctx, `DELETE FROM account_verifications WHERE user_id IN (SELECT v.user_id FROM account_verifications v
		WHERE v.expires_at<=now() AND NOT EXISTS(SELECT 1 FROM account_mail_outbox o WHERE o.user_id=v.user_id
		AND o.token_hash=v.token_hash AND o.state IN ('pending','sending')) ORDER BY v.expires_at LIMIT 100)`)
	if err != nil {
		return fmt.Errorf("prune expired verification digests: %w", err)
	}
	_, err = s.pool.Exec(ctx, `DELETE FROM account_registration_limits WHERE key_hash IN
		(SELECT key_hash FROM account_registration_limits WHERE expires_at<=now() ORDER BY expires_at LIMIT 100)`)
	return err
}

func mailLeaseError(err error) error {
	if errors.Is(err, pgx.ErrNoRows) {
		return ErrAccountMailLease
	}
	return err
}
