package service

import (
	"context"
	"crypto/hmac"
	"crypto/sha256"
	"encoding/hex"
	"encoding/json"
	"errors"
	"os"
	"strings"
	"testing"
	"time"

	"vault/internal/storage/localstore"
	"vault/internal/store/postgres"
	"vault/internal/testutil"
	"vault/migrations"

	"github.com/google/uuid"
)

func TestSealServiceIntegrityIntegration(t *testing.T) {
	dsn := os.Getenv("TEST_DATABASE_URL")
	if dsn == "" {
		t.Skip("TEST_DATABASE_URL unset: seal integration unverified")
	}
	ctx, cancel := context.WithTimeout(context.Background(), 30*time.Second)
	defer cancel()
	check := func(err error) {
		t.Helper()
		if err != nil {
			t.Fatal(err)
		}
	}
	pool, err := postgres.Open(ctx, dsn)
	check(err)
	defer pool.Close()
	check(postgres.Apply(ctx, pool, migrations.FS))
	sealer := NewSeal(pool, testutil.HMACKey())
	if _, err := sealer.Seal(ctx, SealInput{InspectionID: uuid.NewString()}); !errors.Is(err, postgres.ErrNotFound) {
		t.Fatalf("missing inspection: want ErrNotFound, got %v", err)
	}
	property, err := postgres.CreateProperty(ctx, pool, "Synthetic service seal fixture")
	check(err)
	tenancy, err := postgres.CreateTenancy(ctx, pool, property.ID)
	check(err)
	inspection, err := postgres.CreateInspection(ctx, pool, tenancy.ID, "entry")
	check(err)
	if _, err := sealer.Seal(ctx, SealInput{InspectionID: inspection.ID}); !errors.Is(err, postgres.ErrEmpty) {
		t.Fatalf("empty inspection: want ErrEmpty, got %v", err)
	}
	deviceTime := time.Date(2001, 1, 1, 0, 0, 0, 0, time.UTC)
	evidenceService := NewEvidence(pool, localstore.New(t.TempDir()))
	upload, err := evidenceService.Upload(ctx, UploadInput{
		InspectionID: inspection.ID, Type: "doc", Mime: "application/pdf", Filename: "fixture.pdf",
		Content: strings.NewReader("%PDF-1.4\n% synthetic service seal evidence\n%%EOF\n"), CapturedAtDevice: &deviceTime,
	})
	check(err)
	before := time.Now().UTC()
	out, err := sealer.Seal(ctx, SealInput{InspectionID: inspection.ID})
	check(err)
	if out.Status != "SEALED" || out.EvidenceCount != 1 || out.SealedAt.Before(before) || out.SealedAt.After(time.Now().UTC()) {
		t.Fatalf("invalid server seal acknowledgment: %+v", out)
	}
	var body, digest, signature string
	check(pool.QueryRow(ctx, `SELECT body, sha256, signature FROM manifests WHERE inspection_id=$1`, inspection.ID).Scan(&body, &digest, &signature))
	measured := sha256.Sum256([]byte(body))
	if digest != hex.EncodeToString(measured[:]) || digest != out.ManifestSHA256 || signature != out.Signature {
		t.Fatal("stored manifest bytes do not match seal acknowledgment")
	}
	mac := hmac.New(sha256.New, []byte(testutil.HMACKey()))
	mac.Write([]byte(digest))
	if signature != hex.EncodeToString(mac.Sum(nil)) {
		t.Fatal("manifest signature is invalid")
	}
	var manifest manifestDoc
	check(json.Unmarshal([]byte(body), &manifest))
	if manifest.SealedAt != out.SealedAt.UTC().Format(time.RFC3339) || len(manifest.Evidences) != 1 || manifest.Evidences[0].SHA256 != upload.Evidence.SHA256 {
		t.Fatal("manifest does not preserve server time and original evidence hash")
	}
	if manifest.Evidences[0].ReceivedAtServer != upload.Evidence.ReceivedAtServer.UTC().Format(time.RFC3339) {
		t.Fatal("manifest replaced the server receipt time with the device claim")
	}
	if _, err := sealer.Seal(ctx, SealInput{InspectionID: inspection.ID}); !errors.Is(err, postgres.ErrSealed) {
		t.Fatalf("repeat seal: want ErrSealed, got %v", err)
	}
	var unchanged string
	check(pool.QueryRow(ctx, `SELECT body FROM manifests WHERE inspection_id=$1`, inspection.ID).Scan(&unchanged))
	if unchanged != body {
		t.Fatal("repeat seal rewrote the manifest")
	}
	pool.Close()
	if _, err := sealer.Seal(ctx, SealInput{InspectionID: inspection.ID}); err == nil || !strings.Contains(err.Error(), "list evidences:") {
		t.Fatalf("database failure was not surfaced with context: %v", err)
	}
}
