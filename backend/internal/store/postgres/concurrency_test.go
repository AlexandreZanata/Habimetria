package postgres

import (
	"context"
	"fmt"
	"os"
	"sync"
	"testing"
	"time"

	"vault/migrations"
)

// TestConcurrentChainCreation fires parallel property→tenancy→inspection
// chains: every chain must succeed with distinct IDs and no lost update.
// Skips unless TEST_DATABASE_URL/DATABASE_URL is set.
func TestConcurrentChainCreation(t *testing.T) {
	dsn := os.Getenv("TEST_DATABASE_URL")
	if dsn == "" {
		dsn = os.Getenv("DATABASE_URL")
	}
	if dsn == "" {
		t.Skip("TEST_DATABASE_URL/DATABASE_URL unset: db integration unverified")
	}

	ctx, cancel := context.WithTimeout(context.Background(), 60*time.Second)
	defer cancel()

	pool, err := Open(ctx, dsn)
	if err != nil {
		t.Fatalf("open postgres: %v", err)
	}
	defer pool.Close()

	if err := Apply(ctx, pool, migrations.FS); err != nil {
		t.Fatalf("apply migrations: %v", err)
	}

	const workers = 10
	ids := make([]string, workers)
	errs := make([]error, workers)
	var wg sync.WaitGroup
	for w := 0; w < workers; w++ {
		wg.Add(1)
		go func(w int) {
			defer wg.Done()
			addr := fmt.Sprintf("race-test address %d %d", time.Now().UnixNano(), w)
			p, err := CreateProperty(ctx, pool, addr)
			if err != nil {
				errs[w] = err
				return
			}
			ten, err := CreateTenancy(ctx, pool, p.ID)
			if err != nil {
				errs[w] = err
				return
			}
			insp, err := CreateInspection(ctx, pool, ten.ID, "entry")
			if err != nil {
				errs[w] = err
				return
			}
			ids[w] = insp.ID
		}(w)
	}
	wg.Wait()
	seen := map[string]bool{}
	for w := 0; w < workers; w++ {
		if errs[w] != nil {
			t.Fatalf("worker %d: %v", w, errs[w])
		}
		if ids[w] == "" || seen[ids[w]] {
			t.Fatalf("worker %d: missing or duplicate inspection id %q", w, ids[w])
		}
		seen[ids[w]] = true
	}
}
