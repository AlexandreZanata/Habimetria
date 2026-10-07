package postgres

import (
	"context"
	"errors"
	"fmt"
	"os"
	"testing"
	"time"

	"vault/migrations"
)

func TestShareChainIntegration(t *testing.T) {
	// Unique per run so reruns never collide with leftover rows.
	// TestShareChainIntegration walks the property→tenancy→inspection→
	// report→share chain on a real PostgreSQL: orphan inspection/share
	// blocked, duplicate share token rejected, access-log trio recorded.
	// Skips unless TEST_DATABASE_URL/DATABASE_URL is set, so an
	// unconfigured suite never claims DB integration.
	uniq := time.Now().UnixNano()
	token := fmt.Sprintf("chain-token-%d", uniq)
	code := fmt.Sprintf("CHAIN%d", uniq%1000000)
	dsn := os.Getenv("TEST_DATABASE_URL")
	if dsn == "" {
		dsn = os.Getenv("DATABASE_URL")
	}
	if dsn == "" {
		t.Skip("TEST_DATABASE_URL/DATABASE_URL unset: db integration unverified")
	}

	ctx, cancel := context.WithTimeout(context.Background(), 30*time.Second)
	defer cancel()

	pool, err := Open(ctx, dsn)
	if err != nil {
		t.Fatalf("open postgres: %v", err)
	}
	defer pool.Close()

	if err := Apply(ctx, pool, migrations.FS); err != nil {
		t.Fatalf("apply migrations: %v", err)
	}

	p, err := CreateProperty(ctx, pool, "chain-test address")
	if err != nil {
		t.Fatalf("create property: %v", err)
	}
	ten, err := CreateTenancy(ctx, pool, p.ID)
	if err != nil {
		t.Fatalf("create tenancy: %v", err)
	}
	if _, err := CreateInspection(ctx, pool, "123e4567-e89b-12d3-a456-426614174000", "entry"); !errors.Is(err, ErrNotFound) {
		t.Fatalf("orphan inspection must fail with ErrNotFound, got %v", err)
	}
	insp, err := CreateInspection(ctx, pool, ten.ID, "entry")
	if err != nil {
		t.Fatalf("create inspection: %v", err)
	}

	var reportID string
	err = pool.QueryRow(ctx,
		`INSERT INTO reports (inspection_id, verify_code, report_sha256, pdf_key)
		 VALUES ($1, $2, $3, $4) RETURNING id::text`,
		insp.ID, code, "ab", "pdf/chain.pdf",
	).Scan(&reportID)
	if err != nil {
		t.Fatalf("insert report fixture: %v", err)
	}

	if _, err := CreateShare(ctx, pool, "123e4567-e89b-12d3-a456-426614174000", "link", token); !errors.Is(err, ErrNotFound) {
		t.Fatalf("orphan share must fail with ErrNotFound, got %v", err)
	}
	share, err := CreateShare(ctx, pool, reportID, "link", token)
	if err != nil {
		t.Fatalf("create share: %v", err)
	}
	if _, err := CreateShare(ctx, pool, reportID, "link", token); !errors.Is(err, ErrTokenTaken) {
		t.Fatalf("duplicate token must fail with ErrTokenTaken, got %v", err)
	}
	got, err := GetShareByToken(ctx, pool, token)
	if err != nil || got.ID != share.ID {
		t.Fatalf("share roundtrip failed: %+v %v", got, err)
	}
	for _, event := range []string{"created", "accessed", "downloaded"} {
		if err := LogAccess(ctx, pool, share.ID, event, "hash"); err != nil {
			t.Fatalf("log %s: %v", event, err)
		}
	}
	timeline, err := ListShares(ctx, pool, reportID)
	if err != nil {
		t.Fatalf("list shares: %v", err)
	}
	if len(timeline) != 1 || timeline[0].Created != 1 || timeline[0].Accessed != 1 || timeline[0].Downloaded != 1 {
		t.Fatalf("access-log trio not recorded: %+v", timeline)
	}
}
