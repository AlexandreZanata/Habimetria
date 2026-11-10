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
	"vault/internal/service"
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

	mux := httpapi.NewRouter(httpapi.Deps{
		Health:     service.NewHealth(pool),
		Property:   service.NewProperty(pool),
		Tenancy:    service.NewTenancy(pool),
		Inspection: service.NewInspection(pool),
		Room:       service.NewRoom(pool),
		Evidence:   service.NewEvidence(pool, store),
		Finding:    service.NewFinding(pool),
		Coverage:   service.NewCoverage(pool),
		Seal:       service.NewSeal(pool, buildPayments(cfg), cfg.HMACKey),
		Report:     service.NewReport(pool, store, cfg.VerifyBaseURL),
		Share:      service.NewShare(pool, store, cfg.VerifyBaseURL, cfg.HMACKey),
		Occurrence: service.NewOccurrence(pool),
		Compare:    service.NewCompare(pool),
		HMACKey:    cfg.HMACKey,
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

// buildPayments selects the paywall backend. Default off keeps the MVP
// frictionless (photos + evidence + PDF). Live mode enforces provider
// proofs at seal; see docs/03-architecture/payments.md.
func buildPayments(cfg config.Config) service.PaymentPort {
	if cfg.PaymentsMode != "live" {
		return service.StubPayment{}
	}
	if cfg.PaymentsProvider == "asaas" {
		return service.NewAsaasProvider(cfg.AsaasAPIKey, cfg.AsaasAPIBase, nil)
	}
	return service.NewStripeProvider(cfg.StripeSecretKey, cfg.StripeAPIBase, nil)
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
