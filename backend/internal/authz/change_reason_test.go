package authz_test

import (
	"testing"

	"vault/internal/authz"
)

func TestClosedChangeReasons(t *testing.T) {
	for _, reason := range []string{"access_review", "security_response", "account_request"} {
		if !authz.ChangeReason(reason) {
			t.Fatalf("known reason rejected: %s", reason)
		}
	}
	for _, reason := range []string{"", " account_request", "account_request\n", "person@example.test", "account_onboarding"} {
		if authz.ChangeReason(reason) {
			t.Fatal("untrusted reason accepted")
		}
	}
}
