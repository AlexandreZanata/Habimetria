package authz

import "slices"

type Capability struct {
	ID      string
	Support bool
}

var registry = [...]Capability{
	{"accounts.read", true}, {"accounts.write", false},
	{"reports.read", true}, {"audit.read", true},
	{"commerce.read", true}, {"catalog.write", false},
	{"pricing.review", false}, {"pricing.publish", false},
	{"grants.write", false}, {"finance.read", false},
	{"refunds.request", false}, {"events.replay", false},
	{"support.write", true}, {"privacy.process", false},
	{"operations.write", false},
}

func Registry() []Capability { return slices.Clone(registry[:]) }

func Assignable(role, id string) bool {
	for _, capability := range registry {
		if capability.ID == id {
			return role == "admin" || (role == "support" && capability.Support)
		}
	}
	return false
}

// Normalize validates the complete grant, rejecting duplicates and dependencies.
func Normalize(role string, permissions []string) ([]string, bool) {
	if role != "admin" && role != "support" {
		return nil, false
	}
	for i, permission := range permissions {
		if !Assignable(role, permission) || slices.Contains(permissions[:i], permission) {
			return nil, false
		}
	}
	if slices.Contains(permissions, "accounts.write") && !slices.Contains(permissions, "accounts.read") {
		return nil, false
	}
	out := make([]string, 0, len(permissions))
	for _, capability := range registry {
		if slices.Contains(permissions, capability.ID) {
			out = append(out, capability.ID)
		}
	}
	return out, true
}

func Can(active bool, role string, permissions []string, id string) bool {
	if !active || !Assignable("admin", id) {
		return false
	}
	if role == "owner" {
		return true
	}
	_, valid := Normalize(role, permissions)
	return valid && slices.Contains(permissions, id)
}
