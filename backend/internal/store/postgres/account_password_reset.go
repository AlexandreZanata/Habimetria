package postgres

import (
	"context"
	"fmt"

	"github.com/jackc/pgx/v5"
)

func (s *AccountStore) ResetPassword(ctx context.Context, digest, password []byte) error {
	return pgx.BeginFunc(ctx, s.pool, func(tx pgx.Tx) error {
		if _, err := tx.Exec(ctx, `LOCK TABLE users IN ROW EXCLUSIVE MODE`); err != nil {
			return fmt.Errorf("lock reset identity table: %w", err)
		}
		var user string
		err := tx.QueryRow(ctx, `SELECT u.id::text FROM users u JOIN account_password_recoveries r ON r.user_id=u.id
			WHERE r.token_hash=$1 AND r.purpose='password_recovery' AND u.panel_active AND u.panel_role='user'
			AND u.panel_managed AND u.panel_password IS NOT NULL AND NOT u.panel_primary
			AND (u.registration_started_at IS NULL OR u.email_verified_at IS NOT NULL) FOR UPDATE OF u`, digest).Scan(&user)
		if err != nil {
			return panelMissing(err)
		}
		tag, err := tx.Exec(ctx, `DELETE FROM account_password_recoveries WHERE user_id=$1 AND token_hash=$2
			AND purpose='password_recovery' AND expires_at>clock_timestamp()`, user, digest)
		if err != nil {
			return fmt.Errorf("consume recovery: %w", err)
		}
		if tag.RowsAffected() != 1 {
			return ErrNotFound
		}
		if _, err := tx.Exec(ctx, `UPDATE users SET panel_password=$2 WHERE id=$1`, user, password); err != nil {
			return fmt.Errorf("replace password: %w", err)
		}
		if _, err := tx.Exec(ctx, `DELETE FROM account_access_sessions WHERE user_id=$1`, user); err != nil {
			return fmt.Errorf("revoke API access: %w", err)
		}
		if _, err := tx.Exec(ctx, `DELETE FROM panel_sessions WHERE user_id=$1`, user); err != nil {
			return fmt.Errorf("revoke browser sessions: %w", err)
		}
		if _, err := tx.Exec(ctx, `DELETE FROM panel_access_links WHERE user_id=$1`, user); err != nil {
			return fmt.Errorf("revoke activation links: %w", err)
		}
		if _, err := tx.Exec(ctx, `DELETE FROM account_verifications WHERE user_id=$1`, user); err != nil {
			return fmt.Errorf("invalidate verification: %w", err)
		}
		if _, err := tx.Exec(ctx, `UPDATE account_mail_outbox SET state='cancelled',proof_ciphertext=NULL,
			completed_at=clock_timestamp(),failure_code='ineligible',lease_id=NULL,lease_until=NULL
			WHERE user_id=$1 AND template<>'password_changed' AND state IN ('pending','sending')`, user); err != nil {
			return fmt.Errorf("invalidate reset proof mail: %w", err)
		}
		if err := panelAudit(ctx, tx, user, "password_reset", &user); err != nil {
			return err
		}
		if _, err := tx.Exec(ctx, `INSERT INTO account_mail_outbox(id,user_id,template,recipient_digest)
			SELECT gen_random_uuid(),id,'password_changed',digest(lower(email::text),'sha256') FROM users WHERE id=$1`, user); err != nil {
			return fmt.Errorf("enqueue password security notice: %w", err)
		}
		return nil
	})
}
