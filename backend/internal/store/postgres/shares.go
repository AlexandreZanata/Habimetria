package postgres

import (
	"context"
	"errors"
	"fmt"
	"time"

	"github.com/jackc/pgx/v5"
	"github.com/jackc/pgx/v5/pgconn"
	"github.com/jackc/pgx/v5/pgxpool"
)

// Share is a distributable link to a sealed report.
type Share struct {
	ID        string    `json:"id"`
	ReportID  string    `json:"report_id"`
	Channel   string    `json:"channel"`
	Token     string    `json:"token"`
	CreatedAt time.Time `json:"created_at"`
}

// ShareTimeline is a share plus its access event counts.
type ShareTimeline struct {
	Share
	Created    int `json:"created"`
	Accessed   int `json:"accessed"`
	Downloaded int `json:"downloaded"`
}

// ErrTokenTaken signals a share token collision. Callers regenerate.
var ErrTokenTaken = errors.New("share token collision")

// CreateShare inserts a share with a caller-supplied unique token.
func CreateShare(ctx context.Context, pool *pgxpool.Pool, reportID, channel, token string) (Share, error) {
	var s Share
	err := pool.QueryRow(ctx,
		`INSERT INTO shares (report_id, channel, token) VALUES ($1,$2,$3)
		 RETURNING id::text, report_id::text, channel, token, created_at`,
		reportID, channel, token,
	).Scan(&s.ID, &s.ReportID, &s.Channel, &s.Token, &s.CreatedAt)
	if err != nil {
		var pgErr *pgconn.PgError
		if errors.As(err, &pgErr) {
			switch pgErr.Code {
			case "23503":
				return Share{}, fmt.Errorf("report %s: %w", reportID, ErrNotFound)
			case "23505":
				return Share{}, fmt.Errorf("token %s: %w", token, ErrTokenTaken)
			}
		}
		return Share{}, fmt.Errorf("insert share: %w", err)
	}
	return s, nil
}

// GetShareByToken resolves a token to its full share row.
func GetShareByToken(ctx context.Context, pool *pgxpool.Pool, token string) (Share, error) {
	var s Share
	err := pool.QueryRow(ctx,
		`SELECT id::text, report_id::text, channel, token, created_at
		 FROM shares WHERE token=$1`,
		token,
	).Scan(&s.ID, &s.ReportID, &s.Channel, &s.Token, &s.CreatedAt)
	if err != nil {
		if errors.Is(err, pgx.ErrNoRows) {
			return Share{}, fmt.Errorf("share %s: %w", token, ErrNotFound)
		}
		return Share{}, fmt.Errorf("resolve share: %w", err)
	}
	return s, nil
}

// LogAccess appends created|accessed|downloaded events with a hashed IP.
func LogAccess(ctx context.Context, pool *pgxpool.Pool, shareID, event, ipHash string) error {
	_, err := pool.Exec(ctx,
		`INSERT INTO access_logs (share_id, event, ip_hash) VALUES ($1,$2,$3)`,
		shareID, event, ipHash,
	)
	if err != nil {
		return fmt.Errorf("log access: %w", err)
	}
	return nil
}

// ListShares returns shares of a report with event counts, newest first.
func ListShares(ctx context.Context, pool *pgxpool.Pool, reportID string) ([]ShareTimeline, error) {
	rows, err := pool.Query(ctx,
		`SELECT s.id::text, s.report_id::text, s.channel, s.token, s.created_at,
		        COUNT(l.id) FILTER (WHERE l.event='created'),
		        COUNT(l.id) FILTER (WHERE l.event='accessed'),
		        COUNT(l.id) FILTER (WHERE l.event='downloaded')
		 FROM shares s LEFT JOIN access_logs l ON l.share_id = s.id
		 WHERE s.report_id=$1
		 GROUP BY s.id, s.report_id, s.channel, s.token, s.created_at
		 ORDER BY s.created_at DESC`,
		reportID,
	)
	if err != nil {
		return nil, fmt.Errorf("list shares: %w", err)
	}
	defer rows.Close()
	out := []ShareTimeline{}
	for rows.Next() {
		var t ShareTimeline
		if err := rows.Scan(&t.ID, &t.ReportID, &t.Channel, &t.Token, &t.CreatedAt,
			&t.Created, &t.Accessed, &t.Downloaded); err != nil {
			return nil, fmt.Errorf("scan share: %w", err)
		}
		out = append(out, t)
	}
	if err := rows.Err(); err != nil {
		return nil, fmt.Errorf("iterate shares: %w", err)
	}
	return out, nil
}
