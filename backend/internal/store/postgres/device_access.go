package postgres

import (
	"context"
	"errors"
	"fmt"
	"time"

	"github.com/jackc/pgx/v5"
)

var ErrSessionLimit = errors.New("device session limit reached")

type Device struct {
	ID       string `json:"id"`
	Platform string `json:"platform"`
	Label    string `json:"label"`
}
type DeviceGrant struct {
	ID, User string
	Expires  time.Time
	Seconds  int
}
type DeviceSession struct {
	ID       string    `json:"session_id"`
	Platform string    `json:"platform"`
	Label    string    `json:"label"`
	Created  time.Time `json:"created_at"`
	LastUsed time.Time `json:"last_used_at"`
	Expires  time.Time `json:"expires_at"`
}

func deviceUser(ctx context.Context, tx pgx.Tx, user string) error {
	if _, err := tx.Exec(ctx, `LOCK TABLE users IN ROW EXCLUSIVE MODE`); err != nil {
		return fmt.Errorf("lock device identity table: %w", err)
	}
	var id string
	err := tx.QueryRow(ctx, `SELECT id::text FROM users WHERE id=$1 AND panel_active AND panel_role='user'
		AND panel_password IS NOT NULL FOR UPDATE`, user).Scan(&id)
	return panelMissing(err)
}

func revokeDevice(ctx context.Context, tx pgx.Tx, id, reason string) error {
	if _, err := tx.Exec(ctx, `UPDATE account_device_sessions SET revoked_at=clock_timestamp(),revoke_reason=$2
		WHERE id=$1 AND revoked_at IS NULL`, id, reason); err != nil {
		return fmt.Errorf("revoke device family: %w", err)
	}
	return nil
}

func deviceAccess(ctx context.Context, tx pgx.Tx, grant *DeviceGrant, access, refresh []byte) error {
	if _, err := tx.Exec(ctx, `DELETE FROM account_access_sessions WHERE family_id=$1`, grant.ID); err != nil {
		return fmt.Errorf("replace device access: %w", err)
	}
	if _, err := tx.Exec(ctx, `INSERT INTO account_refresh_tokens(token_hash,session_id) VALUES($1,$2)`, refresh, grant.ID); err != nil {
		return fmt.Errorf("issue device refresh: %w", err)
	}
	if err := tx.QueryRow(ctx, `INSERT INTO account_access_sessions(token_hash,user_id,family_id,created_at,expires_at)
		VALUES($1,$2,$3,statement_timestamp(),LEAST(statement_timestamp()+interval '15 minutes',$4))
		RETURNING floor(EXTRACT(EPOCH FROM(expires_at-created_at)))::int`, access, grant.User, grant.ID, grant.Expires).Scan(&grant.Seconds); err != nil {
		return fmt.Errorf("issue device access: %w", err)
	}
	return nil
}

func pruneDeviceFamilies(ctx context.Context, tx pgx.Tx) error {
	if _, err := tx.Exec(ctx, `DELETE FROM account_device_sessions WHERE id IN (SELECT id FROM account_device_sessions
		WHERE expires_at<=clock_timestamp() ORDER BY expires_at,id FOR UPDATE SKIP LOCKED LIMIT 100)`); err != nil {
		return fmt.Errorf("prune expired device families: %w", err)
	}
	return nil
}
