package collectionfixture

import (
	"context"
	"crypto/sha256"
	"io"
	"testing"

	"github.com/jackc/pgx/v5/pgxpool"
)

type Snapshot struct {
	Rows, EvidenceBytes, PDFBytes [32]byte
}

func (f Fixture) Snapshot(t *testing.T, ctx context.Context, pool *pgxpool.Pool, objects Objects) Snapshot {
	t.Helper()
	var rows string
	err := pool.QueryRow(ctx, `SELECT jsonb_build_array(
		(SELECT to_jsonb(p) FROM properties p WHERE id=$1),
		(SELECT jsonb_agg(to_jsonb(t) ORDER BY id) FROM tenancies t WHERE property_id=$1),
		(SELECT jsonb_agg(to_jsonb(i) ORDER BY id) FROM inspections i WHERE tenancy_id=$2),
		(SELECT jsonb_agg(to_jsonb(r) ORDER BY id) FROM rooms r WHERE inspection_id IN ($3,$4)),
		(SELECT jsonb_agg(to_jsonb(e) ORDER BY id) FROM evidences e WHERE inspection_id IN ($3,$4)),
		(SELECT jsonb_agg(to_jsonb(f) ORDER BY id) FROM findings f WHERE room_id IN
		 (SELECT id FROM rooms WHERE inspection_id IN ($3,$4))),
		(SELECT jsonb_agg(to_jsonb(m) ORDER BY inspection_id) FROM manifests m WHERE inspection_id IN ($3,$4)),
		(SELECT jsonb_agg(to_jsonb(r) ORDER BY id) FROM reports r WHERE inspection_id IN ($3,$4)),
		(SELECT jsonb_agg(to_jsonb(o) ORDER BY id) FROM occurrences o WHERE property_id=$1))::text`,
		f.Property, f.Tenancy, f.Inspection, f.Draft).Scan(&rows)
	if err != nil {
		t.Fatal(err)
	}
	read := func(key string) [32]byte {
		t.Helper()
		reader, err := objects.OpenOriginal(ctx, key)
		if err != nil {
			t.Fatal(err)
		}
		bytes, err := io.ReadAll(io.LimitReader(reader, 1<<20))
		closeErr := reader.Close()
		if err != nil || closeErr != nil || string(bytes) != PDF {
			t.Fatalf("synthetic object changed: %v/%v", err, closeErr)
		}
		return sha256.Sum256(bytes)
	}
	return Snapshot{sha256.Sum256([]byte(rows)), read(f.EvidenceKey), read(f.PDFKey)}
}
