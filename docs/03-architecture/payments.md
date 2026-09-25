# Payments — Basic Stripe + Asaas, Off by Default

## MVP decision (binding)

The MVP optimizes one flow: **photos + evidence + PDF, as fast as possible**.
Authentication, users, and billing ship only if the project moves forward.
The paywall therefore exists as a provider seam, default **off**.

## Modes (`PAYMENTS_MODE`)

- `off` (default): seal requires no proof. Zero friction for the MVP loop.
- `live`: seal requires a provider proof, else HTTP 402.

## Providers (`PAYMENTS_PROVIDER`)

- `stripe`: proof = Checkout Session id. Paid iff
  `GET /v1/checkout/sessions/{id}` returns `payment_status: "paid"`.
- `asaas`: proof = payment id. Paid iff `GET /payments/{id}`
  returns `status` `RECEIVED` or `CONFIRMED`.

No SDKs: plain HTTPS with timeouts. Keys via env only
(`STRIPE_SECRET_KEY`, `ASAAS_API_KEY`); Asaas defaults to sandbox.
Price config: `SEAL_PRICE_CENTS` (default 2990), `SEAL_CURRENCY` (BRL).

## Future (only if the project advances)

Real checkout creation endpoints, webhooks, receipts, magic-link auth,
user accounts. The `PaymentPort` seam makes this additive.
