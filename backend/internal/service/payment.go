package service

import (
	"context"
)

// PaymentPort abstracts the paywall provider. S5 ships the stub;
// S6 replaces it with Stripe/MercadoPago without touching Seal.
type PaymentPort interface {
	Verify(ctx context.Context, proof string) error
}

// StubPayment approves every seal. Records nothing.
// S6 persists proofs and enforces real charges.
type StubPayment struct{}

// Verify always succeeds.
func (StubPayment) Verify(context.Context, string) error { return nil }
