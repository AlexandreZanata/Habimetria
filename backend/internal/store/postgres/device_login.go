package postgres

import (
	"bytes"
	"context"
	"fmt"

	"github.com/jackc/pgx/v5"
)

func (s *AccountStore) StartDevice(ctx context.Context, user, id string, password, access, refresh []byte, device Device) (DeviceGrant, error) {
	grant := DeviceGrant{ID: id, User: user}
	err := pgx.BeginFunc(ctx, s.pool, func(tx pgx.Tx) error {
		if err := deviceUser(ctx, tx, user); err != nil {
			return err
		}
		var current []byte
		if err := tx.QueryRow(ctx, `SELECT panel_password FROM users WHERE id=$1`, user).Scan(&current); err != nil {
			return fmt.Errorf("recheck device credential: %w", err)
		}
		if !bytes.Equal(current, password) {
			return ErrNotFound
		}
		if err := pruneDeviceFamilies(ctx, tx); err != nil {
			return err
		}
		if _, err := tx.Exec(ctx, `UPDATE account_device_sessions SET revoked_at=clock_timestamp(),revoke_reason='replaced'
			WHERE user_id=$1 AND device_id=$2 AND revoked_at IS NULL`, user, device.ID); err != nil {
			return fmt.Errorf("replace installation session: %w", err)
		}
		var count int
		if err := tx.QueryRow(ctx, `SELECT count(*) FROM account_device_sessions WHERE user_id=$1 AND revoked_at IS NULL
			AND expires_at>clock_timestamp()`, user).Scan(&count); err != nil {
			return fmt.Errorf("count device sessions: %w", err)
		}
		if count >= 20 {
			return ErrSessionLimit
		}
		if err := tx.QueryRow(ctx, `INSERT INTO account_device_sessions(id,user_id,device_id,platform,label)
			VALUES($1,$2,$3,$4,$5) RETURNING expires_at`, id, user, device.ID, device.Platform, device.Label).Scan(&grant.Expires); err != nil {
			return fmt.Errorf("create device family: %w", err)
		}
		if err := deviceAccess(ctx, tx, &grant, access, refresh); err != nil {
			return err
		}
		return panelAudit(ctx, tx, user, "signed_in", nil)
	})
	return grant, err
}
