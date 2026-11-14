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
		Verification: account.NewVerification(store), Credentials: credentials, PasswordReset: account.NewPasswordReset(store)}
	if cfg.AccountMail.Enabled == "true" && (cfg.PanelOrigin == "" || cfg.PanelOrigin != cfg.AccountMail.Origin) {
		log.Print("account mail requests unavailable: mail and panel origins must match")
		return h
	}
	requests, err := accountdelivery.OpenRequests(cfg.AccountMail, cfg.Environment, pool)
	if err != nil && !errors.Is(err, accountdelivery.ErrDisabled) {
		log.Printf("account mail requests unavailable: %v", err)
	}
	if requests != nil {
		h.Registration, h.Recovery = requests.Registration, requests.Recovery
	}
	return h
}
