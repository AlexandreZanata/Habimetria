package service

import (
	"context"
	"errors"
	"strings"
	"testing"
)

func TestFindingValidation(t *testing.T) {
	s := NewFinding(nil)
	valid := "123e4567-e89b-12d3-a456-426614174000"
	if _, err := s.Create(context.Background(), "nope", valid, "crack", nil); !errors.Is(err, ErrInvalid) {
		t.Fatalf("bad room: want ErrInvalid, got %v", err)
	}
	if _, err := s.Create(context.Background(), valid, "nope", "crack", nil); !errors.Is(err, ErrInvalid) {
		t.Fatalf("bad evidence: want ErrInvalid, got %v", err)
	}
	if _, err := s.Create(context.Background(), valid, valid, "explosion", nil); !errors.Is(err, ErrInvalid) {
		t.Fatalf("bad preset: want ErrInvalid, got %v", err)
	}
	long := strings.Repeat("n", MaxNoteLength+1)
	if _, err := s.Create(context.Background(), valid, valid, "crack", &long); !errors.Is(err, ErrInvalid) {
		t.Fatalf("long note: want ErrInvalid, got %v", err)
	}
	if _, err := s.List(context.Background(), "nope"); !errors.Is(err, ErrInvalid) {
		t.Fatalf("list bad room: want ErrInvalid, got %v", err)
	}
}

func TestRoomPercent(t *testing.T) {
	cases := map[int]int{0: 0, 1: 25, 2: 50, 3: 75, 4: 100, 9: 100}
	for photos, want := range cases {
		if got := RoomPercent(photos); got != want {
			t.Fatalf("photos=%d: want %d, got %d", photos, want, got)
		}
	}
}

func TestUploadGeoValidation(t *testing.T) {
	lat, lng := 91.0, 0.0
	svc := NewEvidenceWithStore(nil, fakeEvidenceStore{uc: draftContext()}, &fakeStorage{})
	_, err := svc.Upload(context.Background(), UploadInput{
		InspectionID: "123e4567-e89b-12d3-a456-426614174000",
		Type:         "photo", Mime: "image/jpeg",
		Latitude: &lat, Longitude: &lng,
		Content: strings.NewReader("x"),
	})
	if !errors.Is(err, ErrInvalid) {
		t.Fatalf("bad lat: want ErrInvalid, got %v", err)
	}
	lonely := 10.0
	_, err = svc.Upload(context.Background(), UploadInput{
		InspectionID: "123e4567-e89b-12d3-a456-426614174000",
		Type:         "photo", Mime: "image/jpeg",
		Latitude: &lonely,
		Content:  strings.NewReader("x"),
	})
	if !errors.Is(err, ErrInvalid) {
		t.Fatalf("lonely lat: want ErrInvalid, got %v", err)
	}
}
