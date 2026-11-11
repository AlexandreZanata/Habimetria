package postgres

import (
	"context"
	"fmt"

	"github.com/jackc/pgx/v5"
)

func (s *AccountStore) VerifyAccount(ctx context.Context, digest, password []byte) error {
	return pgx.BeginFunc(ctx, s.pool, func(tx pgx.Tx) error {
		// Match the administrative/registration lock order: users table, User,
		// then proof. Recheck the proof after waiting for concurrent consumers.
		if _, err := tx.Exec(ctx, `LOCK TABLE users IN ROW EXCLUSIVE MODE`); err != nil {
			return fmt.Errorf("lock verification identities: %w", err)
		}
		var user string
		err := tx.QueryRow(ctx, `SELECT u.id::text FROM users u JOIN account_verifications v ON v.user_id=u.id
			WHERE v.token_hash=$1 AND v.purpose='email_verification'
			AND u.registration_started_at IS NOT NULL AND u.email_verified_at IS NULL
			AND NOT u.panel_active AND u.panel_managed AND u.panel_role='user' AND NOT u.panel_primary
			AND u.panel_password IS NULL FOR UPDATE OF u`, digest).Scan(&user)
		if err != nil {
			return panelMissing(err)
		}
		tag, err := tx.Exec(ctx, `DELETE FROM account_verifications WHERE user_id=$1 AND token_hash=$2
			AND purpose='email_verification' AND expires_at>clock_timestamp()`, user, digest)
		if err != nil {
			return fmt.Errorf("consume email verification: %w", err)
		}
		if tag.RowsAffected() != 1 {
			return ErrNotFound
		}
		if _, err := tx.Exec(ctx, `UPDATE users SET panel_password=$2,panel_active=true,email_verified_at=clock_timestamp() WHERE id=$1`, user, password); err != nil {
			return fmt.Errorf("verify account identity: %w", err)
		}
		if _, err := tx.Exec(ctx, `UPDATE account_mail_outbox SET state='cancelled',proof_ciphertext=NULL,
			completed_at=clock_timestamp(),failure_code='ineligible',lease_id=NULL,lease_until=NULL
			WHERE user_id=$1 AND state IN ('pending','sending')`, user); err != nil {
			return fmt.Errorf("cancel consumed verification delivery: %w", err)
		}
		if _, err := tx.Exec(ctx, `DELETE FROM panel_access_links WHERE user_id=$1`, user); err != nil {
			return fmt.Errorf("clear pending account access: %w", err)
		}
		return panelAudit(ctx, tx, user, "email_verified", &user)
	})
}
