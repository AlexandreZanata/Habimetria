package postgres

import (
	"context"
	"errors"
	"fmt"

	"github.com/jackc/pgx/v5"
)

var ErrPanelRate = errors.New("too many authentication attempts")

func (s *PanelStore) AllowLogin(ctx context.Context, key []byte, limit int) error {
	var attempts int
	err := s.pool.QueryRow(ctx, `INSERT INTO panel_login_limits(key_hash,attempts,expires_at)
		VALUES ($1,1,now()+interval '15 minutes')
		ON CONFLICT(key_hash) DO UPDATE SET
		attempts=CASE WHEN panel_login_limits.expires_at <= now() THEN 1 ELSE panel_login_limits.attempts+1 END,
		expires_at=CASE WHEN panel_login_limits.expires_at <= now() THEN excluded.expires_at ELSE panel_login_limits.expires_at END
		WHERE panel_login_limits.expires_at <= now() OR panel_login_limits.attempts < $2 RETURNING attempts`, key, limit).Scan(&attempts)
	if errors.Is(err, pgx.ErrNoRows) {
		return ErrPanelRate
	}
	if err != nil {
		return fmt.Errorf("rate-limit panel authentication: %w", err)
	}
	return nil
}

func (s *PanelStore) Credential(ctx context.Context, email string) (string, []byte, error) {
	var user string
	var hash []byte
	err := s.pool.QueryRow(ctx, `SELECT id::text,panel_password FROM users
		WHERE email=$1 AND panel_active AND panel_password IS NOT NULL`, email).Scan(&user, &hash)
	return user, hash, panelMissing(err)
}

func (s *PanelStore) StartSession(ctx context.Context, user string, password, session []byte) error {
	return pgx.BeginFunc(ctx, s.pool, func(tx pgx.Tx) error {
		var active string
		err := tx.QueryRow(ctx, `SELECT id::text FROM users WHERE id=$1 AND panel_active AND panel_password=$2 FOR UPDATE`, user, password).Scan(&active)
		if err != nil {
			return panelMissing(err)
		}
		return panelSession(ctx, tx, user, session)
	})
}

func panelSession(ctx context.Context, tx pgx.Tx, user string, session []byte) error {
	if _, err := tx.Exec(ctx, `DELETE FROM panel_sessions WHERE expires_at <= now()`); err != nil {
		return fmt.Errorf("expire sessions: %w", err)
	}
	if _, err := tx.Exec(ctx, `DELETE FROM panel_login_limits WHERE expires_at <= now()`); err != nil {
		return fmt.Errorf("expire login counters: %w", err)
	}
	if _, err := tx.Exec(ctx, `INSERT INTO panel_sessions(token_hash,user_id,expires_at)
		VALUES ($1,$2,now() + interval '12 hours')`, session, user); err != nil {
		return fmt.Errorf("create panel session: %w", err)
	}
	return panelAudit(ctx, tx, user, "signed_in", nil)
}
