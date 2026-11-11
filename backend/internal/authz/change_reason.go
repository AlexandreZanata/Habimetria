package authz

// ChangeReason excludes arbitrary operator text from historical audit records.
func ChangeReason(reason string) bool {
	switch reason {
	case "access_review", "security_response", "account_request":
		return true
	default:
		return false
	}
}
