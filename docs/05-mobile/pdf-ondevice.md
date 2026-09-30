# On-Device PDF — Two-Pass Seal Flow

Follows `docs/03-architecture/client-processing.md`. Server never renders.

## Pass 1 — draft (offline OK)

1. App lays out cover + per-room photos + findings + device-claim disclaimers.
2. Android: `android.graphics.pdf.PdfDocument` (zero deps).
   iOS: Swift host implements `PlatformPdf` (PDFKit, CoreImage QR,
   share sheet); shared code owns the layout model + flow, never pixels.
   (Linux klib checks the Kotlin side; Swift compiles on macOS/Xcode.)
3. Photos embedded at ≤1280px JPEG to bound render time and file size.

## Pass 2 — seal + final (server integration)

1. App calls `POST /inspections/{id}/seal` → gets `verify_code`,
   `manifest_sha256`, `verify_url` (from `GET /reports/{id}`).
2. App renders QR (zxing-core) + seal block page, writes final PDF.
3. App uploads final via `POST /reports/{id}/pdf`; server returns
   `report_sha256`. Mismatch vs local hash → show error, never "saved".

## Local final (L0 authority)

1. Render exactly the sealed Manifest snapshot on-device.
2. Save final PDF bytes locally before sharing; hash once finalized.
3. Sign a detached receipt binding Report ID, Manifest hash, PDF hash,
   format version and signer identity.
4. Never embed the whole-file PDF hash inside that same PDF.
   The PDF may carry Manifest hash, Report ID, key fingerprint, QR and
   verification instructions; exact file integrity lives in the receipt.
5. QR is a lookup/import aid for Report/Manifest identity, never proof
   of byte integrity and never a LAN/localhost URL.

## Local pipeline (L6)

- Shared contract in `vault.local` (`PdfContract`): content model from
  the sealed snapshot, exact photo pagination from aspect rects, style
  tokens. Photo slots fit their page; continuation pages keep the
  no-drop invariant on font-metric drift.
- Platform ports: `AndroidPlanRenderer` (PdfDocument, sampled decode,
  StaticLayout wrap, zxing QR) and `IosPlanRenderer` (UIKit PDF,
  thumbnail decode, CoreText measure, CoreImage QR). Logical contents
  match; bytes need not.
- `ReportGenerator` runs architecture order: pre-check originals →
  paginate → render with progress/cancel → save bytes → hash → sign
  receipt (`local-receipt/v1`) → commit READY. Missing originals fail
  finalization instead of silent omission; cancel/retry keep SEALED.
- Reports persist (`reports`, unique per manifest) with the receipt;
  `LocalPdfScreen` shows progress, hashes, share and verification.

## Budgets

- Final PDF ≤ 50 MB (server cap). Target < 10 MB per 100 photos.
- Render off-main-thread with progress; cancellable; retryable upload
  through the same evidence queue (idempotency-key).
