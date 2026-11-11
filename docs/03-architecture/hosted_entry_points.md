# Hosted Entry Points — C02 Authorization Inventory

Inspected 2026-11-11 at `7f992cc`. This inventory covers existing runtime
entry points, not planned subscription routes. Product authority:
[free local / monthly hosted](../02-product/service_monetization.md).
Execution/evidence: [C02](../06-roadmap/commercial/c02_execution.md).

## Transport classifications

| Class | Authority and commercial treatment |
|---|---|
| public_control | Operational health, static assets and existing account entry/exit; no subscription |
| account_control | Authenticated own account/profile; never paid-gated |
| hosted_owner | Owner-scoped hosted collection operation; future monthly entitlement/quota in addition to ownership |
| staff_control | Authenticated staff/capability operation; existing server RBAC, no customer subscription gate |
| report_access | Authenticated titleholder or explicit Report grant plus current capability; future hosted/recovery policy |
| public_recipient | Verification-code/share-token lookup; publisher-funded service, recipient never required to subscribe |
| delegated | `/painel/` mount only; child router must classify/enforce each concrete route |

Classifications do not grant subscriptions, replace ownership or certify
Evidence. Account recovery/rights requests and contractual data recovery must
remain non-paid-gated when introduced; their endpoints do not exist yet.
Public does not mean unrestricted content: code/token validation, privacy
minimization and resource existence checks remain in the relevant service.

## API registrations

There are 24 owner-scoped API operations, nine explicit public operations, two
account controls and
one optional delegated panel mount. GET policies also cover Go's implicit HEAD.
All private scope strings resolve the stable Property owner, never an owner
supplied in the body/query. Unknown scopes are errors, not public fallbacks.

| Method and path | Class | Owner lookup scope |
|---|---|---|
| GET `/healthz`, GET `/readyz`, GET `/api/v1/ping` | public_control | None |
| POST `/api/v1/account/registrations`, `/api/v1/account/verification`, `/api/v1/account/sessions` | public_control | Bounded neutral initiation or purpose/credential validation and shared throttling |
| GET `/api/v1/account/profile`, DELETE `/api/v1/account/sessions/current` | account_control | Current ordinary User's API bearer; no cookie or paid gate |
| POST `/api/v1/properties` | hosted_owner | properties; creation policy in guard |
| POST `/api/v1/properties/{id}/tenancies` | hosted_owner | properties |
| POST `/api/v1/tenancies/{id}/inspections` | hosted_owner | tenancies |
| POST `/api/v1/inspections/{id}/rooms` | hosted_owner | inspections |
| GET `/api/v1/inspections/{id}/rooms` | hosted_owner | inspections |
| POST `/api/v1/inspections/{id}/rooms:ensure-template` | hosted_owner | inspections |
| POST `/api/v1/inspections/{id}/evidences` | hosted_owner | inspections |
| GET `/api/v1/inspections/{id}/evidences` | hosted_owner | inspections |
| GET `/api/v1/inspections/{id}/evidences/download-url` | hosted_owner | inspections; Evidence ID checked inside that Inspection |
| POST `/api/v1/rooms/{id}/findings` | hosted_owner | rooms; Evidence association also validated |
| GET `/api/v1/rooms/{id}/findings` | hosted_owner | rooms |
| GET `/api/v1/inspections/{id}/coverage` | hosted_owner | inspections |
| POST `/api/v1/inspections/{id}/seal` | hosted_owner | inspections; no per-seal payment |
| GET `/api/v1/reports/{id}` | hosted_owner | reports |
| POST `/api/v1/reports/{id}/pdf` | hosted_owner | reports; finalized bytes immutable |
| GET `/api/v1/reports/{id}/download-url` | hosted_owner | reports |
| POST `/api/v1/reports/{id}/shares` | hosted_owner | reports |
| GET `/api/v1/reports/{id}/shares` | hosted_owner | reports |
| POST `/api/v1/properties/{id}/occurrences` | hosted_owner | properties |
| GET `/api/v1/properties/{id}/occurrences` | hosted_owner | properties |
| POST `/api/v1/occurrences/{id}/evidences` | hosted_owner | occurrences; attached Evidence must belong to same Property |
| GET `/api/v1/occurrences/{id}/evidences` | hosted_owner | occurrences |
| POST `/api/v1/tenancies/{id}/comparisons` | hosted_owner | tenancies; entry/exit Rooms checked |
| GET `/api/v1/tenancies/{id}/comparisons` | hosted_owner | tenancies |
| GET `/v/{code}` | public_recipient | Verification-code lookup; seal anchors only |
| GET `/s/{token}`, GET `/s/{token}/pdf` | public_recipient | Explicit Share-token lookup; authorized PDF delivery |
| `/painel/` | delegated | Concrete child policies below |

## Existing panel routes

All mutations retain exact-Origin/form bounds; sessions/capabilities use the
database. CSV query variants retain their section's policy; `?baixar=1`
retains document authorization. Neither query parameter is an access grant.

