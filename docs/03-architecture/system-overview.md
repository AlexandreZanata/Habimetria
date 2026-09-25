# System Overview — Minimal but Credible

```text
KMP App (Android+iOS)
   │  HTTPS + resumable upload, signed URLs
   ▼
Go API (:8080)
   ├── Postgres 18.6-alpine (source of truth)
   ├── S3-compatible storage (originals immutable)
   ├── PDF worker (async queue → PDF + QR)
   ├── Payment port (Stripe / MercadoPago)
   └── Public verify API: GET /v/{code}
```

## Principles

- API stateless. All trust fields server-side.
- Storage private. Only `signed URL, 5 min TTL`.
- Originals never overwritten. Derivatives: `preview.webp`, `thumbnail.webp`.
- Async heavy work: hash verify, thumbnails, PDF.
- No agency ERP in MVP. Tenant app + share link only.
