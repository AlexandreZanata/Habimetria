package postgres

import (
	"context"
	"fmt"

	"github.com/jackc/pgx/v5"
)

// Fixed fields deliberately exclude names, credentials, free text and artifacts.
type PanelPolicyState struct {
	Role        *string
	Active      *bool
	Permissions []string
}

func panelAccountState(role string, active bool) PanelPolicyState {
	return PanelPolicyState{Role: &role, Active: &active}
}

func panelRoleState(role string, permissions []string) PanelPolicyState {
	return PanelPolicyState{Role: &role, Permissions: permissions}
}

func panelPolicyAudit(ctx context.Context, tx pgx.Tx, actor, event, reason string, subject *string, before, after PanelPolicyState) error {
	_, err := tx.Exec(ctx, `INSERT INTO panel_audit(actor_id,event,subject_id,operation_id,reason_code,
		before_role,after_role,before_active,after_active,before_permissions,after_permissions,occurred_at)
		VALUES($1,$2,$3,gen_random_uuid(),$4,$5,$6,$7,$8,$9,$10,clock_timestamp())`,
		actor, event, subject, reason, before.Role, after.Role, before.Active, after.Active, before.Permissions, after.Permissions)
	if err != nil {
		return fmt.Errorf("write policy audit: %w", err)
	}
	return nil
}
