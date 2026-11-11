package accountdelivery

import (
	"vault/internal/config"
	"vault/internal/service/account"
	"vault/internal/store/postgres"

	"github.com/jackc/pgx/v5/pgxpool"
)

type Requests struct {
	Registration *account.Registration
	Recovery     *account.RecoveryRequest
}

// OpenRequests reads one private proof key for both initiation purposes.
func OpenRequests(settings config.AccountMailConfig, environment string, pool *pgxpool.Pool) (*Requests, error) {
	key, err := proofKey(settings, environment, pool)
	if err != nil {
		return nil, err
	}
	store := postgres.NewAccountStore(pool)
	registration, err := account.NewRegistration(store, key)
	if err != nil {
		return nil, Unavailable("configuration", err)
	}
	recovery, err := account.NewRecoveryRequest(store, key)
	if err != nil {
		return nil, Unavailable("configuration", err)
	}
	return &Requests{registration, recovery}, nil
}
