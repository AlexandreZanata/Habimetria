# Subscription and Account API — Planned Contract

These routes do not exist yet. Introduce them in small slices under `/api/v1`
with handler/service/store separation and update [API surface](../03-architecture/api-surface.md).
Reuse panel identity and authorization; browser cookie and mobile bearer
transports must not accidentally accept one another's CSRF/refresh tokens.

## Identity and required controls

| Method / suffix | Behavior |
|---|---|
| POST `/account/registrations` | Start rate-limited verified self-registration; neutral response |
| POST `/account/verification` | Consume short-lived single-use verification token |
| POST `/account/sessions` | Email/password login, optional required MFA challenge for staff |
| POST `/account/sessions/refresh` | Rotate device-scoped refresh secret; detect reuse |
| DELETE `/account/sessions/{id}` | Revoke own session/device; idempotent |
| POST `/account/recovery` | Neutral email recovery initiation |
| POST `/account/password` | Consume recovery proof; invalidate appropriate sessions |
| GET/PATCH `/account/profile` | Own minimal profile; reauthenticate sensitive change |
| POST `/account/exports` | Durable privacy export request; rate-limit without paywall |
| POST `/account/deletion` | Reauthenticated deletion request, status and next steps |
| GET `/account/requests/{id}` | Own request status; no paid entitlement required |

No token/email/secret in URLs except deliberately single-use verification
links; scrub query logs and referrers. Secure native storage holds refresh
secrets; mobile access tokens are short-lived, audience/scope checked.
Use explicit session revocation and current-role checks. Never introduce
staff access by minting an ordinary customer's session.

## Commerce

| Method / suffix | Behavior |
|---|---|
| GET `/commerce/catalog` | Eligible monthly plans and store identifiers for app/region; no authoritative client-price assumption |
| POST `/commerce/purchase_context` | Bind authenticated User to opaque store account identity; return eligible product context |
| POST `/commerce/purchases/google` | Verify purchaseToken and expected product; return durable delivery status |
| POST `/commerce/purchases/apple` | Verify signed transaction/context; return durable delivery status |
| GET `/commerce/entitlements` | Effective rights, validity, freshness, origin and quota version |
| POST `/commerce/restore` | Reconcile native proof with current User; never transfer another User's chain silently |
| GET `/commerce/subscriptions` | Own subscription states, renewal/end dates and store management information |
| GET `/commerce/usage` | Own hosted allowance/usage/reservations, period and unit |

Mutations require `Idempotency-Key` scoped to subject + operation and a payload
digest. A reused key with changed payload is a conflict. The server computes
ownership, grant and price context; ignore supplied User IDs or alleged paid
amounts as authority. Distinguish proof received, verified, granted and pending
acknowledgment; only verified durable grant enables new hosted consumption.
Transient verification failure returns retryable delivery state, not a paid grant.

Notifications use separate non-session routes `/billing/google/notifications`
and `/billing/apple/notifications` under the server router. Validate Google
Pub/Sub push authentication/audience/topic binding or supported pull delivery;
validate Apple JWS certificate chain/signature/app/environment. Reject
untrusted traffic; persist accepted deliveries before success acknowledgment.
These routes cannot be called as a client-side grant shortcut.

## Cloud contracts

Separate authenticated routes for enrollment, upload reserve/complete,
snapshot finalize/list, restore manifest, changes cursor and Share/revocation.
See [cloud sync](../03-architecture/cloud_sync_contract.md). Match ownership,
entitlement and reservation again at completion; a signed upload URL alone
does not establish authorized receipt. Pin API/schema versions.

## Errors and bounds

Use the repository error envelope and add stable machine-readable reason
codes: `authentication_required`, `not_authorized`, `subscription_required`,
`quota_exceeded`, `purchase_pending`, `purchase_account_conflict`,
`verification_unavailable`, `idempotency_conflict`, `unsupported_version`.
Map to 401/403/409/422/429/503 consistently; document cloud-only 402 if chosen
instead of 403. Never use payment-required on a local operation.
Include retryability/request reference without raw proof, email enumeration,
stack traces, private address or vendor secrets. Bound payload sizes, replay
windows, pagination/cursors, token attempts and restore/verification rates.

## Administrative HTTP

Panel mutations remain same-origin, authenticated and CSRF protected. Add
scoped handlers for catalog drafts, cost rates/snapshots, price publication,
transaction lookup/reconciliation, grants, cases and retention operations.
Each delegates to services and enforces capability + reason + reauthentication
where appropriate. No raw database editor or “mark paid” toggle.
See [administration](commercial_admin.md).

## Compatibility

Keep existing authorized API behavior until each route is migrated. Inventory
unclaimed/anonymous legacy paths; hosted production never accepts ownership
from an arbitrary request. Do not break public verification by blanket auth.
Document explicit response/version changes and test old client fixtures.
