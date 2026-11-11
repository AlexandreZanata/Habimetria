package main

import (
	"context"
	"fmt"
	"log"
	"net/http"
	"os"
	"os/signal"
	"syscall"
	"time"

	"vault/internal/config"
	"vault/internal/httpapi"
	"vault/internal/httpapi/panelweb"
	"vault/internal/service"
	"vault/internal/service/panel"
	"vault/internal/storage"
	"vault/internal/storage/localstore"
	"vault/internal/storage/s3store"
	"vault/internal/store/postgres"
	"vault/migrations"
)

func main() {
	ctx, stop := signal.NotifyContext(context.Background(), os.Interrupt, syscall.SIGTERM)
	defer stop()

	cfg := config.FromEnv()

	if err := cfg.Validate(); err != nil {
		log.Fatalf("invalid config: %v", err)
	}

	dbCtx, cancel := context.WithTimeout(ctx, 10*time.Second)
	defer cancel()

	pool, err := postgres.Open(dbCtx, cfg.DatabaseURL)
	if err != nil {
		log.Fatalf("open postgres: %v", err)
	}
	defer pool.Close()

	if err := postgres.Apply(dbCtx, pool, migrations.FS); err != nil {
		log.Fatalf("apply migrations: %v", err)
	}

	store, err := openStorage(dbCtx, cfg)
	if err != nil {
		log.Fatalf("open storage: %v", err)
	}

	webService := panel.New(postgres.NewPanelStore(pool))
	var webHandler http.Handler
	if cfg.PanelOrigin != "" {
		reader, ok := store.(storage.Reader)
		if !ok {
			log.Fatal("object storage does not support authenticated reads")
		}
		web, err := panelweb.New(webService, reader, cfg.PanelOrigin)
		if err != nil {
			log.Fatalf("configure panel: %v", err)
		}
		webHandler = web.Routes()
	}

	mux := httpapi.NewRouter(httpapi.Deps{
		Health:     service.NewHealth(pool),
		Property:   service.NewProperty(pool),
		Tenancy:    service.NewTenancy(pool),
		Inspection: service.NewInspection(pool),
		Room:       service.NewRoom(pool),
		Evidence:   service.NewEvidence(pool, store),
		Finding:    service.NewFinding(pool),
		Coverage:   service.NewCoverage(pool),
		Seal:       service.NewSeal(pool, cfg.HMACKey),
		Report:     service.NewReport(pool, store, cfg.VerifyBaseURL),
		Share:      service.NewShare(pool, store, cfg.VerifyBaseURL, cfg.HMACKey),
		Occurrence: service.NewOccurrence(pool),
		Compare:    service.NewCompare(pool),
		HMACKey:    cfg.HMACKey,
		Panel:      webHandler,
		Guard: panelweb.ProtectLegacy(webService, cfg.PanelOrigin, panelweb.LegacyPolicy{
			AllowUnclaimedDevelopment: cfg.LegacyAPIMode == config.LegacyUnclaimedDevelopment,
		}),
	})

	srv := &http.Server{Addr: ":" + cfg.Port, Handler: httpapi.SecurityHeaders(mux)}
	go func() {
		log.Printf("vault api listening on :%s", cfg.Port)
		if err := srv.ListenAndServe(); err != nil && err != http.ErrServerClosed {
			log.Fatalf("listen: %v", err)
		}
	}()

	<-ctx.Done()
	shutdownCtx, cancel := context.WithTimeout(context.Background(), 10*time.Second)
	defer cancel()
	if err := srv.Shutdown(shutdownCtx); err != nil {
		log.Printf("shutdown: %v", err)
	}
}

// openStorage selects the S3 or local backend from env.
func openStorage(ctx context.Context, cfg config.Config) (storage.Port, error) {
	if cfg.StorageBackend == "local" {
		return localstore.New(cfg.LocalDir), nil
	}
	s := s3store.New(s3store.Config{
		Endpoint:     cfg.S3Endpoint,
		Region:       cfg.S3Region,
		Bucket:       cfg.S3Bucket,
		AccessKey:    cfg.S3AccessKey,
		SecretKey:    cfg.S3SecretKey,
		UsePathStyle: cfg.S3PathStyle,
	})
	if err := s.EnsureBucket(ctx); err != nil {
		return nil, fmt.Errorf("ensure bucket: %w", err)
	}
	return s, nil
}
