package service

import (
	"bytes"
	"context"
	"crypto/sha256"
	"encoding/hex"
	"errors"
	"io"
	"strings"
	"testing"
	"time"

	"vault/internal/store/postgres"
)

type fakeEvidenceStore struct {
	uc      postgres.UploadContext
	ucErr   error
	create  func(postgres.EvidenceInput) (postgres.Evidence, error)
	get     postgres.Evidence
	getErr  error
	roomOK  bool
	roomErr error
}

func (f fakeEvidenceStore) UploadContext(context.Context, string) (postgres.UploadContext, error) {
	return f.uc, f.ucErr
}

func (f fakeEvidenceStore) Create(_ context.Context, in postgres.EvidenceInput) (postgres.Evidence, error) {
	return f.create(in)
}

func (f fakeEvidenceStore) Get(context.Context, string, string) (postgres.Evidence, error) {
	return f.get, f.getErr
}

func (f fakeEvidenceStore) RoomBelongsTo(context.Context, string, string) (bool, error) {
	return f.roomOK, f.roomErr
}

type fakeStorage struct {
	putKeys []string
	putErr  error
	deleted []string
	url     string
	urlErr  error
}

func (f *fakeStorage) PutOriginal(_ context.Context, key string, r io.Reader, _ string) error {
	if f.putErr != nil {
		return f.putErr
	}
	_, err := io.Copy(io.Discard, r)
	f.putKeys = append(f.putKeys, key)
	return err
}

func (f *fakeStorage) SignedGetURL(context.Context, string, time.Duration) (string, error) {
	return f.url, f.urlErr
}

func (f *fakeStorage) Delete(_ context.Context, key string) error {
	f.deleted = append(f.deleted, key)
	return nil
}

func draftContext() postgres.UploadContext {
	return postgres.UploadContext{Status: "DRAFT", CapPhotos: 300, CapVideos: 30, CapBytes: 1 << 31}
}

func TestUploadPhotoHappy(t *testing.T) {
	content := []byte("fake-jpeg-bytes")
	wantSHA := sha256.Sum256(content)
	st := &fakeStorage{}
	svc := NewEvidenceWithStore(nil, fakeEvidenceStore{
		uc: draftContext(),
		create: func(in postgres.EvidenceInput) (postgres.Evidence, error) {
			return postgres.Evidence{ID: in.ID, SHA256: in.SHA256, Size: in.Size, Sequence: 1, StorageKey: in.StorageKey}, nil
		},
	}, st)
	out, err := svc.Upload(context.Background(), UploadInput{
		InspectionID: "123e4567-e89b-12d3-a456-426614174000",
		Type:         "photo",
		Mime:         "image/jpeg",
		Content:      bytes.NewReader(content),
	})
	if err != nil {
		t.Fatalf("upload: %v", err)
	}
	if out.Evidence.SHA256 != hex.EncodeToString(wantSHA[:]) {
		t.Fatalf("sha mismatch: %s", out.Evidence.SHA256)
	}
	if out.Evidence.Size != int64(len(content)) || out.Evidence.Sequence != 1 {
		t.Fatalf("bad row: %+v", out.Evidence)
	}
	if len(st.putKeys) != 1 || !strings.HasPrefix(st.putKeys[0], "originals/") {
		t.Fatalf("bad storage keys: %v", st.putKeys)
	}
	if len(st.deleted) != 0 {
		t.Fatalf("unexpected delete: %v", st.deleted)
	}
}

func TestUploadSealedSkipsStorage(t *testing.T) {
	st := &fakeStorage{}
	uc := draftContext()
	uc.Status = "SEALED"
	svc := NewEvidenceWithStore(nil, fakeEvidenceStore{uc: uc}, st)
	_, err := svc.Upload(context.Background(), UploadInput{
		InspectionID: "123e4567-e89b-12d3-a456-426614174000",
		Type:         "photo", Mime: "image/jpeg",
		Content: bytes.NewReader([]byte("x")),
	})
	if !errors.Is(err, postgres.ErrSealed) {
		t.Fatalf("want ErrSealed, got %v", err)
	}
	if len(st.putKeys) != 0 {
		t.Fatalf("storage must not be touched when sealed: %v", st.putKeys)
	}
}

