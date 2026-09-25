package service

import (
	"context"
	"encoding/json"
	"fmt"
	"net/http"
	"time"
)

// AsaasProvider verifies cobranças via Asaas REST API v3.
// Proof = Asaas payment id (e.g. "pay_123"). Paid states:
// RECEIVED (pago) and CONFIRMED (confirmado).
type AsaasProvider struct {
	apiKey  string
	baseURL string
	client  *http.Client
}

// NewAsaasProvider builds an Asaas verifier. baseURL defaults to the
// sandbox; production uses https://api.asaas.com/api/v3.
func NewAsaasProvider(apiKey, baseURL string, client *http.Client) AsaasProvider {
	if baseURL == "" {
		baseURL = "https://sandbox.asaas.com/api/v3"
	}
	if client == nil {
		client = &http.Client{Timeout: 10 * time.Second}
	}
	return AsaasProvider{apiKey: apiKey, baseURL: baseURL, client: client}
}

// Verify checks the payment is received or confirmed.
func (p AsaasProvider) Verify(ctx context.Context, proof string) error {
	if p.apiKey == "" {
		return fmt.Errorf("asaas: missing api key: %w", ErrPayment)
	}
	if proof == "" {
		return fmt.Errorf("asaas: missing payment_proof: %w", ErrPayment)
	}
	req, err := http.NewRequestWithContext(ctx, "GET", p.baseURL+"/payments/"+proof, nil)
	if err != nil {
		return fmt.Errorf("asaas: request: %w", err)
	}
	req.Header.Set("access_token", p.apiKey)
	req.Header.Set("Accept", "application/json")
	var body struct {
		ID     string `json:"id"`
		Status string `json:"status"`
	}
	if err := asaasDo(p.client, req, &body); err != nil {
		return err
	}
	if body.Status != "RECEIVED" && body.Status != "CONFIRMED" {
		return fmt.Errorf("asaas: payment %s status %q: %w", proof, body.Status, ErrPayment)
	}
	return nil
}

func asaasDo(client *http.Client, req *http.Request, out any) error {
	resp, err := client.Do(req)
	if err != nil {
		return fmt.Errorf("asaas: call: %w", err)
	}
	defer resp.Body.Close()
	if resp.StatusCode == http.StatusNotFound || resp.StatusCode == http.StatusUnauthorized || resp.StatusCode == http.StatusForbidden {
		return fmt.Errorf("asaas: http %d: %w", resp.StatusCode, ErrPayment)
	}
	if resp.StatusCode != http.StatusOK {
		return fmt.Errorf("asaas: http %d", resp.StatusCode)
	}
	if err := json.NewDecoder(resp.Body).Decode(out); err != nil {
		return fmt.Errorf("asaas: decode: %w", err)
	}
	return nil
}
