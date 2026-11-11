package postgres

import (
	"context"
	"errors"
	"fmt"
	"strings"

	"vault/internal/authz"

	"github.com/jackc/pgx/v5"
)

func (s *PanelStore) Bootstrap(ctx context.Context, email, name string, hash []byte) error {
	return pgx.BeginFunc(ctx, s.pool, func(tx pgx.Tx) error {
		if _, err := tx.Exec(ctx, `LOCK TABLE users IN SHARE ROW EXCLUSIVE MODE`); err != nil {
			return err
		}
		var id string
		err := tx.QueryRow(ctx, `SELECT id::text FROM users WHERE panel_primary AND email=$1`, email).Scan(&id)
		if errors.Is(err, pgx.ErrNoRows) {
			var exists bool
			if err := tx.QueryRow(ctx, `SELECT EXISTS(SELECT 1 FROM users WHERE panel_primary)`).Scan(&exists); err != nil {
				return err
			}
			if exists {
				return ErrPanelPrimary
			}
			err = tx.QueryRow(ctx, `INSERT INTO users(email,display_name,panel_role,panel_primary,panel_managed)
				VALUES ($1,$2,'owner',true,true)
				ON CONFLICT(email) DO UPDATE SET display_name=excluded.display_name, panel_role='owner',
					panel_primary=true, panel_active=true, panel_managed=true RETURNING id::text`, email, name).Scan(&id)
		}
		if err != nil {
			return fmt.Errorf("bootstrap primary account: %w", err)
		}
		return panelLink(ctx, tx, id, hash)
	})
}

// Administrative account operations serialize with each other. Permission
// changes and suspensions use the same lock before reading the actor.
func (s *PanelStore) panelAdminWrite(ctx context.Context, actor string, f func(pgx.Tx, PanelActor) error) error {
	return pgx.BeginFunc(ctx, s.pool, func(tx pgx.Tx) error {
		if _, err := tx.Exec(ctx, `LOCK TABLE users IN SHARE ROW EXCLUSIVE MODE`); err != nil {
			return err
		}
		a, err := scanPanelActor(tx.QueryRow(ctx, `SELECT `+panelAccountColumns+`,`+panelPermissionColumn+`
			FROM users u WHERE u.id=$1 AND u.panel_active`, actor))
		if err != nil {
			return err
		}
		return f(tx, a)
	})
}

func (s *PanelStore) Invite(ctx context.Context, actor, email, name, role string, hash []byte) error {
	return s.panelAdminWrite(ctx, actor, func(tx pgx.Tx, a PanelActor) error {
		if !a.Can("accounts.write") || (role != "user" && a.Role != "owner") {
			return ErrPanelDenied
		}
		var id string
		err := tx.QueryRow(ctx, `INSERT INTO users(email,display_name,panel_role,panel_managed) VALUES ($1,$2,$3,true)
			RETURNING id::text`, email, name, role).Scan(&id)
		if err != nil {
			return fmt.Errorf("create account: %w", panelUnique(err))
		}
		if err := panelLink(ctx, tx, id, hash); err != nil {
			return err
		}
		return panelPolicyAudit(ctx, tx, actor, "account_created", "account_onboarding", &id, PanelPolicyState{}, panelAccountState(role, true))
	})
}

func panelTarget(ctx context.Context, tx pgx.Tx, a PanelActor, id string) (role string, active bool, err error) {
	if !a.Can("accounts.write") {
		return "", false, ErrPanelDenied
	}
	var primary, pending bool
	err = tx.QueryRow(ctx, `SELECT panel_role,panel_active,panel_primary,
		registration_started_at IS NOT NULL AND email_verified_at IS NULL FROM users WHERE id=$1 FOR UPDATE`, id).Scan(&role, &active, &primary, &pending)
	if err != nil {
		return "", false, panelMissing(err)
	}
	if pending || (role != "user" && a.Role != "owner") {
		return "", false, ErrPanelDenied
	}
	if primary {
		return "", false, ErrPanelPrimary
	}
	return role, active, nil
}

func (s *PanelStore) Reissue(ctx context.Context, actor, id string, hash []byte) error {
	return s.panelAdminWrite(ctx, actor, func(tx pgx.Tx, a PanelActor) error {
		role, active, err := panelTarget(ctx, tx, a, id)
		if err != nil {
			return err
		}
		var pending bool
		if err := tx.QueryRow(ctx, `SELECT panel_managed AND panel_password IS NULL FROM users WHERE id=$1`, id).Scan(&pending); err != nil {
			return err
		}
		if !active || !pending {
			return ErrPanelDenied
		}
		if err := panelLink(ctx, tx, id, hash); err != nil {
			return err
		}
		return panelPolicyAudit(ctx, tx, actor, "access_issued", "activation_delivery", &id, panelAccountState(role, active), panelAccountState(role, active))
	})
}

func (s *PanelStore) UpdateAccount(ctx context.Context, actor, id, role string, active bool, reason string) error {
	if !authz.ChangeReason(reason) {
		return ErrPanelDenied
	}
	return s.panelAdminWrite(ctx, actor, func(tx pgx.Tx, a PanelActor) error {
		if actor == id {
			return ErrPanelDenied
		}
		prior, wasActive, err := panelTarget(ctx, tx, a, id)
		if err != nil {
			return err
		}
		if role != "user" && a.Role != "owner" {
			return ErrPanelDenied
		}
		if prior == role && wasActive == active {
			return nil
		}
		if _, err := tx.Exec(ctx, `UPDATE users SET panel_role=$2,panel_active=$3 WHERE id=$1`, id, role, active); err != nil {
			return err
		}
		if _, err := tx.Exec(ctx, `DELETE FROM panel_sessions WHERE user_id=$1`, id); err != nil {
			return err
		}
		if _, err := tx.Exec(ctx, `DELETE FROM panel_access_links WHERE user_id=$1`, id); err != nil {
			return err
		}
		return panelPolicyAudit(ctx, tx, actor, "account_updated", reason, &id, panelAccountState(prior, wasActive), panelAccountState(role, active))
	})
}

func (s *PanelStore) UpdateProfile(ctx context.Context, actor, name string) error {
	return pgx.BeginFunc(ctx, s.pool, func(tx pgx.Tx) error {
		tag, err := tx.Exec(ctx, `UPDATE users SET display_name=$2 WHERE id=$1 AND panel_active`, actor, name)
		if err != nil {
			return err
		}
		if tag.RowsAffected() != 1 {
			return ErrPanelDenied
		}
		return panelAudit(ctx, tx, actor, "profile_updated", nil)
	})
}

func (s *PanelStore) Accounts(ctx context.Context, staff bool, search string, offset int) ([]PanelAccount, error) {
	rows, err := s.pool.Query(ctx, `SELECT `+panelAccountColumns+` FROM users u
		WHERE (u.panel_role <> 'user')=$1 AND (strpos(lower(u.display_name),$2)>0 OR strpos(lower(u.email::text),$2)>0 OR $2='')
		ORDER BY u.created_at DESC,u.id DESC LIMIT 26 OFFSET $3`, staff, strings.ToLower(search), offset)
	if err != nil {
		return nil, err
	}
	defer rows.Close()
	out := []PanelAccount{}
	for rows.Next() {
		var a PanelAccount
		if err := rows.Scan(&a.ID, &a.Email, &a.Name, &a.Role, &a.Active, &a.Primary, &a.CreatedAt, &a.Managed, &a.Enrolled, &a.Pending); err != nil {
			return nil, err
		}
		out = append(out, a)
	}
	return out, rows.Err()
}
