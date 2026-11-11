package postgres

import (
	"context"
	"fmt"

	"github.com/jackc/pgx/v5"
)

func (s *AccountStore) StartAccess(ctx context.Context, user string, password, digest []byte) error {
	return pgx.BeginFunc(ctx, s.pool, func(tx pgx.Tx) error {
		var id string
		err := tx.QueryRow(ctx, `SELECT id::text FROM users WHERE id=$1 AND panel_active
			AND panel_role='user' AND panel_password=$2 FOR UPDATE`, user, password).Scan(&id)
		if err != nil {
			return panelMissing(err)
		}
		if _, err := tx.Exec(ctx, `DELETE FROM account_access_sessions WHERE token_hash IN (
			SELECT token_hash FROM account_access_sessions WHERE expires_at<=clock_timestamp() LIMIT 100)`); err != nil {
			return fmt.Errorf("expire API access: %w", err)
		}
		if _, err := tx.Exec(ctx, `INSERT INTO account_access_sessions(token_hash,user_id,created_at,expires_at)
			VALUES($1,$2,statement_timestamp(),statement_timestamp()+interval '15 minutes')`, digest, user); err != nil {
			return fmt.Errorf("issue API access: %w", err)
		}
		return panelAudit(ctx, tx, user, "signed_in", nil)
	})
}

func (s *AccountStore) AccessActor(ctx context.Context, digest []byte) (PanelActor, error) {
	a, err := scanPanelActor(s.pool.QueryRow(ctx, `SELECT `+panelAccountColumns+`, '{}'::text[]
		FROM users u JOIN account_access_sessions s ON s.user_id=u.id
		WHERE s.token_hash=$1 AND s.audience='account_api' AND s.expires_at>clock_timestamp()
		AND u.panel_active AND u.panel_role='user' AND u.panel_password IS NOT NULL`, digest))
	if err != nil {
		return PanelActor{}, fmt.Errorf("read API access: %w", err)
	}
	return a, nil
}

func (s *AccountStore) EndAccess(ctx context.Context, digest []byte) error {
	if _, err := s.pool.Exec(ctx, `DELETE FROM account_access_sessions WHERE token_hash=$1`, digest); err != nil {
		return fmt.Errorf("revoke API access: %w", err)
	}
	return nil
}
