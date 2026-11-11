# Go Layout — handler → service → store

```text
backend/
  cmd/api/main.go            # wire deps, listen :8080
  cmd/worker/main.go         # PDF + thumbnails queue
  internal/
    httpapi/handler/         # HTTP only: parse, auth, respond
    httpapi/router.go        # routes
    service/                 # use-cases: seal, manifest, share
    store/postgres/          # SQL only, no HTTP
    storage/port.go          # StoragePort interface
    storage/s3store/         # S3 impl
    evidence/hasher.go       # SHA-256 streaming
    report/pdf.go            # PDF + QR contract
    auth/magiclink.go        # token issue/verify
    config/config.go         # env only
  migrations/0001_init.sql
  Dockerfile
```

Rules: handlers never touch `database/sql`. Stores never import `net/http`. Services own transactions and seal invariant.

The optional [web panel](web_panel.md) follows the same handler, service and
PostgreSQL boundaries. The Go binary embeds its templates and static assets.

Planned monthly commerce follows the same boundaries; proposed package
ownership is in [commercial domain](../03-architecture/commercial_domain.md).
No provider calls in stores and no subscription dependency in local workflows.
