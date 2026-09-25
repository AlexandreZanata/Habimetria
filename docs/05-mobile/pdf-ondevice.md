# On-Device PDF — Two-Pass Seal Flow

Follows `docs/03-architecture/client-processing.md`. Server never renders.

## Pass 1 — draft (offline OK)

1. App lays out cover + per-room photos + findings + device-claim disclaimers.
2. Android: `android.graphics.pdf.PdfDocument` (zero deps).
   iOS: Swift host implements `PlatformPdf` (PDFKit, CoreImage QR,
   share sheet); shared code owns the layout model + flow, never pixels.
   (Linux klib checks the Kotlin side; Swift compiles on macOS/Xcode.)
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
