package postgres

import (
	"context"
	"fmt"

	"github.com/jackc/pgx/v5"
)

func (s *AccountStore) RotateDevice(ctx context.Context, digest []byte, device string, access, next []byte) (DeviceGrant, error) {
	var grant DeviceGrant
	err := s.pool.QueryRow(ctx, `SELECT f.id::text,f.user_id::text FROM account_refresh_tokens r
		JOIN account_device_sessions f ON f.id=r.session_id WHERE r.token_hash=$1`, digest).Scan(&grant.ID, &grant.User)
	if err != nil {
		return grant, panelMissing(err)
	}
	denied := false
	err = pgx.BeginFunc(ctx, s.pool, func(tx pgx.Tx) error {
		if err := deviceUser(ctx, tx, grant.User); err != nil {
			return err
		}
		var actual string
		var live, unexpired bool
		var generation int
		if err := tx.QueryRow(ctx, `SELECT device_id::text,revoked_at IS NULL,expires_at>clock_timestamp(),generation,expires_at
			FROM account_device_sessions WHERE id=$1 AND user_id=$2 FOR UPDATE`, grant.ID, grant.User).
			Scan(&actual, &live, &unexpired, &generation, &grant.Expires); err != nil {
			return panelMissing(err)
		}
		if !live || actual != device {
			denied = true
			return nil
		}
		var used bool
		if err := tx.QueryRow(ctx, `SELECT used_at IS NOT NULL FROM account_refresh_tokens
			WHERE token_hash=$1 AND session_id=$2 FOR UPDATE`, digest, grant.ID).Scan(&used); err != nil {
			return panelMissing(err)
		}
		reason := ""
		switch {
		case !unexpired:
			reason = "expired"
		case used:
			reason = "refresh_reuse"
		case generation >= 8192:
			reason = "rotation_limit"
		}
		if reason != "" {
			denied = true
			return revokeDevice(ctx, tx, grant.ID, reason)
		}
		if _, err := tx.Exec(ctx, `UPDATE account_refresh_tokens SET used_at=clock_timestamp() WHERE token_hash=$1`, digest); err != nil {
			return fmt.Errorf("consume device refresh: %w", err)
		}
		if _, err := tx.Exec(ctx, `UPDATE account_device_sessions SET generation=generation+1,last_used_at=clock_timestamp() WHERE id=$1`, grant.ID); err != nil {
			return fmt.Errorf("record device rotation: %w", err)
		}
		if err := deviceAccess(ctx, tx, &grant, access, next); err != nil {
			return err
		}
		return pruneDeviceFamilies(ctx, tx)
	})
	// A reused proof must commit revocation, never roll it back with the denial.
	if err == nil && denied {
		err = ErrNotFound
	}
	return grant, err
}
