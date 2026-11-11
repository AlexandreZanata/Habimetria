package catalog

import (
	"context"
	"math"

	"vault/internal/store/postgres"
)

// Internal persistence boundary, not an HTTP authorization or purchase grant.
// Catalog mutation exposure must add current staff permissions/audit in C07/C41.
type Store interface {
	CreatePlan(context.Context, string) (postgres.ServicePlan, error)
	Plan(context.Context, string) (postgres.ServicePlan, error)
	AppendVersion(context.Context, string, postgres.ServicePlanVersionInput) (postgres.ServicePlanVersion, error)
	Version(context.Context, string, int) (postgres.ServicePlanVersion, error)
	Versions(context.Context, string, int, int) ([]postgres.ServicePlanVersion, error)
	RetirePlan(context.Context, string) (postgres.ServicePlan, error)
}

type Service struct{ store Store }

func New(store Store) *Service { return &Service{store: store} }

func (s *Service) CreatePlan(ctx context.Context, code string) (postgres.ServicePlan, error) {
	if err := ValidateCode(code); err != nil {
		return postgres.ServicePlan{}, err
	}
	return s.store.CreatePlan(ctx, code)
}

func (s *Service) Plan(ctx context.Context, id string) (postgres.ServicePlan, error) {
	if !validID(id) {
		return postgres.ServicePlan{}, ErrInvalid
	}
	return s.store.Plan(ctx, id)
}

func (s *Service) AppendVersion(ctx context.Context, id string, terms postgres.ServicePlanVersionInput) (postgres.ServicePlanVersion, error) {
	if !validID(id) {
		return postgres.ServicePlanVersion{}, ErrInvalid
	}
	if err := ValidateVersion(terms); err != nil {
		return postgres.ServicePlanVersion{}, err
	}
	return s.store.AppendVersion(ctx, id, terms)
}

func (s *Service) Version(ctx context.Context, id string, number int) (postgres.ServicePlanVersion, error) {
	if !validID(id) || number < 1 || number > math.MaxInt32 {
		return postgres.ServicePlanVersion{}, ErrInvalid
	}
	version, err := s.store.Version(ctx, id, number)
	if err != nil {
		return postgres.ServicePlanVersion{}, err
	}
	if err := ValidateVersion(version.ServicePlanVersionInput); err != nil {
		return postgres.ServicePlanVersion{}, err
	}
	return version, nil
}

func (s *Service) Versions(ctx context.Context, id string, after, limit int) ([]postgres.ServicePlanVersion, error) {
	if !validID(id) || after < 0 || after > math.MaxInt32 || limit < 1 || limit > 100 {
		return nil, ErrInvalid
	}
	if _, err := s.store.Plan(ctx, id); err != nil {
		return nil, err
	}
	versions, err := s.store.Versions(ctx, id, after, limit)
	if err != nil {
		return nil, err
	}
	for _, version := range versions {
		if err := ValidateVersion(version.ServicePlanVersionInput); err != nil {
			return nil, err
		}
	}
	return versions, nil
}

func (s *Service) RetirePlan(ctx context.Context, id string) (postgres.ServicePlan, error) {
	if !validID(id) {
		return postgres.ServicePlan{}, ErrInvalid
	}
	return s.store.RetirePlan(ctx, id)
}
