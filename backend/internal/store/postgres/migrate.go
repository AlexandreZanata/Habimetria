package postgres

import (
	"context"
	"errors"
	"fmt"
	"io/fs"
	"sort"

	"github.com/jackc/pgx/v5"
	"github.com/jackc/pgx/v5/pgxpool"
)

// Apply runs SQL files from migrationFS in filename order.
// Each file runs once, tracked in schema_migrations. Idempotent.
func Apply(ctx context.Context, pool *pgxpool.Pool, migrationFS fs.FS) error {
	return pgx.BeginFunc(ctx, pool, func(tx pgx.Tx) error {
		if _, err := tx.Exec(ctx, `SELECT pg_advisory_xact_lock(4821, 1)`); err != nil {
			return fmt.Errorf("lock schema migrations: %w", err)
		}
		return applyMigrations(ctx, tx, migrationFS)
	})
}

func applyMigrations(ctx context.Context, tx pgx.Tx, migrationFS fs.FS) error {
	if err := ensureVersionTable(ctx, tx); err != nil {
		return err
	}
	entries, err := fs.ReadDir(migrationFS, ".")
	if err != nil {
		return fmt.Errorf("read migrations: %w", err)
	}
	names := make([]string, 0, len(entries))
	for _, e := range entries {
		if !e.IsDir() && len(e.Name()) > 4 && e.Name()[len(e.Name())-4:] == ".sql" {
			names = append(names, e.Name())
		}
	}
	sort.Strings(names)
	for _, name := range names {
		applied, err := isApplied(ctx, tx, name)
		if err != nil {
			return err
		}
		if applied {
			continue
		}
		sql, err := fs.ReadFile(migrationFS, name)
		if err != nil {
			return fmt.Errorf("read %s: %w", name, err)
		}
		if _, err := tx.Exec(ctx, string(sql)); err != nil {
			return fmt.Errorf("apply %s: %w", name, err)
		}
		if _, err := tx.Exec(ctx, `INSERT INTO schema_migrations (filename) VALUES ($1)`, name); err != nil {
			return fmt.Errorf("record %s: %w", name, err)
		}
	}
	return nil
}

func ensureVersionTable(ctx context.Context, tx pgx.Tx) error {
	_, err := tx.Exec(ctx, `CREATE TABLE IF NOT EXISTS schema_migrations (
	  filename TEXT PRIMARY KEY,
	  applied_at TIMESTAMPTZ NOT NULL DEFAULT now()
	)`)
	if err != nil {
		return fmt.Errorf("ensure schema_migrations: %w", err)
	}
	return nil
}

func isApplied(ctx context.Context, tx pgx.Tx, name string) (bool, error) {
	var one int
	err := tx.QueryRow(ctx, `SELECT 1 FROM schema_migrations WHERE filename=$1`, name).Scan(&one)
	if err != nil {
		if errors.Is(err, pgx.ErrNoRows) {
			return false, nil
		}
		return false, fmt.Errorf("check %s: %w", name, err)
	}
	return true, nil
}
