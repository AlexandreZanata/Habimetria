package service

import (
	"context"
	"encoding/json"
	"fmt"
	"net/http"
	"time"
)

// ErrPayment marks a 402 paywall rejection: missing proof or
// provider reports unpaid. Wraps the provider cause.
var ErrPayment = paymentError("payment required or not confirmed")

type paymentError string

func (e paymentError) Error() string { return string(e) }

// StripeProvider verifies Checkout Sessions via Stripe's REST API.
// No SDK: one GET with basic auth. Proof = checkout session id.
type StripeProvider struct {
	secretKey string
	baseURL   string
	client    *http.Client
}

// NewStripeProvider builds a Stripe verifier. baseURL defaults to the
// live API; tests and local fakes inject their own.
func NewStripeProvider(secretKey, baseURL string, client *http.Client) StripeProvider {
	if baseURL == "" {
		baseURL = "https://api.stripe.com"
	}
	if client == nil {
		client = &http.Client{Timeout: 10 * time.Second}
	}
	return StripeProvider{secretKey: secretKey, baseURL: baseURL, client: client}
}

// Verify checks the session is paid.
func (p StripeProvider) Verify(ctx context.Context, proof string) error {
	if p.secretKey == "" {
		return fmt.Errorf("stripe: missing secret key: %w", ErrPayment)
	}
	if proof == "" {
		return fmt.Errorf("stripe: missing payment_proof: %w", ErrPayment)
	}
	req, err := http.NewRequestWithContext(ctx, "GET", p.baseURL+"/v1/checkout/sessions/"+proof, nil)
	if err != nil {
		return fmt.Errorf("stripe: request: %w", err)
	}
	req.SetBasicAuth(p.secretKey, "")
	var body struct {
		ID            string `json:"id"`
		PaymentStatus string `json:"payment_status"`
	}
	if err := stripeDo(p.client, req, &body); err != nil {
		return err
	}
	if body.PaymentStatus != "paid" {
		return fmt.Errorf("stripe: session %s status %q: %w", proof, body.PaymentStatus, ErrPayment)
	}
	return nil
}

func stripeDo(client *http.Client, req *http.Request, out any) error {
	resp, err := client.Do(req)
	if err != nil {
		return fmt.Errorf("stripe: call: %w", err)
	}
	defer resp.Body.Close()
	if resp.StatusCode == http.StatusNotFound || resp.StatusCode == http.StatusUnauthorized {
		return fmt.Errorf("stripe: http %d: %w", resp.StatusCode, ErrPayment)
	}
	if resp.StatusCode != http.StatusOK {
		return fmt.Errorf("stripe: http %d", resp.StatusCode)
	}
	if err := json.NewDecoder(resp.Body).Decode(out); err != nil {
		return fmt.Errorf("stripe: decode: %w", err)
	}
	return nil
}
