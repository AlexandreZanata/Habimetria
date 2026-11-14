package postgres_test

import (
	"context"
	"errors"
	"testing"
	"time"

	"vault/internal/service"
	"vault/internal/store/postgres"
	"vault/internal/testutil"
	"vault/migrations"

	"github.com/jackc/pgx/v5/pgxpool"
)

func roomInspection(t *testing.T) (context.Context, *pgxpool.Pool, string) {
	t.Helper()
	ctx, pool := testutil.Database(t)
	if err := postgres.Apply(ctx, pool, migrations.FS); err != nil {
		t.Fatal(err)
	}
	var inspection string
	if err := pool.QueryRow(ctx, `WITH p AS (INSERT INTO properties(address) VALUES('Synthetic Room guard') RETURNING id),
		t AS (INSERT INTO tenancies(property_id) SELECT id FROM p RETURNING id)
		INSERT INTO inspections(tenancy_id,kind) SELECT id,'entry' FROM t RETURNING id::text`).Scan(&inspection); err != nil {
		t.Fatal(err)
	}
	return ctx, pool, inspection
}

func TestRoomWritesObserveConcurrentSealIntegration(t *testing.T) {
	for _, template := range []bool{false, true} {
		t.Run(map[bool]string{false: "single", true: "template"}[template], func(t *testing.T) {
			ctx, pool, inspection := roomInspection(t)
			if _, err := pool.Exec(ctx, `INSERT INTO evidences(inspection_id,type,mime,size,storage_key,sha256,sequence)
				VALUES($1,'doc','application/pdf',1,'synthetic-room-guard','synthetic-digest',1)`, inspection); err != nil {
				t.Fatal(err)
			}
			block, err := pool.Begin(ctx)
			if err != nil {
				t.Fatal(err)
			}
			defer block.Rollback(ctx)
			if _, err := block.Exec(ctx, `SELECT id FROM inspections WHERE id=$1 FOR UPDATE`, inspection); err != nil {
				t.Fatal(err)
			}
			wait := func(want int) {
				t.Helper()
				deadline := time.Now().Add(5 * time.Second)
				for time.Now().Before(deadline) {
					var waiting int
					if err := pool.QueryRow(ctx, `SELECT count(*) FROM pg_stat_activity WHERE datname=current_database()
						AND wait_event_type='Lock' AND query LIKE 'SELECT status FROM inspections%'`).Scan(&waiting); err != nil {
						t.Fatal(err)
					}
					if waiting == want {
						return
					}
					time.Sleep(10 * time.Millisecond)
				}
				t.Fatal("Room/seal writes did not enter the expected lock queue")
			}
			sealed, written := make(chan error, 1), make(chan error, 1)
			go func() {
				_, err := service.NewSeal(pool, testutil.HMACKey()).Seal(ctx, service.SealInput{InspectionID: inspection})
				sealed <- err
			}()
			wait(1)
			go func() {
				if template {
					_, err := postgres.EnsureTemplate(ctx, pool, inspection)
					written <- err
				} else {
					_, err := postgres.CreateRoom(ctx, pool, inspection, "extra", "Synthetic extra")
					written <- err
				}
			}()
			wait(2)
			if err := block.Commit(ctx); err != nil {
				t.Fatal(err)
			}
			if err := <-sealed; err != nil {
				t.Fatal(err)
			}
			if err := <-written; !errors.Is(err, postgres.ErrSealed) {
				t.Fatalf("queued Room write escaped terminal seal: %v", err)
			}
			var count int
			if err := pool.QueryRow(ctx, `SELECT count(*) FROM rooms WHERE inspection_id=$1`, inspection).Scan(&count); err != nil || count != 0 {
				t.Fatalf("sealed Room inventory changed: %v", err)
			}
		})
	}
}

func TestDraftRoomTemplateAtomicityIntegration(t *testing.T) {
	ctx, pool, inspection := roomInspection(t)
	if _, err := pool.Exec(ctx, `CREATE FUNCTION fixture_template_failure() RETURNS TRIGGER LANGUAGE plpgsql AS $$
		BEGIN IF NEW.key='bedroom' THEN RAISE EXCEPTION 'synthetic template failure'; END IF; RETURN NEW; END $$;
		CREATE TRIGGER fixture_template_failure BEFORE INSERT ON rooms FOR EACH ROW EXECUTE FUNCTION fixture_template_failure()`); err != nil {
		t.Fatal(err)
	}
	if _, err := postgres.EnsureTemplate(ctx, pool, inspection); err == nil {
		t.Fatal("template insertion failure swallowed")
	}
	rooms, err := postgres.ListRooms(ctx, pool, inspection)
	if err != nil || len(rooms) != 0 {
		t.Fatalf("template failure left partial Rooms: %v", err)
	}
	if _, err := pool.Exec(ctx, `DROP TRIGGER fixture_template_failure ON rooms`); err != nil {
		t.Fatal(err)
	}
	if _, err := postgres.CreateRoom(ctx, pool, inspection, "living", "Synthetic named room"); err != nil {
		t.Fatal(err)
	}
	if _, err := postgres.CreateRoom(ctx, pool, inspection, "living", "Duplicate"); !errors.Is(err, postgres.ErrRoomExists) {
		t.Fatalf("duplicate Room contract changed: %v", err)
	}
	for range 2 {
		rooms, err := postgres.EnsureTemplate(ctx, pool, inspection)
		if err != nil || len(rooms) != len(postgres.DefaultRooms) {
			t.Fatalf("DRAFT template/idempotency failed: %v", err)
		}
	}
	if _, err := postgres.CreateRoom(ctx, pool, testutil.ID(9999), "absent", "Absent"); !errors.Is(err, postgres.ErrNotFound) {
		t.Fatalf("missing Room parent contract changed: %v", err)
	}
}
