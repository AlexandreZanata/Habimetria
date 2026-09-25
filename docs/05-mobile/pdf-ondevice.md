# On-Device PDF — Two-Pass Seal Flow

Follows `docs/03-architecture/client-processing.md`. Server never renders.

## Pass 1 — draft (offline OK)

1. App lays out cover + per-room photos + findings + device-claim disclaimers.
2. Android: `android.graphics.pdf.PdfDocument` (zero deps).
   iOS: `PDFKit` via `expect/actual` interface. Common layout model in
   `commonMain` (`vault.pdf` package); platform code only draws.
3. Photos embedded at ≤1280px JPEG to bound render time and file size.

## Pass 2 — seal + final

1. App calls `POST /inspections/{id}/seal` → gets `verify_code`,
   `manifest_sha256`, `verify_url` (from `GET /reports/{id}`).
2. App renders QR (zxing-core) + seal block page, writes final PDF.
3. App uploads final via `POST /reports/{id}/pdf`; server returns
   `report_sha256`. Mismatch vs local hash → show error, never "saved".

## Budgets

- Final PDF ≤ 50 MB (server cap). Target < 10 MB per 100 photos.
- Render off-main-thread with progress; cancellable; retryable upload
  through the same evidence queue (idempotency-key).
