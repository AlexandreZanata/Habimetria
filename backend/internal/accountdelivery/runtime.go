package accountdelivery

import (
	"errors"

	"vault/internal/config"
	"vault/internal/mail"
	"vault/internal/service/account"
	"vault/internal/store/postgres"

	"github.com/jackc/pgx/v5/pgxpool"
)

var ErrDisabled = errors.New("account mail is disabled")

type Runtime struct {
	Registration *account.Registration
	Delivery     *account.Delivery
}

func Open(settings config.AccountMailConfig, environment string, pool *pgxpool.Pool) (*Runtime, error) {
	key, err := proofKey(settings, environment, pool)
	if err != nil {
		return nil, err
	}
	var password []byte
	if settings.PasswordFile != "" {
		password, err = readSecret(settings.PasswordFile, 4096)
		if err != nil {
			return nil, Unavailable("SMTP password", err)
		}
		defer clear(password)
	}
	sender, err := mail.NewSMTP(settings.SMTPSettings(), password)
	if err != nil {
		return nil, Unavailable("configuration", err)
	}
	store := postgres.NewAccountStore(pool)
	registration, err := account.NewRegistration(store, key)
	if err != nil {
		return nil, Unavailable("configuration", err)
	}
	delivery, err := account.NewDelivery(store, key, sender)
	if err != nil {
		return nil, Unavailable("configuration", err)
	}
	return &Runtime{registration, delivery}, nil
}
