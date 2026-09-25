# MVP 0.1 — Frozen Scope (ship in days, not weeks)

## Must (13 items)

1. Create property
2. Create rooms from template
3. Guided photo checklist + coverage %
4. Findings (photo + preset + optional note)
5. Original upload + server SHA-256 + server timestamp
6. Magic-link auth (after value, before seal)
7. Seal DRAFT → SEALED + manifest + signature
8. Paywall at seal (PaymentPort stub → real provider)
9. PDF with QR
10. Public `GET /v/{code}` verify page data
11. Share link (WhatsApp/Email/copy) + access log
12. Occurrence tables migrated (API minimal or hidden)
13. Exit inspection tables migrated (UI later)

## Non-must

Passwords, agency panel, AI, edit-after-seal, public photos, long video.

## Slice order (each shippable)

S1 DB+health → S2 property/rooms → S3 upload+hash → S4 findings+coverage →
S5 seal+manifest → S6 pay stub → S7 PDF+QR → S8 share+verify → S9 KMP capture+queue.
