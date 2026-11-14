package main

import (
	"context"
	"encoding/json"
	"errors"
	"flag"
	"io"
	"time"

	"vault/internal/accountdelivery"
	"vault/internal/config"
	"vault/internal/store/postgres"
)

func run(ctx context.Context, args []string, output io.Writer) error {
	flags := flag.NewFlagSet("account-mail", flag.ContinueOnError)
	flags.SetOutput(io.Discard)
	once := flags.Bool("once", false, "process one batch")
	status := flags.Bool("status", false, "show safe aggregate state")
	limit := flags.Int("limit", 20, "batch size 1-100")
	if err := flags.Parse(args); err != nil {
		return errors.New("invalid account-mail options")
	}
	if flags.NArg() != 0 || *limit < 1 || *limit > 100 || (*once && *status) {
		return errors.New("invalid account-mail options")
	}
	cfg := config.FromEnv()
	if cfg.Environment != "development" && cfg.Environment != "production" {
		return errors.New("invalid account-mail environment")
	}
	dbCtx, cancel := context.WithTimeout(ctx, 10*time.Second)
	pool, err := postgres.Open(dbCtx, cfg.DatabaseURL)
	cancel()
	if err != nil {
		return accountdelivery.Unavailable("database", err)
	}
	defer pool.Close()
	write := func(value any) error {
		if err := json.NewEncoder(output).Encode(value); err != nil {
			return accountdelivery.Unavailable("output", err)
		}
		return nil
	}
	if *status {
		queryCtx, cancel := context.WithTimeout(ctx, 10*time.Second)
		defer cancel()
		value, err := postgres.NewAccountStore(pool).MailStatus(queryCtx)
		if err != nil {
			return accountdelivery.Unavailable("status", err)
		}
		return write(value)
	}
	runtime, err := accountdelivery.Open(cfg.AccountMail, cfg.Environment, pool)
	if err != nil {
		return err
	}
	for {
		result, err := runtime.Delivery.Drain(ctx, *limit)
		if err != nil {
			if ctx.Err() != nil {
				return nil
			}
			return err
		}
		if *once || result.Claimed > 0 {
			if err := write(result); err != nil {
				return err
			}
		}
		if *once {
			return nil
		}
		timer := time.NewTimer(5 * time.Second)
		select {
		case <-ctx.Done():
			timer.Stop()
			return nil
		case <-timer.C:
		}
	}
}
