package privacy

func accountFields() []Field {
	return []Field{
		{"account_password_recoveries", "user_id", "identity-link", "bind a customer-owned recovery proof to its User", "operational"},
		{"account_password_recoveries", "token_hash", "capability-digest", "validate a single-use purpose-bound password recovery", "operational"},
		{"account_password_recoveries", "created_at", "temporal", "server creation of a customer recovery window", "operational"},
		{"account_password_recoveries", "expires_at", "temporal", "server deadline for single-use recovery authority", "operational"},
		{"account_mail_outbox", "recipient_digest", "pseudonymized-identifier", "prevent security notice delivery to a changed recipient address", "operational"},
		{"account_access_sessions", "user_id", "identity-link", "resolve short-lived API authority to its current ordinary User", "operational"},
		{"account_access_sessions", "token_hash", "capability-digest", "validate and revoke API-only access without retaining the raw secret", "operational"},
		{"account_access_sessions", "created_at", "temporal", "server issuance of short-lived API authority", "operational"},
		{"account_access_sessions", "expires_at", "temporal", "server deadline for API authority; no device clock claim", "operational"},
		{"users", "registration_started_at", "temporal", "server instant of self-registration; no device claim", "identity"},
		{"users", "email_verified_at", "temporal", "server observation of email proof; never inferred for legacy accounts", "identity"},
		{"account_verifications", "user_id", "identity-link", "bind an expiring verification to its pending User", "operational"},
		{"account_verifications", "token_hash", "capability-digest", "validate a purpose-bound email ownership proof", "operational"},
		{"account_mail_outbox", "user_id", "identity-link", "resolve the verification recipient without copying the address", "operational"},
		{"account_mail_outbox", "token_hash", "capability-digest", "match delivery to the current verification proof", "operational"},
		{"account_mail_outbox", "proof_ciphertext", "encrypted-capability", "deliver a narrowly scoped proof; erase on terminal state", "operational"},
		{"account_registration_limits", "key_hash", "pseudonymized-identifier", "HMAC source/address throttling without raw IP/email", "operational"},
	}
}
