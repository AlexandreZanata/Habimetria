package postgres

import (
	"context"
	"fmt"

	"github.com/jackc/pgx/v5"
)

func (s *AccountStore) ListDevices(ctx context.Context, user string) ([]DeviceSession, error) {
	result := []DeviceSession{}
	err := pgx.BeginFunc(ctx, s.pool, func(tx pgx.Tx) error {
		if err := deviceUser(ctx, tx, user); err != nil {
			return err
		}
		rows, err := tx.Query(ctx, `SELECT id::text,platform,label,created_at,last_used_at,expires_at FROM account_device_sessions
			WHERE user_id=$1 AND revoked_at IS NULL AND expires_at>clock_timestamp() ORDER BY created_at,id LIMIT 20`, user)
		if err != nil {
			return fmt.Errorf("list own devices: %w", err)
		}
		defer rows.Close()
		for rows.Next() {
			var d DeviceSession
			if err := rows.Scan(&d.ID, &d.Platform, &d.Label, &d.Created, &d.LastUsed, &d.Expires); err != nil {
				return fmt.Errorf("scan device: %w", err)
			}
			result = append(result, d)
		}
		return rows.Err()
	})
	return result, err
}

func (s *AccountStore) RevokeDevice(ctx context.Context, user, id string) error {
	return pgx.BeginFunc(ctx, s.pool, func(tx pgx.Tx) error {
		if err := deviceUser(ctx, tx, user); err != nil {
			return err
		}
		var owned string
		if err := tx.QueryRow(ctx, `SELECT id::text FROM account_device_sessions WHERE id=$1 AND user_id=$2 FOR UPDATE`, id, user).Scan(&owned); err != nil {
			return panelMissing(err)
		}
		return revokeDevice(ctx, tx, owned, "logout")
	})
}
