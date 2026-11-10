package service

import (
	"fmt"
	"testing"
	"time"

	"vault/internal/store/postgres"
)

func thousandEvidences() []postgres.SealEvidence {
	out := make([]postgres.SealEvidence, 0, 1000)
	for i := 0; i < 1000; i++ {
		out = append(out, postgres.SealEvidence{
			Sequence: i + 1, EvidenceID: fmt.Sprintf("ev-%d", i),
			SHA256: "aa", Mime: "image/jpeg", Size: 100,
		})
	}
	return out
}

// Measures manifest build at municipal scale. Reported, never gated;
// see TestBuildManifest1000Budget for the gate.
func BenchmarkBuildManifest1000(b *testing.B) {
	sealedAt := time.Date(2026, 9, 25, 12, 0, 0, 0, time.UTC)
	evidences := thousandEvidences()
	b.ResetTimer()
	for i := 0; i < b.N; i++ {
		if _, err := BuildManifest("ins-1", sealedAt, evidences); err != nil {
			b.Fatal(err)
		}
	}
}

// Budget gate: 1000-evidence manifest must build well under the 500 ms
// CI budget. Generous on purpose: it catches 10x regressions, not noise.
func TestBuildManifest1000Budget(t *testing.T) {
	sealedAt := time.Date(2026, 9, 25, 12, 0, 0, 0, time.UTC)
	start := time.Now()
	body, err := BuildManifest("ins-1", sealedAt, thousandEvidences())
	if err != nil {
		t.Fatal(err)
	}
	if elapsed := time.Since(start); elapsed > 500*time.Millisecond {
		t.Fatalf("manifest 1000ev took %v, budget 500ms", elapsed)
	}
	if len(body) == 0 {
		t.Fatal("empty manifest")
	}
}
