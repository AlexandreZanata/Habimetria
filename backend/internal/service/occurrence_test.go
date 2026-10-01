package service

import (
	"context"
	"errors"
	"strings"
	"testing"
)

func TestOccurrenceRejectsBadPropertyID(t *testing.T) {
	s := NewOccurrence(nil)
	if _, err := s.Create(context.Background(), "not-a-uuid", "Leak", nil); !errors.Is(err, ErrInvalid) {
		t.Fatalf("want ErrInvalid, got %v", err)
	}
	if _, err := s.List(context.Background(), "not-a-uuid"); !errors.Is(err, ErrInvalid) {
		t.Fatalf("list: want ErrInvalid, got %v", err)
	}
}

func TestOccurrenceRejectsBlankTitle(t *testing.T) {
	s := NewOccurrence(nil)
	for _, title := range []string{"", "   "} {
		if _, err := s.Create(context.Background(), "123e4567-e89b-12d3-a456-426614174000", title, nil); !errors.Is(err, ErrInvalid) {
			t.Fatalf("title %q: want ErrInvalid, got %v", title, err)
		}
	}
}

func TestOccurrenceRejectsLongTitle(t *testing.T) {
	s := NewOccurrence(nil)
	long := strings.Repeat("a", MaxOccurrenceTitle+1)
	if _, err := s.Create(context.Background(), "123e4567-e89b-12d3-a456-426614174000", long, nil); !errors.Is(err, ErrInvalid) {
		t.Fatalf("want ErrInvalid, got %v", err)
	}
}
