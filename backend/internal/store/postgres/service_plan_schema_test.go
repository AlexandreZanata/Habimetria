package postgres_test

import (
	"io/fs"
	"strings"
	"testing"
	"testing/fstest"

	"vault/internal/store/postgres"
	"vault/internal/testutil"
	"vault/migrations"
)

const fixtureAllowance = `[{"capability":"cloud_backup","unit":"bytes","window":"retained","limit":1024}]`

func TestServicePlanMigrationIntegration(t *testing.T) {
	ctx, pool := testutil.Database(t)
	prefix := fstest.MapFS{}
	entries, err := fs.ReadDir(migrations.FS, ".")
	if err != nil {
		t.Fatal(err)
	}
	for _, entry := range entries {
		if !strings.HasSuffix(entry.Name(), ".sql") || entry.Name() >= "0006" {
			continue
		}
		bytes, err := migrations.FS.ReadFile(entry.Name())
		if err != nil {
			t.Fatal(err)
		}
		prefix[entry.Name()] = &fstest.MapFile{Data: bytes}
	}
	if err := postgres.Apply(ctx, pool, prefix); err != nil {
		t.Fatal(err)
	}
	var property string
	if err := pool.QueryRow(ctx, `INSERT INTO properties(address) VALUES('Synthetic pre-catalog Property') RETURNING id::text`).Scan(&property); err != nil {
		t.Fatal(err)
	}
	for range 2 {
		if err := postgres.Apply(ctx, pool, migrations.FS); err != nil {
			t.Fatal(err)
		}
	}
	var address string
	var owner *string
	var plans, versions, applied int
	if err := pool.QueryRow(ctx, `SELECT address,owner_user_id::text FROM properties WHERE id=$1`, property).Scan(&address, &owner); err != nil {
		t.Fatal(err)
	}
	if err := pool.QueryRow(ctx, `SELECT (SELECT count(*) FROM service_plans), (SELECT count(*) FROM service_plan_versions), (SELECT count(*) FROM schema_migrations)`).Scan(&plans, &versions, &applied); err != nil {
		t.Fatal(err)
	}
	if address != "Synthetic pre-catalog Property" || owner != nil || plans != 0 || versions != 0 || applied != 6 {
		t.Fatal("migration changed existing data, seeded an offer or failed replay")
	}
}

func TestServicePlanConstraintsIntegration(t *testing.T) {
	ctx, pool := testutil.Database(t)
	if err := postgres.Apply(ctx, pool, migrations.FS); err != nil {
		t.Fatal(err)
	}
	var plan string
	if err := pool.QueryRow(ctx, `INSERT INTO service_plans(code) VALUES('fixture-plan') RETURNING id::text`).Scan(&plan); err != nil {
		t.Fatal(err)
	}
	insert := func(name, period, scope string, format int, allowances string) error {
		_, err := pool.Exec(ctx, `INSERT INTO service_plan_versions(plan_id,version_number,name,billing_period,subject_scope,format_version,allowances)
			VALUES($1,1,$2,$3,$4,$5,$6::jsonb)`, plan, name, period, scope, format, allowances)
		return err
	}
	for _, value := range []string{
		`null`, `{}`, `[]`, `[null]`, `[{}]`,
		strings.Replace(fixtureAllowance, "cloud_backup", "local_pdf", 1),
		strings.Replace(fixtureAllowance, "cloud_backup", "account_recovery", 1),
		strings.Replace(fixtureAllowance, "bytes", "GB", 1),
		strings.Replace(fixtureAllowance, "retained", "forever", 1),
		strings.Replace(fixtureAllowance, "1024", "null", 1),
		strings.Replace(fixtureAllowance, "1024", "0", 1),
		strings.Replace(fixtureAllowance, "1024", "-1", 1),
		strings.Replace(fixtureAllowance, "1024", "1.5", 1),
		strings.Replace(fixtureAllowance, "1024", "1.0", 1),
		strings.Replace(fixtureAllowance, "1024", `"unlimited"`, 1),
		strings.Replace(fixtureAllowance, "1024", "9223372036854775808", 1),
		strings.TrimSuffix(fixtureAllowance, "]") + "," + strings.TrimPrefix(fixtureAllowance, "["),
		strings.Replace(fixtureAllowance, `"limit":1024`, `"limit":1024,"local_pdf":true`, 1),
		strings.Replace(fixtureAllowance, "cloud_backup", strings.Repeat("x", 8193), 1),
	} {
		if err := insert("Fixture", "monthly", "individual", 1, value); err == nil {
			t.Fatalf("accepted invalid allowances: %.160s", value)
		}
	}
	for _, tc := range []struct {
		name, period, scope string
		format              int
	}{
		{"Fixture", "annual", "individual", 1}, {"Fixture", "monthly", "organization", 1},
		{"Fixture", "monthly", "individual", 2}, {"  Fixture", "monthly", "individual", 1},
		{"Fi\nxture", "monthly", "individual", 1}, {"x", "monthly", "individual", 1},
	} {
		if err := insert(tc.name, tc.period, tc.scope, tc.format, fixtureAllowance); err == nil {
			t.Fatalf("accepted invalid version metadata: %+v", tc)
		}
	}
	if err := insert("Fixture", "monthly", "individual", 1, fixtureAllowance); err != nil {
		t.Fatal(err)
	}
	if err := insert("Duplicate", "monthly", "individual", 1, fixtureAllowance); err == nil {
		t.Fatal("duplicate plan version accepted")
	}
	for _, code := range []string{"x", "UPPER", " leading", "local paid plan", strings.Repeat("x", 65)} {
		if _, err := pool.Exec(ctx, `INSERT INTO service_plans(code) VALUES($1)`, code); err == nil {
			t.Fatalf("invalid stable code accepted: %q", code)
		}
	}
}

