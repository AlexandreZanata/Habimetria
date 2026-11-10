package main

import (
	"context"
	"fmt"
	"log"
	"os"
	"time"

	"vault/internal/config"
	"vault/internal/httpapi/panelweb"
	"vault/internal/service/panel"
	"vault/internal/store/postgres"
	"vault/migrations"
)

func main() {
	if len(os.Args) != 3 {
		log.Fatal("usage: panel-owner <email> <name>")
	}
	cfg := config.FromEnv()
	if err := cfg.Validate(); err != nil {
		log.Fatal(err)
	}
	ctx, cancel := context.WithTimeout(context.Background(), 15*time.Second)
	defer cancel()
	pool, err := postgres.Open(ctx, cfg.DatabaseURL)
	if err != nil {
		log.Fatal(err)
	}
	defer pool.Close()
	s := panel.New(postgres.NewPanelStore(pool))
	if _, err := panelweb.New(s, nil, cfg.PanelOrigin); err != nil {
		log.Fatal(err)
	}
	if err := postgres.Apply(ctx, pool, migrations.FS); err != nil {
		log.Fatal(err)
	}
	code, err := s.Bootstrap(ctx, os.Args[1], os.Args[2])
	if err != nil {
		log.Fatal(err)
	}
	fmt.Printf("%s/painel/#%s\n", cfg.PanelOrigin, code)
}
