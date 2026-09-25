package service

import (
	"context"

	"vault/internal/store/postgres"

	"github.com/jackc/pgx/v5/pgxpool"
)

// pgReportStore adapts package-level postgres funcs to ReportStore.
type pgReportStore struct {
	pool *pgxpool.Pool
}

func (s pgReportStore) Get(ctx context.Context, reportID string) (postgres.Report, error) {
	return postgres.GetReport(ctx, s.pool, reportID)
}

func (s pgReportStore) GetByCode(ctx context.Context, code string) (postgres.Report, error) {
	return postgres.GetReportByCode(ctx, s.pool, code)
}

func (s pgReportStore) Attach(ctx context.Context, reportID, sha, key string) (postgres.Report, error) {
	return postgres.AttachPDF(ctx, s.pool, reportID, sha, key)
}
