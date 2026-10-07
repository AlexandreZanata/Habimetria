package postgres

import (
	"context"
	"os"
	"testing"
	"time"

	"vault/internal/testutil"
	"vault/migrations"
)

// TestEvidenceServerClock proves received_at_server comes from the database
// clock: it must fall inside the test window even when the device claim is
// backdated. Skips unless TEST_DATABASE_URL/DATABASE_URL is set.
func TestEvidenceServerClock(t *testing.T) {
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

	p, err := CreateProperty(ctx, pool, "clock-test address")
	if err != nil {
		t.Fatalf("create property: %v", err)
	}
	ten, err := CreateTenancy(ctx, pool, p.ID)
	if err != nil {
		t.Fatalf("create tenancy: %v", err)
	}
	insp, err := CreateInspection(ctx, pool, ten.ID, "entry")
	if err != nil {
		t.Fatalf("create inspection: %v", err)
	}
	room, err := CreateRoom(ctx, pool, insp.ID, "room", "Room")
	if err != nil {
		t.Fatalf("create room: %v", err)
	}

	backdated := time.Date(2001, 2, 3, 4, 5, 6, 0, time.UTC)
	before := time.Now().UTC()
	ev, err := CreateEvidence(ctx, pool, EvidenceInput{
		ID: testutil.ID(int(time.Now().UnixNano() % 9000)), InspectionID: insp.ID, RoomID: &room.ID,
		Type: "photo", Mime: "image/jpeg",
		Size: 10, StorageKey: "k", SHA256: "ab", CapturedAtDevice: &backdated,
	})
	if err != nil {
		t.Fatalf("create evidence: %v", err)
	}
	after := time.Now().UTC()
	if ev.ReceivedAtServer.Before(before.Add(-time.Minute)) || ev.ReceivedAtServer.After(after.Add(time.Minute)) {
		t.Fatalf("received_at_server outside server window: %v", ev.ReceivedAtServer)
	}
	if !ev.ReceivedAtServer.After(backdated.Add(24 * time.Hour)) {
		t.Fatalf("server clock must not follow the backdated device claim: %v", ev.ReceivedAtServer)
	}
	if ev.CapturedAtDevice == nil || !ev.CapturedAtDevice.Equal(backdated) {
		t.Fatalf("device claim must be preserved as-is: %v", ev.CapturedAtDevice)
	}
}
