package postgres

import (
	"context"
	"errors"
	"fmt"

	"github.com/jackc/pgx/v5"
)

func (s *AccountStore) RequestRecovery(ctx context.Context, email string, source, address []byte, issue func(string) AccountChallenge) error {
	return pgx.BeginFunc(ctx, s.pool, func(tx pgx.Tx) error {
		allowed, err := registrationLimits(ctx, tx, source, address)
		if err != nil || !allowed {
			return err
		}
		var user string
		err = tx.QueryRow(ctx, `SELECT id::text FROM users WHERE email=$1 AND panel_active AND panel_role='user'
			AND panel_managed AND panel_password IS NOT NULL AND NOT panel_primary
			AND (registration_started_at IS NULL OR email_verified_at IS NOT NULL) FOR UPDATE`, email).Scan(&user)
		if errors.Is(err, pgx.ErrNoRows) {
			return nil
		}
		if err != nil {
			return fmt.Errorf("lock recovery identity: %w", err)
		}
		var cooling bool
		if err := tx.QueryRow(ctx, `SELECT EXISTS(SELECT 1 FROM account_password_recoveries WHERE user_id=$1
			AND created_at>now()-interval '60 seconds')`, user).Scan(&cooling); err != nil {
			return fmt.Errorf("check recovery cooldown: %w", err)
		}
		if cooling {
			return nil
		}
		proof := issue(user)
		if _, err := tx.Exec(ctx, `UPDATE account_mail_outbox SET state='cancelled',proof_ciphertext=NULL,
			completed_at=clock_timestamp(),failure_code='replaced',lease_id=NULL,lease_until=NULL
			WHERE user_id=$1 AND template='password_recovery' AND state IN ('pending','sending')`, user); err != nil {
			return fmt.Errorf("replace recovery mail: %w", err)
		}
		if _, err := tx.Exec(ctx, `INSERT INTO account_password_recoveries(user_id,token_hash) VALUES($1,$2)
			ON CONFLICT(user_id) DO UPDATE SET token_hash=excluded.token_hash,created_at=now(),expires_at=now()+interval '30 minutes'`, user, proof.Digest); err != nil {
			return fmt.Errorf("persist recovery proof: %w", err)
		}
		if _, err := tx.Exec(ctx, `INSERT INTO account_mail_outbox(id,user_id,template,token_hash,proof_ciphertext)
			VALUES($1,$2,'password_recovery',$3,$4)`, proof.Operation, user, proof.Digest, proof.Ciphertext); err != nil {
			return fmt.Errorf("enqueue recovery: %w", err)
		}
		return nil
	})
}