| Method and concrete path | Class | Additional current enforcement |
|---|---|---|
| GET `/painel/`, GET `/painel/assets/` subtree | public_control | Login rendering/static files; no hosted collection access |
| GET `/painel/entrar`, GET/POST `/painel/cadastro`, GET/POST `/painel/verificar` | public_control | Shared signup/verification services; exact-Origin POST, no GET consume |
| POST `/painel/entrar`, POST `/painel/ativar` | public_control | Credentials/token purpose, expiry and rate limits |
| POST `/painel/sair` | public_control | Session revocation/cookie cleanup, including expired sessions |
| GET/POST `/painel/perfil` | account_control | Own profile only |
| GET `/painel/minha-area` | hosted_owner | Owner-scoped counts |
| GET/POST `/painel/imoveis` | hosted_owner | Own Properties/new DRAFT creation |
| GET `/painel/vistorias` | hosted_owner | Own Inspections |
| GET/POST `/painel/relatorios` | hosted_owner | Own Reports and deliberate staff availability/revocation |
| GET `/painel/administracao` | staff_control | Existing overview role rule |
| GET/POST `/painel/usuarios`, GET/POST `/painel/equipe` | staff_control | Account/team capabilities and primary-owner protections |
| GET `/painel/disponibilizados` | staff_control | Current reports.read plus exact Report grant |
| GET `/painel/atividade` | staff_control | Existing activity capability/scope |
| GET/POST `/painel/configuracoes` | staff_control | Owner-managed permission matrix |
| GET `/painel/documentos/{id}` | report_access | Titleholder or current reports.read and exact titleholder grant |

The registry binds these 28 child routes concretely. Adding an unknown
page or method must not inherit a generic section route's access implicitly.

## Runtime registration and proof

- `backend/internal/httpapi/route_policies.go` declares the API catalog;
  `router.go` registers only its classified entries.
- The shared [routepolicy package](../../backend/internal/httpapi/routepolicy/README.md)
  rejects unclassified/duplicate/invalid catalogs at startup. Missing private
  guards (including a guard returning nil) produce 503 without handler execution.
  Public controls/recipients bypass private guards. `Rules()` returns a detached
  sorted inventory, not a mutable authorization policy or public HTTP endpoint.
- Panel `route_policies.go` and `routes.go` bind concrete sections. Private
  sections require a database-verified session; existing service RBAC/ownership
  and exact Report grants still decide resource access. The verified actor is
  cached only within that request, never across requests or role revocation.
- `route_policies_test.go` in API and panel packages pins public allowlists,
  scope coverage and concrete section registration. The shared package tests
  guard absence, invalid registration, HEAD and detached inspection behavior.
- Panel `boundary_fixture_test.go` and `boundary_integration_test.go` exercise
  all 24 owner routes with real DB records: anonymous/other/staff denial,
  owner reads, body/query identity injection, account controls and valid/unknown
  recipient tokens. Existing document tests still cover Report grant/revocation.

## Mobile and background entry points

- `vault/api/VaultApi.kt` maps mobile HTTP calls onto the owner/recipient
  routes above. It has no authenticated account binding yet. Signed-object
  downloads are delegated short-lived storage capabilities after authorized
  issuance; URL possession does not grant collection ownership.
- `vault/queue/UploadPump.kt` reads queued bytes, posts Evidence and marks
  confirmed only after the server acknowledgment. It is at-least-once and
  currently lacks request idempotency; timeout-after-store duplicates are a
  known later cloud-delivery concern, not fixed by route classification.
- `vault/sync/ServerMirror.kt` mirrors Occurrence, exit Inspection and comparison
  through their existing owner routes. LOCAL_ONLY returns without network.
  DB mirror links preserve local/remote mapping; they are not entitlement.
- Android `UploadWorker.kt` builds VaultApi and drains the same queue;
  `scheduleIfAllowed` gates LOCAL_ONLY. Its lower-level `schedule`/`doWork`
  are not account/entitlement checks. Revalidate owner/enrollment in later
  session/sync slices; no worker can bypass server route authorization.
- Android MainActivity and iOS MainViewController currently open LOCAL_ONLY
  sessions. AppContainer exposes optional remote seams with a separate sync
  mode; never wrap local repositories in a hosted subscription gate.
- C09 adds the optional durable `backend/cmd/account-mail` worker with bounded
  leases and proof/identity rechecks. Planned billing/retention workers need
  explicit subjects, capabilities and replay policies when implemented.

## C02 boundary and remaining work

C02.a records this inventory. C02.b binds declared API/panel policies and
rejects unknown classification/missing authorization at registration/runtime.
Ownership and current panel RBAC remain service-backed, with two-User proof.
This does not introduce account sessions into mobile clients or paid rights.

The C04 guard defaults to owned resources independently of panel exposure.
Unclaimed API work requires explicit validated `unclaimed-development` mode
with local HTTP origins and no panel; production rejects that exception.
Existing owned resources always require their owner's current session.
Required monthly entitlement/quota
enforcement is C18/C21 and related cloud steps. Documented funding class is
not a claim those services are already monetized or production-qualified.
