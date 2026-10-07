package service

import (
	"context"
	"crypto/sha256"
	"encoding/hex"
	"fmt"
	"io"
	"os"
	"strings"

	"vault/internal/store/postgres"

	"github.com/google/uuid"
)

// Upload buffers one file, hashes it server-side, enforces caps,
// stores immutable originals, and inserts the evidence row.
// received_at_server comes from the database, never the device.
func (s *EvidenceService) Upload(ctx context.Context, in UploadInput) (UploadResult, error) {
	if err := checkUUID(in.InspectionID, "inspection_id"); err != nil {
		return UploadResult{}, err
	}
	if in.RoomID != nil {
		if err := checkUUID(*in.RoomID, "room_id"); err != nil {
			return UploadResult{}, err
		}
	}
	typ := strings.ToLower(strings.TrimSpace(in.Type))
	maxBytes, err := checkTypeAndMime(typ, in.Mime)
	if err != nil {
		return UploadResult{}, err
	}
	if err := checkGeo(in.Latitude, in.Longitude); err != nil {
		return UploadResult{}, err
	}

	uc, err := s.store.UploadContext(ctx, in.InspectionID)
	if err != nil {
		return UploadResult{}, err
	}
	if uc.Status != "DRAFT" {
		return UploadResult{}, fmt.Errorf("inspection %s: %w", in.InspectionID, postgres.ErrSealed)
	}
	if in.RoomID != nil {
		ok, err := s.store.RoomBelongsTo(ctx, in.InspectionID, *in.RoomID)
		if err != nil {
			return UploadResult{}, err
		}
		if !ok {
			return UploadResult{}, fmt.Errorf("room not in inspection: %w", ErrInvalid)
		}
	}
	if err := checkCountQuota(typ, uc); err != nil {
		return UploadResult{}, err
	}

	sha, size, tmp, err := bufferAndHash(in.Content)
	if err != nil {
		return UploadResult{}, err
	}
	defer os.Remove(tmp.Name())
	defer tmp.Close()
	if size == 0 {
		return UploadResult{}, fmt.Errorf("empty file: %w", ErrInvalid)
	}
	if size > maxBytes {
		return UploadResult{}, fmt.Errorf("%s exceeds %d bytes: %w", typ, maxBytes, ErrTooLarge)
	}
	if uc.TotalBytes+size > uc.CapBytes {
		return UploadResult{}, fmt.Errorf("tenancy byte cap: %w", ErrQuota)
	}

	id := uuid.NewString()
	key := "originals/" + in.InspectionID + "/" + id
	if _, err := tmp.Seek(0, io.SeekStart); err != nil {
		return UploadResult{}, fmt.Errorf("rewind: %w", err)
	}
	if err := s.storage.PutOriginal(ctx, key, tmp, in.Mime); err != nil {
		return UploadResult{}, err
	}
	e, err := s.store.Create(ctx, postgres.EvidenceInput{
		ID: id, InspectionID: in.InspectionID, RoomID: in.RoomID,
		Type: typ, Mime: in.Mime, Size: size,
		StorageKey: key, SHA256: sha, CapturedAtDevice: in.CapturedAtDevice,
		Latitude: in.Latitude, Longitude: in.Longitude,
	})
	if err != nil {
		_ = s.storage.Delete(ctx, key) // orphan hygiene, best-effort
		return UploadResult{}, err
	}
	return UploadResult{Evidence: e}, nil
}

// checkTypeAndMime validates the evidence type and its MIME category.
// Returns the per-type byte limit.
func checkTypeAndMime(typ, mime string) (int64, error) {
	mime = strings.ToLower(strings.TrimSpace(mime))
	switch typ {
	case "photo":
		if !strings.HasPrefix(mime, "image/") {
			return 0, fmt.Errorf("photo needs image/* mime: %w", ErrInvalid)
		}
		return MaxPhotoBytes, nil
	case "video":
		if !strings.HasPrefix(mime, "video/") {
			return 0, fmt.Errorf("video needs video/* mime: %w", ErrInvalid)
		}
		return MaxVideoBytes, nil
	case "doc":
		if mime == "" || !strings.Contains(mime, "/") {
			return 0, fmt.Errorf("doc needs a mime type: %w", ErrInvalid)
		}
		return MaxDocBytes, nil
	default:
		return 0, fmt.Errorf("type must be photo|video|doc: %w", ErrInvalid)
	}
}

// checkCountQuota enforces per-type counts before buffering bytes.
func checkCountQuota(typ string, uc postgres.UploadContext) error {
	switch typ {
	case "photo":
		if uc.PhotoCount >= uc.CapPhotos {
			return fmt.Errorf("photo cap %d: %w", uc.CapPhotos, ErrQuota)
		}
	case "video":
		if uc.VideoCount >= uc.CapVideos {
			return fmt.Errorf("video cap %d: %w", uc.CapVideos, ErrQuota)
		}
	}
	return nil
}

// checkGeo validates device-reported coordinates. Nil means absent.
func checkGeo(lat, lng *float64) error {
	if lat == nil && lng == nil {
		return nil
	}
	if lat == nil || lng == nil {
		return fmt.Errorf("latitude and longitude go together: %w", ErrInvalid)
	}
	if *lat < -90 || *lat > 90 {
		return fmt.Errorf("latitude out of range: %w", ErrInvalid)
	}
	if *lng < -180 || *lng > 180 {
		return fmt.Errorf("longitude out of range: %w", ErrInvalid)
	}
	return nil
}

// bufferAndHash streams content to a temp file while hashing.
// Returns hex SHA-256, byte size, and the open temp file.
func bufferAndHash(r io.Reader) (string, int64, *os.File, error) {
	tmp, err := os.CreateTemp("", "evidence-*")
	if err != nil {
		return "", 0, nil, fmt.Errorf("tmp: %w", err)
	}
	h := sha256.New()
	n, err := io.Copy(tmp, io.TeeReader(r, h))
	if err != nil {
		tmp.Close()
		os.Remove(tmp.Name())
		return "", 0, nil, fmt.Errorf("buffer: %w", err)
	}
	return hex.EncodeToString(h.Sum(nil)), n, tmp, nil
}
