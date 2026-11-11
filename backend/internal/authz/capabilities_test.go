package authz_test

import (
	"slices"
	"testing"

	"vault/internal/authz"
)

func TestRegistryAuthority(t *testing.T) {
	seen := map[string]bool{}
	for _, capability := range authz.Registry() {
		id := capability.ID
		if seen[id] || id == "" {
			t.Fatal("duplicate/empty capability")
		}
		seen[id] = true
		grant := []string{id}
		if id == "accounts.write" {
			grant = append(grant, "accounts.read")
		}
		for _, role := range []string{"owner", "admin", "support", "user", "unknown"} {
			want := role == "owner" || role == "admin" || (role == "support" && capability.Support)
			if authz.Can(true, role, grant, id) != want || authz.Can(false, role, grant, id) {
				t.Fatalf("incorrect authority for %s/%s", role, id)
			}
			if role != "owner" && authz.Can(true, role, nil, id) {
				t.Fatalf("implicit grant for %s/%s", role, id)
			}
			if authz.Can(true, role, grant, "invented.write") {
				t.Fatal("unknown capability granted")
			}
		}
		if id != "reports.read" && authz.Can(true, "admin", grant, "reports.read") {
			t.Fatalf("%s implies Report access", id)
		}
	}
	copy := authz.Registry()
	copy[0].ID = "replaced"
	if authz.Registry()[0].ID == "replaced" {
		t.Fatal("mutable registry leaked")
	}
}

func TestCompleteGrantValidation(t *testing.T) {
	for _, tc := range []struct {
		role string
		ids  []string
	}{
		{"user", nil}, {"owner", nil}, {"admin", []string{"unknown"}},
		{"admin", []string{"accounts.write"}}, {"support", []string{"accounts.read", "accounts.write"}},
		{"admin", []string{"commerce.read", "commerce.read"}}, {"support", []string{"events.replay"}},
	} {
		if _, ok := authz.Normalize(tc.role, tc.ids); ok {
			t.Fatalf("accepted invalid grant: %+v", tc)
		}
	}
	in := []string{"support.write", "commerce.read"}
	out, ok := authz.Normalize("support", in)
	if !ok || !slices.Equal(out, []string{"commerce.read", "support.write"}) {
		t.Fatal("valid grant not normalized")
	}
	out[0] = "changed"
	if in[0] != "support.write" {
		t.Fatal("normalization aliases input")
	}
	if out, ok := authz.Normalize("admin", nil); !ok || out == nil || len(out) != 0 {
		t.Fatal("empty explicit grant rejected")
	}
}
