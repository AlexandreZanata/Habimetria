// Package privacy holds the LGPD processing inventory: every table
// column that carries personal data (or points at it), with its purpose
// and retention class, plus the retention class of every table. Tests
// enforce the inventory against the live schema, so a new personal-data
// column fails CI until it is registered here with a purpose. Legal bases,
// periods and the RIPD stay counsel-owned and are never claimed by code.
package privacy

// Field is one registered personal-data location.
type Field struct {
	Table     string
	Column    string
	Category  string
	Purpose   string
	Retention string
}

// Registry lists personal-data columns. Free-text and image-locator rows
// are included with handling rules, not just direct identifiers.
func Registry() []Field {
	return append(accountFields(), []Field{
		{"service_plans", "code", "catalog-identifier", "stable offer identifier; must not hold customer identity", "system"},
		{"service_plan_versions", "name", "catalog-label", "monthly offer display name; must not hold personal names", "system"},
		{"users", "email", "direct-identifier", "account identity for sign-in and access management", "identity"},
		{"users", "display_name", "direct-identifier", "account profile and staff attribution", "identity"},
		{"users", "panel_password", "credential-digest", "verify the account password with a unique salt", "identity"},
		{"panel_access_links", "user_id", "identity-link", "associate a pending activation with its account", "operational"},
		{"panel_access_links", "token_hash", "capability-digest", "validate an expiring one-time activation link", "operational"},
		{"panel_sessions", "user_id", "identity-link", "resolve an authenticated session to an active account", "operational"},
		{"panel_sessions", "token_hash", "capability-digest", "look up and revoke an authenticated session", "operational"},
		{"panel_login_limits", "key_hash", "pseudonymized-identifier", "limit sign-in attempts by network and account", "operational"},
		{"panel_report_access", "report_id", "artifact-link", "record the titleholder's current report availability", "operational"},
		{"panel_audit", "actor_id", "identity-link", "attribute an account or permission event", "operational-log"},
		{"panel_audit", "before_permissions", "authorization-state", "bounded prior capability identifiers; no customer content", "operational-log"},
		{"panel_audit", "after_permissions", "authorization-state", "bounded resulting capability identifiers; no customer content", "operational-log"},
		{"panel_audit", "reason_code", "reason-code", "closed policy-change reason; no operator free text", "operational-log"},
		{"panel_audit", "operation_id", "operation-link", "server-generated identity of a successful policy change", "operational-log"},
		{"panel_audit", "subject_id", "identity-link", "reference an affected account when applicable", "operational-log"},
		{"properties", "owner_user_id", "identity-link", "property ownership link", "identity"},
		{"properties", "address", "location", "vistoria subject property", "sealed-record"},
		{"rooms", "label", "free-text", "room navigation label; must not hold personal names", "sealed-record"},
		{"evidences", "storage_key", "image-locator", "locator for evidence photo bytes, which may show people", "sealed-record"},
		{"evidences", "latitude", "location", "device capture coordinate bound to the evidence", "sealed-record"},
		{"evidences", "longitude", "location", "device capture coordinate bound to the evidence", "sealed-record"},
		{"evidences", "captured_at_device", "temporal", "device capture-time claim bound to the evidence", "sealed-record"},
		{"findings", "note", "free-text", "technical finding note; must not hold contact data", "sealed-record"},
		{"occurrences", "title", "free-text", "occurrence record", "sealed-record"},
		{"occurrences", "body", "free-text", "occurrence record", "sealed-record"},
		{"reports", "pdf_key", "artifact-locator", "locator for the report PDF, which holds names and photos", "sealed-record"},
		{"manifests", "body", "sealed-payload", "canonical sealed payload; no direct PII by construction, tracked by name", "sealed-record"},
		{"shares", "token", "capability", "bearer token for recipient access; rotate, never log", "operational"},
		{"access_logs", "ip_hash", "pseudonymized-network", "HMAC-pseudonymized viewer IP for share audit", "operational-log"},
	}...)
}

// RetentionClasses assigns every application table a retention class.
// Periods require municipal archive law input and stay counsel-owned;
// code enforces coverage, never durations.
func RetentionClasses() map[string]string {
	return map[string]string{
		"users": "identity", "properties": "sealed-record",
		"tenancies": "sealed-record", "inspections": "sealed-record",
		"rooms": "sealed-record", "evidences": "sealed-record",
		"findings": "sealed-record", "reports": "sealed-record",
		"manifests": "sealed-record", "occurrences": "sealed-record",
		"occurrence_evidences": "sealed-record", "comparisons": "sealed-record",
		"shares": "operational", "access_logs": "operational-log",
		"schema_migrations":  "system",
		"panel_access_links": "operational", "panel_sessions": "operational",
		"panel_login_limits": "operational", "panel_report_access": "operational",
		"panel_role_permissions": "system", "panel_audit": "operational-log",
		"account_verifications": "operational", "account_mail_outbox": "operational",
		"account_registration_limits": "operational",
		"account_access_sessions":     "operational",
		"account_password_recoveries": "operational",
		"service_plans":               "system", "service_plan_versions": "system",
	}
}
