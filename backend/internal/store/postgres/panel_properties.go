package postgres

import (
	"context"
	"fmt"
	"time"

	"github.com/jackc/pgx/v5"
)

type PanelInspection struct {
	ID, Address, Kind, Status string
	CreatedAt                 time.Time
}

func (s *PanelStore) Properties(ctx context.Context, owner string, offset int) ([]Property, error) {
	rows, err := s.pool.Query(ctx, `SELECT id::text,address,created_at FROM properties
		WHERE owner_user_id=$1 ORDER BY created_at DESC,id DESC LIMIT 26 OFFSET $2`, owner, offset)
	if err != nil {
		return nil, err
	}
	return pgx.CollectRows(rows, func(row pgx.CollectableRow) (Property, error) {
		var p Property
		err := row.Scan(&p.ID, &p.Address, &p.CreatedAt)
		return p, err
	})
}

func (s *PanelStore) CreateOwnedProperty(ctx context.Context, owner, address string) error {
	tag, err := s.pool.Exec(ctx, `INSERT INTO properties(owner_user_id,address)
		SELECT id,$2 FROM users WHERE id=$1 AND panel_active`, owner, address)
	if err != nil {
		return fmt.Errorf("create owned property: %w", err)
	}
	if tag.RowsAffected() != 1 {
		return ErrPanelDenied
	}
	return nil
}

func (s *PanelStore) Inspections(ctx context.Context, owner string, offset int) ([]PanelInspection, error) {
	rows, err := s.pool.Query(ctx, `SELECT i.id::text,p.address,i.kind,i.status,i.created_at FROM inspections i
		JOIN tenancies t ON t.id=i.tenancy_id JOIN properties p ON p.id=t.property_id
		WHERE p.owner_user_id=$1 ORDER BY i.created_at DESC,i.id DESC LIMIT 26 OFFSET $2`, owner, offset)
	if err != nil {
		return nil, err
	}
	return pgx.CollectRows(rows, func(row pgx.CollectableRow) (PanelInspection, error) {
		var i PanelInspection
		err := row.Scan(&i.ID, &i.Address, &i.Kind, &i.Status, &i.CreatedAt)
		return i, err
	})
}

func (s *PanelStore) CreateOwnedInspection(ctx context.Context, owner, property string) error {
	// The Web form starts a new entry tenancy, like creating a new
	// inspection from an existing property in the mobile flow.
	tag, err := s.pool.Exec(ctx, `WITH t AS (
		INSERT INTO tenancies(property_id) SELECT p.id FROM properties p
		JOIN users u ON u.id=p.owner_user_id WHERE p.id=$1 AND u.id=$2 AND u.panel_active
		RETURNING id
	) INSERT INTO inspections(tenancy_id) SELECT id FROM t`, property, owner)
	if err != nil {
		return fmt.Errorf("create owned inspection: %w", err)
	}
	if tag.RowsAffected() != 1 {
		return ErrNotFound
	}
	return nil
}
