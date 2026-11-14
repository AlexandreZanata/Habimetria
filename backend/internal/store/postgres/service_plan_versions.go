package postgres

import (
	"bytes"
	"context"
	"encoding/json"
	"fmt"
	"math"

	"github.com/jackc/pgx/v5"
)

const servicePlanVersionColumns = `id::text,plan_id::text,version_number,name,billing_period,subject_scope,format_version,allowances,created_at`

func scanServicePlanVersion(row pgx.Row) (ServicePlanVersion, error) {
	var version ServicePlanVersion
	var allowances []byte
	if err := row.Scan(&version.ID, &version.PlanID, &version.Number, &version.Name, &version.BillingPeriod,
		&version.SubjectScope, &version.FormatVersion, &allowances, &version.CreatedAt); err != nil {
		return version, servicePlanError(err)
	}
	decoder := json.NewDecoder(bytes.NewReader(allowances))
	decoder.DisallowUnknownFields()
	if err := decoder.Decode(&version.Allowances); err != nil {
		return ServicePlanVersion{}, fmt.Errorf("decode hosted allowances: %w", err)
	}
	return version, nil
}

func (s *ServicePlanStore) AppendVersion(ctx context.Context, planID string, terms ServicePlanVersionInput) (ServicePlanVersion, error) {
	var version ServicePlanVersion
	allowances, err := json.Marshal(terms.Allowances)
	if err != nil {
		return version, fmt.Errorf("encode hosted allowances: %w", err)
	}
	err = pgx.BeginFunc(ctx, s.pool, func(tx pgx.Tx) error {
		plan, err := scanServicePlan(tx.QueryRow(ctx, `SELECT `+servicePlanColumns+` FROM service_plans WHERE id=$1 FOR UPDATE`, planID))
		if err != nil {
			return err
		}
		if plan.RetiredAt != nil {
			return ErrServicePlanRetired
		}
		var number int64
		if err := tx.QueryRow(ctx, `SELECT COALESCE(MAX(version_number),0)::bigint+1 FROM service_plan_versions WHERE plan_id=$1`, planID).Scan(&number); err != nil {
			return fmt.Errorf("allocate plan version: %w", err)
		}
		if number > math.MaxInt32 {
			return fmt.Errorf("version number exhausted: %w", ErrServicePlanInvalid)
		}
		version, err = scanServicePlanVersion(tx.QueryRow(ctx, `INSERT INTO service_plan_versions
			(plan_id,version_number,name,billing_period,subject_scope,format_version,allowances)
			VALUES($1,$2,$3,$4,$5,$6,$7::jsonb) RETURNING `+servicePlanVersionColumns,
			planID, number, terms.Name, terms.BillingPeriod, terms.SubjectScope, terms.FormatVersion, allowances))
		return err
	})
	if err != nil {
		return ServicePlanVersion{}, fmt.Errorf("append service plan version: %w", err)
	}
	return version, nil
}

func (s *ServicePlanStore) Version(ctx context.Context, planID string, number int) (ServicePlanVersion, error) {
	version, err := scanServicePlanVersion(s.pool.QueryRow(ctx, `SELECT `+servicePlanVersionColumns+` FROM service_plan_versions WHERE plan_id=$1 AND version_number=$2`, planID, number))
	if err != nil {
		return ServicePlanVersion{}, fmt.Errorf("read service plan version: %w", err)
	}
	return version, nil
}

// Bounded keyset pagination retains historical versions after retirement.
func (s *ServicePlanStore) Versions(ctx context.Context, planID string, after, limit int) ([]ServicePlanVersion, error) {
	rows, err := s.pool.Query(ctx, `SELECT `+servicePlanVersionColumns+` FROM service_plan_versions
		WHERE plan_id=$1 AND version_number>$2 ORDER BY version_number LIMIT $3`, planID, after, limit)
	if err != nil {
		return nil, fmt.Errorf("list service plan versions: %w", err)
	}
	defer rows.Close()
	versions := []ServicePlanVersion{}
	for rows.Next() {
		version, err := scanServicePlanVersion(rows)
		if err != nil {
			return nil, fmt.Errorf("scan service plan version: %w", err)
		}
		versions = append(versions, version)
	}
	if err := rows.Err(); err != nil {
		return nil, fmt.Errorf("iterate service plan versions: %w", err)
	}
	return versions, nil
}
