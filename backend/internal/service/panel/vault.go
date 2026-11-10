package panel

import (
	"context"

	"vault/internal/store/postgres"
)

func (s *Service) Overview(ctx context.Context, a postgres.PanelActor, admin bool) (postgres.PanelMetrics, error) {
	if admin && a.Role == "user" {
		return postgres.PanelMetrics{}, postgres.ErrPanelDenied
	}
	return s.store.Metrics(ctx, a, admin)
}

func (s *Service) Properties(ctx context.Context, a postgres.PanelActor, offset int) ([]postgres.Property, error) {
	return s.store.Properties(ctx, a.ID, offset)
}

func (s *Service) CreateProperty(ctx context.Context, a postgres.PanelActor, address string) error {
	address, err := cleanText(address, 3, 400)
	if err != nil {
		return err
	}
	return s.store.CreateOwnedProperty(ctx, a.ID, address)
}

func (s *Service) Inspections(ctx context.Context, a postgres.PanelActor, offset int) ([]postgres.PanelInspection, error) {
	return s.store.Inspections(ctx, a.ID, offset)
}

func (s *Service) CreateInspection(ctx context.Context, a postgres.PanelActor, property string) error {
	if ValidateID(property) != nil {
		return ErrInput
	}
	return s.store.CreateOwnedInspection(ctx, a.ID, property)
}

func (s *Service) Reports(ctx context.Context, a postgres.PanelActor, admin bool, offset int) ([]postgres.PanelReport, error) {
	if admin && !a.Can("reports.read") {
		return nil, postgres.ErrPanelDenied
	}
	return s.store.Reports(ctx, a.ID, admin, offset)
}

func (s *Service) ReportKey(ctx context.Context, a postgres.PanelActor, id string) (string, error) {
	if ValidateID(id) != nil {
		return "", ErrInput
	}
	return s.store.ReportKey(ctx, a.ID, id, a.Can("reports.read"))
}

func (s *Service) SetReportAccess(ctx context.Context, a postgres.PanelActor, id string, grant bool) error {
	if ValidateID(id) != nil {
		return ErrInput
	}
	return s.store.SetReportAccess(ctx, a.ID, id, grant)
}

func (s *Service) ResourceOwner(ctx context.Context, scope, id string) (*string, error) {
	if ValidateID(id) != nil {
		return nil, ErrInput
	}
	return s.store.ResourceOwner(ctx, scope, id)
}
