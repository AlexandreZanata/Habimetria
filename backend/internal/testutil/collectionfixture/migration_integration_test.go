package collectionfixture_test

import (
	"io/fs"
	"strings"
	"testing"
	"testing/fstest"

	"vault/internal/service"
	"vault/internal/store/postgres"
	"vault/internal/testutil"
	"vault/internal/testutil/collectionfixture"
	"vault/internal/testutil/s3fixture"
	"vault/migrations"
)

func TestMultiOwnerMigrationPreservationIntegration(t *testing.T) {
	objects := s3fixture.Store(t)
	ctx, pool := testutil.Database(t)
	prefix := fstest.MapFS{}
	entries, err := fs.ReadDir(migrations.FS, ".")
	if err != nil {
		t.Fatal(err)
	}
	expected := 0
	for _, entry := range entries {
		if !strings.HasSuffix(entry.Name(), ".sql") {
			continue
		}
		expected++
		if entry.Name() < "0005" {
			data, err := migrations.FS.ReadFile(entry.Name())
			if err != nil {
				t.Fatal(err)
			}
			prefix[entry.Name()] = &fstest.MapFile{Data: data}
		}
	}
	if err := postgres.Apply(ctx, pool, prefix); err != nil {
		t.Fatal(err)
	}
	for i, email := range []string{"migration-alice@example.test", "migration-bob@example.test"} {
		if _, err := pool.Exec(ctx, `INSERT INTO users(id,email) VALUES($1,$2)`, testutil.ID(i+1), email); err != nil {
			t.Fatal(err)
		}
	}
	owners := []string{testutil.ID(1), testutil.ID(2), ""}
	fixtures := []collectionfixture.Fixture{}
	before := []collectionfixture.Snapshot{}
	for i, owner := range owners {
		fixture := collectionfixture.New(t, ctx, pool, objects, owner, []string{"Alice", "Bob", "Unclaimed"}[i])
		fixtures = append(fixtures, fixture)
		before = append(before, fixture.Snapshot(t, ctx, pool, objects))
	}
	failure := fstest.MapFS{"9999_fixture_failure.sql": &fstest.MapFile{Data: []byte(`
		CREATE TABLE fixture_rollback(id INT);
		INSERT INTO properties(address) VALUES('Synthetic rollback marker'); SELECT 1/0;`)}}
	if err := postgres.Apply(ctx, pool, failure); err == nil {
		t.Fatal("failed migration reported success")
	}
	var rolledBack bool
	if err := pool.QueryRow(ctx, `SELECT to_regclass('fixture_rollback') IS NULL
		AND NOT EXISTS(SELECT 1 FROM properties WHERE address='Synthetic rollback marker')
		AND NOT EXISTS(SELECT 1 FROM schema_migrations WHERE filename='9999_fixture_failure.sql')`).Scan(&rolledBack); err != nil || !rolledBack {
		t.Fatalf("migration failure left partial DDL/data/ledger: %v", err)
	}
	for range 2 {
		if err := postgres.Apply(ctx, pool, migrations.FS); err != nil {
			t.Fatal(err)
		}
	}
	var applied, enrolled, grants, plans int
	if err := pool.QueryRow(ctx, `SELECT (SELECT count(*) FROM schema_migrations),
		(SELECT count(*) FROM users WHERE panel_managed OR panel_password IS NOT NULL OR panel_primary OR panel_role<>'user'),
		(SELECT count(*) FROM panel_report_access),(SELECT count(*) FROM service_plans)`).Scan(&applied, &enrolled, &grants, &plans); err != nil {
		t.Fatal(err)
	}
	if applied != expected || enrolled != 0 || grants != 0 || plans != 0 {
		t.Fatal("migration seeded enrollment, Report access or an offer, or replay diverged")
	}
	for i, fixture := range fixtures {
		if after := fixture.Snapshot(t, ctx, pool, objects); after != before[i] {
			t.Fatalf("migration changed core history or private objects for fixture %d", i)
		}
		for scope, id := range map[string]string{"properties": fixture.Property, "tenancies": fixture.Tenancy,
			"inspections": fixture.Inspection, "rooms": fixture.Room, "reports": fixture.Report, "occurrences": fixture.Occurrence} {
			owner, err := postgres.NewPanelStore(pool).ResourceOwner(ctx, scope, id)
			if err != nil || (owners[i] == "" && owner != nil) || (owners[i] != "" && (owner == nil || *owner != owners[i])) {
				t.Fatalf("migration changed owner scope %s: %v", scope, err)
			}
		}
		verified, err := service.NewReport(pool, objects, "http://localhost:8080").Verify(ctx, fixture.VerifyCode, testutil.HMACKey())
		if err != nil || !verified.SignatureValid || verified.ManifestSHA256 == "" || !verified.PDFReady {
			t.Fatalf("migration broke frozen verification: %v", err)
		}
		var separateClocks bool
		if err := pool.QueryRow(ctx, `SELECT captured_at_device=$2 AND received_at_server<>$2 FROM evidences WHERE id=$1`,
			fixture.Evidence, testutil.TestTime()).Scan(&separateClocks); err != nil || !separateClocks {
			t.Fatalf("fixture confused device claim and server instant: %v", err)
		}
	}
	otherCtx, other := testutil.Database(t)
	if err := postgres.Apply(otherCtx, other, migrations.FS); err != nil {
		t.Fatal(err)
	}
	if _, err := other.Exec(otherCtx, `INSERT INTO properties(address) VALUES('Independent fixture database')`); err != nil {
		t.Fatal(err)
	}
	var original, independent int
	if err := pool.QueryRow(ctx, `SELECT count(*) FROM properties`).Scan(&original); err != nil {
		t.Fatal(err)
	}
	if err := other.QueryRow(otherCtx, `SELECT count(*) FROM properties`).Scan(&independent); err != nil {
		t.Fatal(err)
	}
	if original != 3 || independent != 1 {
		t.Fatal("independent fixture databases contaminated each other")
	}
}
