package postgres

import (
	"context"
	"time"

	"vault/internal/authz"

	"github.com/jackc/pgx/v5"
)

type PanelRole struct {
	Role        string
	Permissions []string
}

func (s *PanelStore) Roles(ctx context.Context) ([]PanelRole, error) {
	rows, err := s.pool.Query(ctx, `SELECT role,permissions FROM panel_role_permissions ORDER BY role`)
	if err != nil {
		return nil, err
	}
	defer rows.Close()
	out := []PanelRole{}
	for rows.Next() {
		var p PanelRole
		if err := rows.Scan(&p.Role, &p.Permissions); err != nil {
			return nil, err
		}
		out = append(out, p)
	}
	return out, rows.Err()
}

func (s *PanelStore) SetRole(ctx context.Context, actor, role string, permissions []string) error {
	permissions, valid := authz.Normalize(role, permissions)
	if !valid {
		return ErrPanelDenied
	}
	return s.panelAdminWrite(ctx, actor, func(tx pgx.Tx, a PanelActor) error {
		if a.Role != "owner" {
			return ErrPanelDenied
		}
		tag, err := tx.Exec(ctx, `UPDATE panel_role_permissions SET permissions=$2 WHERE role=$1`, role, permissions)
		if err != nil {
			return err
		}
		if tag.RowsAffected() != 1 {
			return ErrNotFound
		}
		return panelAudit(ctx, tx, actor, "permissions_updated", nil)
	})
}

type PanelEvent struct {
	Event, Actor string
	At           time.Time
}

func (s *PanelStore) Activity(ctx context.Context, offset int) ([]PanelEvent, error) {
	rows, err := s.pool.Query(ctx, `SELECT a.event,u.display_name,a.occurred_at FROM panel_audit a JOIN users u ON u.id=a.actor_id
		WHERE a.event IN ('account_created','account_updated','access_issued','permissions_updated','signed_in','profile_updated')
		ORDER BY a.id DESC LIMIT 26 OFFSET $1`, offset)
	if err != nil {
		return nil, err
	}
	defer rows.Close()
	out := []PanelEvent{}
	for rows.Next() {
		var event PanelEvent
		if err := rows.Scan(&event.Event, &event.Actor, &event.At); err != nil {
			return nil, err
		}
		out = append(out, event)
	}
	return out, rows.Err()
}

type PanelMetrics struct {
	Accounts, Staff, Reports, Properties, Inspections *int
}

func (s *PanelStore) Metrics(ctx context.Context, a PanelActor, admin bool) (PanelMetrics, error) {
	var m PanelMetrics
	if admin {
		err := s.pool.QueryRow(ctx, `SELECT
			CASE WHEN $1 THEN (SELECT count(*) FROM users WHERE panel_role='user') END,
			CASE WHEN $1 THEN (SELECT count(*) FROM users WHERE panel_role<>'user' AND panel_active) END,
			CASE WHEN $2 THEN (SELECT count(*) FROM panel_report_access) END`, a.Can("accounts.read"), a.Can("reports.read")).
			Scan(&m.Accounts, &m.Staff, &m.Reports)
		return m, err
	}
	err := s.pool.QueryRow(ctx, `SELECT
		(SELECT count(*) FROM properties WHERE owner_user_id=$1),
		(SELECT count(*) FROM inspections i JOIN tenancies t ON t.id=i.tenancy_id JOIN properties p ON p.id=t.property_id WHERE p.owner_user_id=$1),
		(SELECT count(*) FROM reports r JOIN inspections i ON i.id=r.inspection_id JOIN tenancies t ON t.id=i.tenancy_id JOIN properties p ON p.id=t.property_id WHERE p.owner_user_id=$1 AND r.pdf_key IS NOT NULL)`, a.ID).
		Scan(&m.Properties, &m.Inspections, &m.Reports)
	return m, err
}
