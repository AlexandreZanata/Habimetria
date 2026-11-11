package postgres

import (
	"context"
	"errors"
	"fmt"

	"github.com/jackc/pgx/v5"
)

func registrationLimits(ctx context.Context, tx pgx.Tx, source, address []byte) (bool, error) {
	if _, err := tx.Exec(ctx, `DELETE FROM account_registration_limits WHERE key_hash IN
		(SELECT key_hash FROM account_registration_limits WHERE expires_at<=now() ORDER BY expires_at LIMIT 100)`); err != nil {
		return false, fmt.Errorf("expire registration limits: %w", err)
	}
	for i, key := range [][]byte{source, address} {
		limit := 10
		if i == 1 {
			limit = 3
		}
		var attempts int
		err := tx.QueryRow(ctx, `INSERT INTO account_registration_limits(key_hash,attempts,expires_at)
			VALUES($1,1,now()+interval '15 minutes') ON CONFLICT(key_hash) DO UPDATE SET
			attempts=CASE WHEN account_registration_limits.expires_at<=now() THEN 1 ELSE account_registration_limits.attempts+1 END,
			expires_at=CASE WHEN account_registration_limits.expires_at<=now() THEN excluded.expires_at ELSE account_registration_limits.expires_at END
			WHERE account_registration_limits.expires_at<=now() OR account_registration_limits.attempts<$2 RETURNING attempts`, key, limit).Scan(&attempts)
		if errors.Is(err, pgx.ErrNoRows) {
			return false, nil
		}
		if err != nil {
			return false, fmt.Errorf("limit registration: %w", err)
		}
	}
	return true, nil
}
