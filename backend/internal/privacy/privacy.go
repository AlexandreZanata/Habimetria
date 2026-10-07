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
	return []Field{
		{"users", "email", "direct-identifier", "account identity for magic-link sign-in", "identity"},
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
	}
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
		"schema_migrations": "system",
	}
}
