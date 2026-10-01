package service

import (
	"context"
	"errors"
	"testing"
)

func TestCompareRejectsBadIDs(t *testing.T) {
	s := NewCompare(nil)
	valid := "123e4567-e89b-12d3-a456-426614174000"
	if _, err := s.Upsert(context.Background(), "not-a-uuid", valid, valid, "walls", nil, nil, "changed"); !errors.Is(err, ErrInvalid) {
		t.Fatalf("bad tenancy id: want ErrInvalid, got %v", err)
	}
	if _, err := s.Upsert(context.Background(), valid, "not-a-uuid", valid, "walls", nil, nil, "changed"); !errors.Is(err, ErrInvalid) {
		t.Fatalf("bad entry room id: want ErrInvalid, got %v", err)
	}
	if _, err := s.Upsert(context.Background(), valid, valid, valid, "walls", strPtr("nope"), nil, "changed"); !errors.Is(err, ErrInvalid) {
		t.Fatalf("bad evidence id: want ErrInvalid, got %v", err)
	}
	if _, err := s.List(context.Background(), "not-a-uuid"); !errors.Is(err, ErrInvalid) {
		t.Fatalf("list: want ErrInvalid, got %v", err)
	}
}

func TestCompareRejectsBadVocabulary(t *testing.T) {
	s := NewCompare(nil)
	valid := "123e4567-e89b-12d3-a456-426614174000"
	if _, err := s.Upsert(context.Background(), valid, valid, valid, "ceiling-fan", nil, nil, "changed"); !errors.Is(err, ErrInvalid) {
		t.Fatalf("bad category: want ErrInvalid, got %v", err)
	}
	if _, err := s.Upsert(context.Background(), valid, valid, valid, "walls", nil, nil, "liability"); !errors.Is(err, ErrInvalid) {
		t.Fatalf("bad tag: want ErrInvalid, got %v", err)
	}
}

func TestCompareNormalizesCase(t *testing.T) {
	s := NewCompare(nil)
	_ = s
	if !validValue("walls", ShotCategories) || validValue("WALLS", ShotCategories) {
		t.Fatal("categories match lowercase only; service lowercases before store")
	}
	if !validValue("wear", CompareTags) || validValue("WEAR", CompareTags) {
		t.Fatal("tags match lowercase only; service lowercases before store")
	}
}

func strPtr(s string) *string { return &s }
