// Synthetic fixtures shared by Go tests. Every value is deterministic and
// obviously fake: fixed UUID namespace, fixed instants, fixed keys. No real
// personal data, address, photo, location or secret may enter this package.
package testutil

import (
	"fmt"
	"time"
)

// ID returns a deterministic valid UUID in the test namespace.
// Different seeds yield different IDs; the output is stable per seed.
func ID(seed int) string {
	return fmt.Sprintf("123e4567-e89b-12d3-a456-42661417%04d", seed%10000)
}

// TestTime returns the fixed reference instant used across suites.
func TestTime() time.Time {
	ts, err := time.Parse(time.RFC3339, "2026-09-25T10:00:00Z")
	if err != nil {
		panic(err)
	}
	return ts
}

// HMACKey returns the fixed 32-byte test signing key. Test-only.
func HMACKey() string {
	return "test-only-hmac-key-0123456789abc"
}
