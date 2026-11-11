package panel

import (
	"context"

	"vault/internal/authz"
	"vault/internal/store/postgres"

	"github.com/google/uuid"
)

func ValidateID(id string) error {
	if _, err := uuid.Parse(id); err != nil {
		return ErrInput
	}
	return nil
}

func (s *Service) Accounts(ctx context.Context, actor postgres.PanelActor, staff bool, query string, offset int) ([]postgres.PanelAccount, error) {
	if !actor.Can("accounts.read") {
		return nil, postgres.ErrPanelDenied
	}
	return s.store.Accounts(ctx, staff, query, offset)
}

func (s *Service) UpdateAccount(ctx context.Context, actor, id, role string, active bool, reason string) error {
	if ValidateID(id) != nil || !validRole(role) || !authz.ChangeReason(reason) {
		return ErrInput
	}
	return s.store.UpdateAccount(ctx, actor, id, role, active, reason)
}

func (s *Service) UpdateProfile(ctx context.Context, actor, name string) error {
	name, err := cleanText(name, 2, 100)
	if err != nil {
		return err
	}
	return s.store.UpdateProfile(ctx, actor, name)
}

func (s *Service) SetRole(ctx context.Context, actor, role string, permissions []string, reason string) error {
	allowed, valid := authz.Normalize(role, permissions)
	if !valid || !authz.ChangeReason(reason) {
		return ErrInput
	}
	return s.store.SetRole(ctx, actor, role, allowed, reason)
}

func (s *Service) Roles(ctx context.Context, actor postgres.PanelActor) ([]postgres.PanelRole, error) {
	if !actor.Active || actor.Role != "owner" {
		return nil, postgres.ErrPanelDenied
	}
	return s.store.Roles(ctx)
}

func (s *Service) Activity(ctx context.Context, actor postgres.PanelActor, offset int) ([]postgres.PanelEvent, error) {
	if !actor.Can("audit.read") {
		return nil, postgres.ErrPanelDenied
	}
	return s.store.Activity(ctx, offset)
}
