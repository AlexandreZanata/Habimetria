package postgres

import (
	"context"
	"errors"
	"fmt"

	"github.com/jackc/pgx/v5"
	"github.com/jackc/pgx/v5/pgconn"
	"github.com/jackc/pgx/v5/pgxpool"
)

type ServicePlanStore struct{ pool *pgxpool.Pool }

func NewServicePlanStore(pool *pgxpool.Pool) *ServicePlanStore { return &ServicePlanStore{pool: pool} }

const servicePlanColumns = `id::text,code,created_at,retired_at`

func scanServicePlan(row pgx.Row) (ServicePlan, error) {
	var plan ServicePlan
	err := row.Scan(&plan.ID, &plan.Code, &plan.CreatedAt, &plan.RetiredAt)
	return plan, servicePlanError(err)
}

func servicePlanError(err error) error {
	if errors.Is(err, pgx.ErrNoRows) {
		return ErrNotFound
	}
	var databaseError *pgconn.PgError
	if errors.As(err, &databaseError) {
		switch databaseError.Code {
		case "23505":
			return fmt.Errorf("catalog uniqueness: %w", ErrServicePlanConflict)
		case "23514", "22003":
			return fmt.Errorf("catalog constraints: %w", ErrServicePlanInvalid)
		}
	}
	return err
}

func (s *ServicePlanStore) CreatePlan(ctx context.Context, code string) (ServicePlan, error) {
	plan, err := scanServicePlan(s.pool.QueryRow(ctx, `INSERT INTO service_plans(code) VALUES($1) RETURNING `+servicePlanColumns, code))
	if err != nil {
		return ServicePlan{}, fmt.Errorf("create service plan: %w", err)
	}
	return plan, nil
}

func (s *ServicePlanStore) Plan(ctx context.Context, id string) (ServicePlan, error) {
	plan, err := scanServicePlan(s.pool.QueryRow(ctx, `SELECT `+servicePlanColumns+` FROM service_plans WHERE id=$1`, id))
	if err != nil {
		return ServicePlan{}, fmt.Errorf("read service plan: %w", err)
	}
	return plan, nil
}

func (s *ServicePlanStore) RetirePlan(ctx context.Context, id string) (ServicePlan, error) {
	var plan ServicePlan
	err := pgx.BeginFunc(ctx, s.pool, func(tx pgx.Tx) error {
		var err error
		plan, err = scanServicePlan(tx.QueryRow(ctx, `SELECT `+servicePlanColumns+` FROM service_plans WHERE id=$1 FOR UPDATE`, id))
		if err != nil {
			return err
		}
		if plan.RetiredAt != nil {
			return nil
		}
		plan, err = scanServicePlan(tx.QueryRow(ctx, `UPDATE service_plans SET retired_at=now() WHERE id=$1 RETURNING `+servicePlanColumns, id))
		return err
	})
	if err != nil {
		return ServicePlan{}, fmt.Errorf("retire service plan: %w", err)
	}
	return plan, nil
}
