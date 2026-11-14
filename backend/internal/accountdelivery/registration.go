package accountdelivery

import (
	"vault/internal/accountproof"
	"vault/internal/config"
	"vault/internal/service/account"
	"vault/internal/store/postgres"

	"github.com/jackc/pgx/v5/pgxpool"
)

// OpenRegistration requires only the proof key; SMTP credentials stay in the worker.
func OpenRegistration(settings config.AccountMailConfig, environment string, pool *pgxpool.Pool) (*account.Registration, error) {
	key, err := proofKey(settings, environment, pool)
	if err != nil {
		return nil, err
	}
	return account.NewRegistration(postgres.NewAccountStore(pool), key)
}

func proofKey(settings config.AccountMailConfig, environment string, pool *pgxpool.Pool) (*accountproof.Key, error) {
	if err := settings.Validate(environment); err != nil {
		return nil, Unavailable("configuration", err)
	}
	if settings.Enabled != "true" {
		return nil, ErrDisabled
	}
	if pool == nil {
		return nil, Unavailable("database", account.ErrInput)
	}
	master, err := readSecret(settings.ProofKeyFile, 32)
	if err != nil {
		return nil, Unavailable("proof key", err)
	}
	key, err := accountproof.New(master)
	clear(master)
	if err != nil {
		return nil, Unavailable("proof key", err)
	}
	return key, nil
}
