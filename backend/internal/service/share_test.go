package service

import (
	"context"
	"errors"
	"testing"
)

func TestShareValidation(t *testing.T) {
	s := NewShare(nil, nil, "https://example.com", "k")
	valid := "123e4567-e89b-12d3-a456-426614174000"
	if _, err := s.Create(context.Background(), "nope", "link", "1.2.3.4"); !errors.Is(err, ErrInvalid) {
		t.Fatalf("bad report id: want ErrInvalid, got %v", err)
	}
	if _, err := s.Create(context.Background(), valid, "pigeon", "1.2.3.4"); !errors.Is(err, ErrInvalid) {
		t.Fatalf("bad channel: want ErrInvalid, got %v", err)
	}
	if _, err := s.Open(context.Background(), "", "1.2.3.4"); !errors.Is(err, ErrInvalid) {
		t.Fatalf("empty token: want ErrInvalid, got %v", err)
	}
	if _, err := s.DownloadURL(context.Background(), "", "1.2.3.4"); !errors.Is(err, ErrInvalid) {
		t.Fatalf("empty token download: want ErrInvalid, got %v", err)
	}
	if _, err := s.Timeline(context.Background(), "nope"); !errors.Is(err, ErrInvalid) {
		t.Fatalf("bad timeline id: want ErrInvalid, got %v", err)
	}
}

func TestShareURLShape(t *testing.T) {
	s := NewShare(nil, nil, "https://example.com", "k")
	if got := s.ShareURL("ABC123"); got != "https://example.com/s/ABC123" {
		t.Fatalf("bad share url: %s", got)
	}
}
