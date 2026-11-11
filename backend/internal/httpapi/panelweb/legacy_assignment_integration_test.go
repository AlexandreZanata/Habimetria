package panelweb_test

import (
	"crypto/sha256"
	"encoding/json"
	"net/url"
	"os"
	"testing"
)

// Synthetic rehearsal of the operator runbook. This is not a production claim,
// user identity verification, automatic migration or an exposed assignment API.
func TestReviewedLegacyAssignmentPreservesArtifactsIntegration(t *testing.T) {
	f := newBoundaryFixture(t)
	h := f.h
	path := "/painel/documentos/" + f.ids["reports"]
	pdf := h.call("GET", path, f.owner, nil, 200).Body.String()
	originalHash := func() [32]byte {
		t.Helper()
		out := h.boundaryRequest("GET", "/api/v1/inspections/"+f.ids["inspections"]+"/evidences/download-url?evidence_id="+f.evidenceID, f.owner, "")
		if out.Code != 200 {
			t.Fatalf("owner original access: %d", out.Code)
		}
		var link struct {
			URL string `json:"url"`
		}
		require(t, json.Unmarshal(out.Body.Bytes(), &link))
		parsed, err := url.Parse(link.URL)
		require(t, err)
		if parsed.Scheme != "file" {
			t.Fatal("expected isolated local fixture storage")
		}
		bytes, err := os.ReadFile(parsed.Path)
		require(t, err)
		return sha256.Sum256(bytes)
	}
	beforeOriginal := originalHash()
	frozen := func() string {
		t.Helper()
		var bytes string
		require(t, h.pool.QueryRow(h.ctx, `SELECT jsonb_build_object(
			'inspection',to_jsonb(i),'manifest',to_jsonb(m),'report',to_jsonb(r),
			'evidences',(SELECT jsonb_agg(to_jsonb(e) ORDER BY e.id) FROM evidences e WHERE e.inspection_id=i.id))::text
			FROM inspections i JOIN manifests m ON m.inspection_id=i.id JOIN reports r ON r.inspection_id=i.id
			WHERE i.id=$1`, f.ids["inspections"]).Scan(&bytes))
		return bytes
	}
	before := frozen()
	_, err := h.pool.Exec(h.ctx, `UPDATE properties SET owner_user_id=NULL WHERE id=$1`, f.ids["properties"])
	require(t, err)
	h.call("GET", path, f.owner, nil, 404)
	// Bootstrapping another operator must not claim legacy records.
	_, err = h.panel.Bootstrap(h.ctx, "primary@example.test", "Primary")
	require(t, err)
	var owner *string
	require(t, h.pool.QueryRow(h.ctx, `SELECT owner_user_id::text FROM properties WHERE id=$1`, f.ids["properties"]).Scan(&owner))
	if owner != nil {
		t.Fatal("bootstrap automatically assigned legacy ownership")
	}
	// After independent review, update exactly one NULL owner in a transaction.
	// A supplied actor/account ID alone never authorizes this through HTTP.
	tx, err := h.pool.Begin(h.ctx)
	require(t, err)
	defer tx.Rollback(h.ctx)
	var property string
	require(t, tx.QueryRow(h.ctx, `SELECT id::text FROM properties WHERE id=$1 AND owner_user_id IS NULL FOR UPDATE`, f.ids["properties"]).Scan(&property))
	tag, err := tx.Exec(h.ctx, `UPDATE properties p SET owner_user_id=u.id FROM users u
		WHERE p.id=$1 AND p.owner_user_id IS NULL AND u.id=$2 AND u.panel_active AND u.panel_password IS NOT NULL`, property, f.ownerID)
	require(t, err)
	if tag.RowsAffected() != 1 {
		t.Fatal("reviewed assignment did not affect exactly one property")
	}
	_, err = tx.Exec(h.ctx, `INSERT INTO panel_audit(actor_id,event,subject_id) VALUES($1,'legacy_property_assigned',$2)`, h.accountID("primary@example.test"), property)
	require(t, err)
	require(t, tx.Commit(h.ctx))
	if before != frozen() {
		t.Fatal("ownership assignment changed sealed rows or immutable artifact metadata")
	}
	if got := h.call("GET", path, f.owner, nil, 200).Body.String(); got != pdf {
		t.Fatal("assignment changed PDF bytes")
	}
	if originalHash() != beforeOriginal {
		t.Fatal("assignment changed original bytes")
	}
	h.call("GET", path, f.other, nil, 404)
	h.call("GET", path, f.staff, nil, 404)
	// Neither the migration rehearsal nor a dev API creates Report grants.
	var grants, audits int
	require(t, h.pool.QueryRow(h.ctx, `SELECT count(*) FROM panel_report_access WHERE report_id=$1`, f.ids["reports"]).Scan(&grants))
	require(t, h.pool.QueryRow(h.ctx, `SELECT count(*) FROM panel_audit WHERE event='legacy_property_assigned' AND subject_id=$1`, property).Scan(&audits))
	if grants != 0 || audits != 1 {
		t.Fatal("assignment unexpectedly granted staff access or lost audit")
	}
	otherID := h.accountID("boundary-bob@example.test")
	tag, err = h.pool.Exec(h.ctx, `UPDATE properties SET owner_user_id=$2 WHERE id=$1 AND owner_user_id IS NULL`, property, otherID)
	require(t, err)
	if tag.RowsAffected() != 0 {
		t.Fatal("assignment retry reassigned an owned property")
	}
	// Grant controls, public verification and strict API ownership still work.
	h.call("POST", "/painel/relatorios", f.owner, url.Values{"acao": {"disponibilizar"}, "id": {f.ids["reports"]}}, 303)
	h.call("POST", "/painel/relatorios", f.owner, url.Values{"acao": {"revogar"}, "id": {f.ids["reports"]}}, 303)
	verified := h.boundaryRequest("GET", "/v/"+f.verify, nil, "")
	var result struct {
		Valid bool `json:"signature_valid"`
	}
	require(t, json.Unmarshal(verified.Body.Bytes(), &result))
	if verified.Code != 200 || !result.Valid {
		t.Fatal("assignment broke existing public integrity verification")
	}
	if h.boundaryRequest("GET", "/api/v1/reports/"+f.ids["reports"], f.owner, "").Code != 200 ||
		h.boundaryRequest("GET", "/api/v1/reports/"+f.ids["reports"], nil, "").Code != 404 {
		t.Fatal("reviewed ownership did not preserve the API boundary")
	}
}
