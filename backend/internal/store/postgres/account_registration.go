package postgres

import (
	"context"
	"errors"
	"fmt"

	"github.com/jackc/pgx/v5"
	"github.com/jackc/pgx/v5/pgxpool"
)

type AccountStore struct{ pool *pgxpool.Pool }

func NewAccountStore(pool *pgxpool.Pool) *AccountStore { return &AccountStore{pool} }

type AccountChallenge struct {
	Operation          string
	Digest, Ciphertext []byte
}

func (s *AccountStore) Register(ctx context.Context, email string, source, address []byte, issue func(string) AccountChallenge) error {
	return pgx.BeginFunc(ctx, s.pool, func(tx pgx.Tx) error {
		allowed, err := registrationLimits(ctx, tx, source, address)
		if err != nil || !allowed {
			return err
		}
		var user string
		err = tx.QueryRow(ctx, `INSERT INTO users(email,panel_managed,panel_active,registration_started_at)
			VALUES($1,true,false,now()) ON CONFLICT(email) DO NOTHING RETURNING id::text`, email).Scan(&user)
		if err != nil && !errors.Is(err, pgx.ErrNoRows) {
			return fmt.Errorf("create pending registration: %w", err)
		}
		var eligible bool
		err = tx.QueryRow(ctx, `SELECT id::text,registration_started_at IS NOT NULL AND email_verified_at IS NULL
			AND panel_role='user' AND NOT panel_active AND panel_password IS NULL AND panel_managed
			FROM users WHERE email=$1 FOR UPDATE`, email).Scan(&user, &eligible)
		if err != nil {
			return fmt.Errorf("lock registration: %w", err)
		}
		if !eligible {
			return nil
		}
		var cooling bool
		if err := tx.QueryRow(ctx, `SELECT EXISTS(SELECT 1 FROM account_verifications
			WHERE user_id=$1 AND created_at>now()-interval '60 seconds')`, user).Scan(&cooling); err != nil {
			return err
		}
		if cooling {
			return nil
		}
		proof := issue(user)
		if _, err := tx.Exec(ctx, `UPDATE account_mail_outbox SET state='cancelled',proof_ciphertext=NULL,
			completed_at=now(),failure_code='replaced',lease_id=NULL,lease_until=NULL WHERE user_id=$1 AND state IN ('pending','sending')`, user); err != nil {
			return err
		}
		if _, err := tx.Exec(ctx, `INSERT INTO account_verifications(user_id,token_hash) VALUES($1,$2)
			ON CONFLICT(user_id) DO UPDATE SET token_hash=excluded.token_hash,created_at=now(),expires_at=now()+interval '30 minutes'`, user, proof.Digest); err != nil {
			return err
		}
		_, err = tx.Exec(ctx, `INSERT INTO account_mail_outbox(id,user_id,token_hash,proof_ciphertext) VALUES($1,$2,$3,$4)`,
			proof.Operation, user, proof.Digest, proof.Ciphertext)
		return err
	})
}
