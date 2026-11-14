package activation

import (
	"context"
	"errors"
	"io"

	"vault/internal/config"
)

type Operation string

const (
	NewPurchase  Operation = "new_purchase"
	Delivery     Operation = "delivery"
	Restore      Operation = "restore"
	Reconcile    Operation = "reconcile"
	Notification Operation = "notification"
)

var (
	ErrOperation             = errors.New("unsupported store operation")
	ErrStoreDisabled         = errors.New("store integration disabled")
	ErrNewPurchasesDisabled  = errors.New("new purchases disabled")
	ErrContext               = errors.New("store app/environment context mismatch")
	ErrCredentialUnavailable = errors.New("store credential unavailable")
)

type StoreContext struct{ Store, Environment, AppID, AppleAppID string }

// Prepared is internal adapter setup, never identity/payment verification,
// an entitlement, an HTTP response or authorization to private user data.
type Prepared struct {
	Context         StoreContext
	IssuerID, KeyID string
	Credential      io.ReadCloser `json:"-"`
}

type Gate struct{ settings config.CommerceConfig }

// New retains a validated value snapshot and does not read credential files.
func New(cfg config.Config) (*Gate, error) {
	if err := cfg.Validate(); err != nil {
		return nil, err
	}
	return &Gate{settings: cfg.Commerce}, nil
}

func (g *Gate) Prepare(ctx context.Context, operation Operation, binding StoreContext) (Prepared, error) {
	if err := ctx.Err(); err != nil {
		return Prepared{}, err
	}
	switch operation {
	case NewPurchase, Delivery, Restore, Reconcile, Notification:
	default:
		return Prepared{}, ErrOperation
	}
	c := g.settings
	if !c.StoreEnabled(binding.Store) {
		return Prepared{}, ErrStoreDisabled
	}
	if binding.Environment != c.Environment || binding.AppID != c.AppIdentity(binding.Store) ||
		(binding.Store == config.AppStore && binding.AppleAppID != c.Apple.AppID) ||
		(binding.Store == config.GooglePlay && binding.AppleAppID != "") {
		return Prepared{}, ErrContext
	}
	if operation == NewPurchase && c.NewPurchasesEnabled != "true" {
		return Prepared{}, ErrNewPurchasesDisabled
	}
	credential, err := openCredential(c.SecretsDirectory, c.SecretReference(binding.Store))
	if err != nil {
		return Prepared{}, err
	}
	if err := ctx.Err(); err != nil {
		return Prepared{}, errors.Join(err, credential.Close())
	}
	prepared := Prepared{Context: binding, Credential: credential}
	if binding.Store == config.AppStore {
		prepared.IssuerID, prepared.KeyID = c.Apple.IssuerID, c.Apple.KeyID
	}
	return prepared, nil
}
