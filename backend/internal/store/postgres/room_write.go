package postgres

import (
	"context"
	"errors"
	"fmt"

	"github.com/jackc/pgx/v5"
	"github.com/jackc/pgx/v5/pgxpool"
)

func roomWrite(ctx context.Context, pool *pgxpool.Pool, inspection string, write func(pgx.Tx) error) error {
	return pgx.BeginFunc(ctx, pool, func(tx pgx.Tx) error {
		var status string
		if err := tx.QueryRow(ctx, `SELECT status FROM inspections WHERE id=$1 FOR UPDATE`, inspection).Scan(&status); err != nil {
			if errors.Is(err, pgx.ErrNoRows) {
				return fmt.Errorf("Room parent: %w", ErrNotFound)
			}
			return fmt.Errorf("lock Room parent: %w", err)
		}
		if status != "DRAFT" {
			return fmt.Errorf("write Room: %w", ErrSealed)
		}
		return write(tx)
	})
}
