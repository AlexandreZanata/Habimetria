package service

import (
	"context"
	"errors"
	"testing"
)

func TestPropertyRejectsBlankAddress(t *testing.T) {
	s := NewProperty(nil)
	for _, addr := range []string{"", "  ", "ab"} {
		if _, err := s.Create(context.Background(), addr); !errors.Is(err, ErrInvalid) {
			t.Fatalf("address %q: want ErrInvalid, got %v", addr, err)
		}
	}
}

func TestTenancyRejectsBadPropertyID(t *testing.T) {
	s := NewTenancy(nil)
	if _, err := s.Create(context.Background(), "not-a-uuid"); !errors.Is(err, ErrInvalid) {
		t.Fatalf("want ErrInvalid, got %v", err)
	}
}

func TestInspectionValidation(t *testing.T) {
	s := NewInspection(nil)
	valid := "123e4567-e89b-12d3-a456-426614174000"
	if _, err := s.Create(context.Background(), "nope", "entry"); !errors.Is(err, ErrInvalid) {
		t.Fatalf("bad tenancy id: want ErrInvalid, got %v", err)
	}
	if _, err := s.Create(context.Background(), valid, "sideways"); !errors.Is(err, ErrInvalid) {
		t.Fatalf("bad kind: want ErrInvalid, got %v", err)
	}
}

func TestRoomValidation(t *testing.T) {
	s := NewRoom(nil)
	valid := "123e4567-e89b-12d3-a456-426614174000"
	if _, err := s.Create(context.Background(), "nope", "kitchen", ""); !errors.Is(err, ErrInvalid) {
		t.Fatalf("bad inspection id: want ErrInvalid, got %v", err)
	}
	if _, err := s.Create(context.Background(), valid, "   ", ""); !errors.Is(err, ErrInvalid) {
		t.Fatalf("blank key: want ErrInvalid, got %v", err)
	}
	if _, err := s.List(context.Background(), "nope"); !errors.Is(err, ErrInvalid) {
		t.Fatalf("list bad id: want ErrInvalid, got %v", err)
	}
	if _, err := s.EnsureTemplate(context.Background(), "nope"); !errors.Is(err, ErrInvalid) {
		t.Fatalf("template bad id: want ErrInvalid, got %v", err)
	}
}
