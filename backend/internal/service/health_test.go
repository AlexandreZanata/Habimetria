package service

import (
	"context"
	"errors"
	"testing"
)

type fakePinger struct{ err error }

func (f fakePinger) Ping(context.Context) error { return f.err }

func TestCheckDBOk(t *testing.T) {
	s := NewHealth(fakePinger{})
	st := s.Check(context.Background())
	if st.Status != "ok" || st.Database != "ok" {
		t.Fatalf("want ok/ok, got %+v", st)
	}
	if !s.Ready(context.Background()) {
		t.Fatal("want ready true")
	}
}

func TestCheckDBDown(t *testing.T) {
	s := NewHealth(fakePinger{err: errors.New("down")})
	st := s.Check(context.Background())
	if st.Status != "degraded" {
		t.Fatalf("want degraded, got %+v", st)
	}
	if s.Ready(context.Background()) {
		t.Fatal("want ready false")
	}
}

func TestCheckNoDB(t *testing.T) {
	s := NewHealth(nil)
	if s.Ready(context.Background()) {
		t.Fatal("nil db must not be ready")
	}
}
