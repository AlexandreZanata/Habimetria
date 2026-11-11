package collectionfixture

import (
	"context"
	"strings"
	"testing"
	"time"

	"vault/internal/service"
	"vault/internal/storage"
	"vault/internal/testutil"

	"github.com/jackc/pgx/v5/pgxpool"
)

const PDF = "%PDF-1.4\n% synthetic commercial fixture, no customer data\n%%EOF\n"

type Objects interface {
	storage.Port
	storage.Reader
}

type Fixture struct {
	Property, Tenancy, Draft, Inspection, Room, Evidence, Finding string
	Report, VerifyCode, Occurrence, EvidenceKey, PDFKey           string
}

func New(t *testing.T, ctx context.Context, pool *pgxpool.Pool, objects Objects, owner, label string) Fixture {
	t.Helper()
	check := func(err error) {
		t.Helper()
		if err != nil {
			t.Fatal(err)
		}
	}
	property, err := service.NewProperty(pool).Create(ctx, "Synthetic fixture "+label)
	check(err)
	if owner != "" {
		_, err := pool.Exec(ctx, `UPDATE properties SET owner_user_id=$2 WHERE id=$1`, property.ID, owner)
		check(err)
	}
	tenancy, err := service.NewTenancy(pool).Create(ctx, property.ID)
	check(err)
	draft, err := service.NewInspection(pool).Create(ctx, tenancy.ID, "entry")
	check(err)
	inspection, err := service.NewInspection(pool).Create(ctx, tenancy.ID, "entry")
	check(err)
	room, err := service.NewRoom(pool).Create(ctx, inspection.ID, "fixture-room", "Synthetic room "+label)
	check(err)
	claim := testutil.TestTime()
	upload, err := service.NewEvidence(pool, objects).Upload(ctx, service.UploadInput{
		InspectionID: inspection.ID, RoomID: &room.ID, Type: "doc", Filename: "fixture.pdf", Mime: "application/pdf",
		Content: strings.NewReader(PDF), CapturedAtDevice: &claim,
	})
	check(err)
	cleanupObject(t, objects, upload.Evidence.StorageKey)
	finding, err := service.NewFinding(pool).Create(ctx, room.ID, upload.Evidence.ID, "crack", nil)
	check(err)
	sealed, err := service.NewSeal(pool, testutil.HMACKey()).Seal(ctx, service.SealInput{InspectionID: inspection.ID})
	check(err)
	report, err := service.NewReport(pool, objects, "http://localhost:8080").Attach(ctx, service.AttachInput{
		ReportID: sealed.ReportID, Filename: "fixture.pdf", Mime: "application/pdf", Content: strings.NewReader(PDF),
	})
	check(err)
	cleanupObject(t, objects, report.PDFKey)
	occurrence, err := service.NewOccurrence(pool).Create(ctx, property.ID, "Synthetic occurrence "+label, nil)
	check(err)
	return Fixture{property.ID, tenancy.ID, draft.ID, inspection.ID, room.ID, upload.Evidence.ID, finding.ID,
		report.ID, report.VerifyCode, occurrence.ID, upload.Evidence.StorageKey, report.PDFKey}
}

func cleanupObject(t *testing.T, objects Objects, key string) {
	t.Helper()
	t.Cleanup(func() {
		ctx, cancel := context.WithTimeout(context.Background(), 10*time.Second)
		defer cancel()
		if err := objects.Delete(ctx, key); err != nil {
			t.Errorf("remove fixture object: %v", err)
		}
	})
}
