package postgres

import (
	"context"
	"fmt"
	"time"

	"github.com/jackc/pgx/v5"
)

type PanelReport struct {
	ID, Address, VerifyCode string
	Ready, Granted          bool
	SealedAt                time.Time
}

const panelReportFrom = ` FROM reports r
	JOIN inspections i ON i.id=r.inspection_id
	JOIN tenancies t ON t.id=i.tenancy_id JOIN properties p ON p.id=t.property_id
	LEFT JOIN panel_report_access a ON a.report_id=r.id `

func (s *PanelStore) Reports(ctx context.Context, owner string, admin bool, offset int) ([]PanelReport, error) {
	rows, err := s.pool.Query(ctx, `SELECT r.id::text,p.address,r.verify_code,r.pdf_key IS NOT NULL,
		a.report_id IS NOT NULL,i.sealed_at`+panelReportFrom+`
		WHERE i.status='SEALED' AND
		((NOT $2 AND p.owner_user_id=$1) OR ($2 AND a.report_id IS NOT NULL AND r.pdf_key IS NOT NULL))
		ORDER BY i.sealed_at DESC,r.id DESC LIMIT 26 OFFSET $3`, owner, admin, offset)
	if err != nil {
		return nil, err
	}
	return pgx.CollectRows(rows, func(row pgx.CollectableRow) (PanelReport, error) {
		var r PanelReport
		err := row.Scan(&r.ID, &r.Address, &r.VerifyCode, &r.Ready, &r.Granted, &r.SealedAt)
		return r, err
	})
}

func (s *PanelStore) ReportKey(ctx context.Context, actor, id string, staffAccess bool) (string, error) {
	var key string
	err := s.pool.QueryRow(ctx, `SELECT r.pdf_key`+panelReportFrom+`
		WHERE r.id=$1 AND r.pdf_key IS NOT NULL AND i.status='SEALED'
		AND (p.owner_user_id=$2 OR ($3 AND a.report_id IS NOT NULL))`, id, actor, staffAccess).Scan(&key)
	if err != nil {
		return "", fmt.Errorf("resolve authorized PDF: %w", panelMissing(err))
	}
	return key, nil
}

func (s *PanelStore) SetReportAccess(ctx context.Context, owner, id string, grant bool) error {
	return pgx.BeginFunc(ctx, s.pool, func(tx pgx.Tx) error {
		var report string
		err := tx.QueryRow(ctx, `SELECT r.id::text`+panelReportFrom+`
			JOIN users u ON u.id=p.owner_user_id
			WHERE r.id=$1 AND u.id=$2 AND u.panel_active AND r.pdf_key IS NOT NULL AND i.status='SEALED'
			FOR UPDATE OF r`, id, owner).Scan(&report)
		if err != nil {
			return fmt.Errorf("authorize report availability: %w", panelMissing(err))
		}
		if grant {
			_, err = tx.Exec(ctx, `INSERT INTO panel_report_access(report_id) VALUES($1) ON CONFLICT DO NOTHING`, id)
		} else {
			_, err = tx.Exec(ctx, `DELETE FROM panel_report_access WHERE report_id=$1`, id)
		}
		return err
	})
}
