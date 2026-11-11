package privacy

func accountFields() []Field {
	return []Field{
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