func TestServicePlanHistoryImmutabilityIntegration(t *testing.T) {
	ctx, pool := testutil.Database(t)
	if err := postgres.Apply(ctx, pool, migrations.FS); err != nil {
		t.Fatal(err)
	}
	var plan, version string
	if err := pool.QueryRow(ctx, `INSERT INTO service_plans(code) VALUES('immutable-fixture') RETURNING id::text`).Scan(&plan); err != nil {
		t.Fatal(err)
	}
	if err := pool.QueryRow(ctx, `INSERT INTO service_plan_versions(plan_id,version_number,name,billing_period,subject_scope,format_version,allowances)
		VALUES($1,1,'Fixture','monthly','individual',1,$2::jsonb) RETURNING id::text`, plan, fixtureAllowance).Scan(&version); err != nil {
		t.Fatal(err)
	}
	var before, after string
	if err := pool.QueryRow(ctx, `SELECT row_to_json(v)::text FROM service_plan_versions v WHERE id=$1`, version).Scan(&before); err != nil {
		t.Fatal(err)
	}
	for _, sql := range []string{
		`UPDATE service_plan_versions SET name='Changed'`, `UPDATE service_plan_versions SET version_number=2`,
		`DELETE FROM service_plan_versions`, `TRUNCATE service_plan_versions`,
		`UPDATE service_plans SET code='replaced-fixture'`, `DELETE FROM service_plans`, `TRUNCATE service_plans CASCADE`,
	} {
		if _, err := pool.Exec(ctx, sql); err == nil {
			t.Fatalf("mutable catalog history: %s", sql)
		}
	}
	if _, err := pool.Exec(ctx, `UPDATE service_plans SET retired_at=now() WHERE id=$1`, plan); err != nil {
		t.Fatal(err)
	}
	if _, err := pool.Exec(ctx, `UPDATE service_plans SET retired_at=NULL WHERE id=$1`, plan); err == nil {
		t.Fatal("retirement reopened")
	}
	if _, err := pool.Exec(ctx, `INSERT INTO service_plan_versions(plan_id,version_number,name,billing_period,subject_scope,format_version,allowances)
		VALUES($1,2,'Later','monthly','individual',1,$2::jsonb)`, plan, fixtureAllowance); err == nil {
		t.Fatal("retired plan accepted new terms")
	}
	if err := pool.QueryRow(ctx, `SELECT row_to_json(v)::text FROM service_plan_versions v WHERE id=$1`, version).Scan(&after); err != nil {
		t.Fatal(err)
	}
	if before != after {
		t.Fatal("retirement/mutation rewrote version history")
	}
}
