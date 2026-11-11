package privacy

func deviceFields() []Field {
	return []Field{
		{"account_device_sessions", "user_id", "identity-link", "bind a native family to its own User", "operational"},
		{"account_device_sessions", "device_id", "pseudonymized-identifier", "random installation claim; not hardware attestation", "operational"},
		{"account_device_sessions", "label", "free-text", "recognize own device; must not include private collection data", "operational"},
		{"account_device_sessions", "created_at", "temporal", "server issuance of native authority", "operational"},
		{"account_device_sessions", "last_used_at", "temporal", "server observation of refresh activity", "operational"},
		{"account_device_sessions", "expires_at", "temporal", "absolute server refresh deadline", "operational"},
		{"account_device_sessions", "revoked_at", "temporal", "server revocation of native authority", "operational"},
		{"account_refresh_tokens", "session_id", "identity-link", "scope current/spent refresh to one native family", "operational"},
		{"account_refresh_tokens", "token_hash", "capability-digest", "detect single-use refresh replay without raw secrets", "operational"},
		{"account_refresh_tokens", "created_at", "temporal", "server issuance of refresh generation", "operational"},
		{"account_refresh_tokens", "used_at", "temporal", "server observation of consumed refresh", "operational"},
		{"account_access_sessions", "family_id", "identity-link", "revoke API access with its native family", "operational"},
	}
}
