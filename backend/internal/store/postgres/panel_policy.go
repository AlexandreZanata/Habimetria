package postgres

import (
	"context"
	"slices"
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

func (s *PanelStore) SetRole(ctx context.Context, actor, role string, permissions []string, reason string) error {
	permissions, valid := authz.Normalize(role, permissions)
	if !valid || !authz.ChangeReason(reason) {
		return ErrPanelDenied
	}
	return s.panelAdminWrite(ctx, actor, func(tx pgx.Tx, a PanelActor) error {
		if a.Role != "owner" {
			return ErrPanelDenied
		}
		var before []string
		if err := tx.QueryRow(ctx, `SELECT permissions FROM panel_role_permissions WHERE role=$1 FOR UPDATE`, role).Scan(&before); err != nil {
			return panelMissing(err)
		}
		if slices.Equal(before, permissions) {
			return nil
		}
		tag, err := tx.Exec(ctx, `UPDATE panel_role_permissions SET permissions=$2 WHERE role=$1`, role, permissions)
		if err != nil {
			return err
		}
		if tag.RowsAffected() != 1 {
			return ErrNotFound
		}
		return panelPolicyAudit(ctx, tx, actor, "permissions_updated", reason, nil, panelRoleState(role, before), panelRoleState(role, permissions))
	})
}

type PanelEvent struct {
	Event, Actor               string
	Operation, Reason, Subject *string
	Before, After              PanelPolicyState
	At                         time.Time
}

func (s *PanelStore) Activity(ctx context.Context, offset int) ([]PanelEvent, error) {
	rows, err := s.pool.Query(ctx, `SELECT a.event,u.display_name,a.occurred_at,
		a.operation_id::text,a.reason_code,a.subject_id::text,a.before_role,a.after_role,
		a.before_active,a.after_active,a.before_permissions,a.after_permissions FROM panel_audit a JOIN users u ON u.id=a.actor_id
		WHERE a.event IN ('account_created','account_updated','access_issued','permissions_updated','signed_in','profile_updated')
		ORDER BY a.id DESC LIMIT 26 OFFSET $1`, offset)
	if err != nil {
		return nil, err
	}
	defer rows.Close()
	out := []PanelEvent{}
	for rows.Next() {
		var event PanelEvent
		if err := rows.Scan(&event.Event, &event.Actor, &event.At, &event.Operation, &event.Reason, &event.Subject,
			&event.Before.Role, &event.After.Role, &event.Before.Active, &event.After.Active, &event.Before.Permissions, &event.After.Permissions); err != nil {
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
