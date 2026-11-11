package main

import (
	"errors"
	"log"

	"vault/internal/accountdelivery"
	"vault/internal/config"
	"vault/internal/httpapi"
	"vault/internal/service/account"
	"vault/internal/service/panel"
	"vault/internal/store/postgres"

	"github.com/jackc/pgx/v5/pgxpool"
)

func connectedAccount(cfg config.Config, pool *pgxpool.Pool, credentials *panel.Service) *httpapi.AccountHandler {
	store := postgres.NewAccountStore(pool)
	h := &httpapi.AccountHandler{Authentication: account.NewAuthentication(store, credentials),
		Verification: account.NewVerification(store), Credentials: credentials}
	if cfg.AccountMail.Enabled == "true" && (cfg.PanelOrigin == "" || cfg.PanelOrigin != cfg.AccountMail.Origin) {
		log.Print("account registration unavailable: mail and panel origins must match")
		return h
	}
	registration, err := accountdelivery.OpenRegistration(cfg.AccountMail, cfg.Environment, pool)
	if err != nil && !errors.Is(err, accountdelivery.ErrDisabled) {
		log.Printf("account registration unavailable: %v", err)
	}
	h.Registration = registration
	return h
}