func TestUploadPhotoQuota(t *testing.T) {
	st := &fakeStorage{}
	uc := draftContext()
	uc.PhotoCount = 300
	svc := NewEvidenceWithStore(nil, fakeEvidenceStore{uc: uc}, st)
	_, err := svc.Upload(context.Background(), UploadInput{
		InspectionID: "123e4567-e89b-12d3-a456-426614174000",
		Type:         "photo", Mime: "image/jpeg",
		Content: bytes.NewReader([]byte("x")),
	})
	if !errors.Is(err, ErrQuota) {
		t.Fatalf("want ErrQuota, got %v", err)
	}
}

func TestUploadTooLarge(t *testing.T) {
	old := MaxPhotoBytes
	MaxPhotoBytes = 4
	defer func() { MaxPhotoBytes = old }()
	st := &fakeStorage{}
	svc := NewEvidenceWithStore(nil, fakeEvidenceStore{uc: draftContext()}, st)
	_, err := svc.Upload(context.Background(), UploadInput{
		InspectionID: "123e4567-e89b-12d3-a456-426614174000",
		Type:         "photo", Mime: "image/jpeg",
		Content: bytes.NewReader([]byte("five-bytes!")),
	})
	if !errors.Is(err, ErrTooLarge) {
		t.Fatalf("want ErrTooLarge, got %v", err)
	}
	if len(st.putKeys) != 0 {
		t.Fatalf("oversize must not reach storage: %v", st.putKeys)
	}
}

func TestUploadBadTypeMime(t *testing.T) {
	svc := NewEvidenceWithStore(nil, fakeEvidenceStore{uc: draftContext()}, &fakeStorage{})
	_, err := svc.Upload(context.Background(), UploadInput{
		InspectionID: "123e4567-e89b-12d3-a456-426614174000",
		Type:         "photo", Mime: "video/mp4",
		Content: bytes.NewReader([]byte("x")),
	})
	if !errors.Is(err, ErrInvalid) {
		t.Fatalf("want ErrInvalid, got %v", err)
	}
}

func TestUploadDBFailureDeletesOrphan(t *testing.T) {
	st := &fakeStorage{}
	svc := NewEvidenceWithStore(nil, fakeEvidenceStore{
		uc: draftContext(),
		create: func(postgres.EvidenceInput) (postgres.Evidence, error) {
			return postgres.Evidence{}, errors.New("db down")
		},
	}, st)
	_, err := svc.Upload(context.Background(), UploadInput{
		InspectionID: "123e4567-e89b-12d3-a456-426614174000",
		Type:         "doc", Mime: "application/pdf",
		Content: bytes.NewReader([]byte("pdf")),
	})
	if err == nil {
		t.Fatal("want error")
	}
	if len(st.deleted) != 1 || st.deleted[0] != st.putKeys[0] {
		t.Fatalf("orphan not cleaned: put=%v del=%v", st.putKeys, st.deleted)
	}
}

func TestUploadRoomOutsideInspection(t *testing.T) {
	room := "123e4567-e89b-12d3-a456-426614174001"
	svc := NewEvidenceWithStore(nil, fakeEvidenceStore{uc: draftContext()}, &fakeStorage{})
	_, err := svc.Upload(context.Background(), UploadInput{
		InspectionID: "123e4567-e89b-12d3-a456-426614174000",
		RoomID:       &room,
		Type:         "photo", Mime: "image/jpeg",
		Content: bytes.NewReader([]byte("x")),
	})
	if !errors.Is(err, ErrInvalid) {
		t.Fatalf("want ErrInvalid, got %v", err)
	}
}

func TestDownloadURLNotFound(t *testing.T) {
	svc := NewEvidenceWithStore(nil, fakeEvidenceStore{getErr: postgres.ErrNotFound}, &fakeStorage{})
	_, _, err := svc.DownloadURL(context.Background(),
		"123e4567-e89b-12d3-a456-426614174000", "123e4567-e89b-12d3-a456-426614174001")
	if !errors.Is(err, postgres.ErrNotFound) {
		t.Fatalf("want ErrNotFound, got %v", err)
	}
}
