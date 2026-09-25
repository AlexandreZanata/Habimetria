package service

import (
	"context"
	"errors"
	"net/http"
	"net/http/httptest"
	"testing"
)

func TestStripePaidAndUnpaid(t *testing.T) {
	srv := httptest.NewServer(http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
		if r.URL.Path == "/v1/checkout/sessions/cs_paid" {
			w.Write([]byte(`{"id":"cs_paid","payment_status":"paid"}`))
			return
		}
		w.Write([]byte(`{"id":"cs_open","payment_status":"unpaid"}`))
	}))
	defer srv.Close()
	p := NewStripeProvider("sk_test", srv.URL, nil)
	if err := p.Verify(context.Background(), "cs_paid"); err != nil {
		t.Fatalf("paid: %v", err)
	}
	if err := p.Verify(context.Background(), "cs_open"); !errors.Is(err, ErrPayment) {
		t.Fatalf("unpaid: want ErrPayment, got %v", err)
	}
	if err := p.Verify(context.Background(), ""); !errors.Is(err, ErrPayment) {
		t.Fatalf("empty proof: want ErrPayment, got %v", err)
	}
	if err := NewStripeProvider("", srv.URL, nil).Verify(context.Background(), "cs_paid"); !errors.Is(err, ErrPayment) {
		t.Fatalf("missing key: want ErrPayment")
	}
}

func TestAsaasReceivedAndPending(t *testing.T) {
	srv := httptest.NewServer(http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
		if r.URL.Path == "/payments/pay_ok" {
			w.Write([]byte(`{"id":"pay_ok","status":"RECEIVED"}`))
			return
		}
		w.Write([]byte(`{"id":"pay_x","status":"PENDING"}`))
	}))
	defer srv.Close()
	p := NewAsaasProvider("key", srv.URL, nil)
	if err := p.Verify(context.Background(), "pay_ok"); err != nil {
		t.Fatalf("received: %v", err)
	}
	if err := p.Verify(context.Background(), "pay_x"); !errors.Is(err, ErrPayment) {
		t.Fatalf("pending: want ErrPayment, got %v", err)
	}
	if err := NewAsaasProvider("", srv.URL, nil).Verify(context.Background(), "pay_ok"); !errors.Is(err, ErrPayment) {
		t.Fatalf("missing key: want ErrPayment")
	}
}

func TestPaymentProvidersUnauthorizedMaps402(t *testing.T) {
	srv := httptest.NewServer(http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
		w.WriteHeader(http.StatusUnauthorized)
	}))
	defer srv.Close()
	if err := NewStripeProvider("bad", srv.URL, nil).Verify(context.Background(), "x"); !errors.Is(err, ErrPayment) {
		t.Fatalf("stripe 401: want ErrPayment, got %v", err)
	}
	if err := NewAsaasProvider("bad", srv.URL, nil).Verify(context.Background(), "x"); !errors.Is(err, ErrPayment) {
		t.Fatalf("asaas 401: want ErrPayment, got %v", err)
	}
}
