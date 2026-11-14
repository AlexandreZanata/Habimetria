# Implementation Map — Expected Code Boundaries

Paths below identify existing integration points or proposed feature folders,
not files already implemented. Re-inspect actual layout before writing.
Each new code file must be indexed by its folder README/parent doc and stay
one concept/~200 lines. Use [commit breakdown](commit_breakdown.md).

| Slices | Primary integration points / proposed ownership |
|---|---|
| C01 | `backend/internal/service/seal.go`, `payment*.go`, `internal/config`, API wiring/tests, `infra/.env.example` |
| C02-C04 | `backend/internal/httpapi/router.go`, private handlers, `panelweb/legacy.go`, `service/panel`, PostgreSQL ownership guards; shared `vault/local` tests |
| C05 | Incremental `backend/migrations`, `service/commerce/catalog`, `store/postgres` catalog queries |
| C06-C08 | `internal/config`, `infra`, `.github/workflows/verify.yml`, `internal/testutil`; existing permission/audit modules |
| C09-C11 | `service/panel` credential reuse; proposed `service/account`, `httpapi` account handlers and `internal/mail` real adapter/outbox |
| C12 | Account session schema/service/API; `shared/.../vault/account`; Android Keystore/iOS Keychain adapters |
| C13, C16 | `composeApp/.../vault/ui` account screens, navigation/AppContainer; account-scoped sync cancellation |
| C14-C15 | Panel auth/forms/templates, account MFA/privacy services/stores; mobile/public rights UI |
| C17-C18 | Proposed `service/commerce` ledger/entitlement/grant files and matching PostgreSQL queries/migrations |
| C19-C20 | Billing notification handlers, event/job stores and proposed bounded worker executors; no HTTP in stores |
| C21-C23 | Commerce usage/quota/cost/pricing services, decimal/unit types, PostgreSQL; panel pricing forms/views |
| C24 | Proposed `internal/billing/google` and `internal/billing/apple` catalog adapters; price publication service/audit |
| C25 | `composeApp/src/androidMain` native billing adapter; `shared/.../vault/billing`; Gradle dependency catalog |
| C26-C27 | Google verification/lifecycle/authenticated RTDN adapter, commerce services/jobs/stores; Android restore/manage bridge |
| C28 | `app/iosApp`, actual new project/schemes/Info.plist/privacy resources; `Makefile` native verification path |
| C29 | Swift StoreKit bridge and `composeApp/src/iosMain`; shared billing interface/state |
| C30-C31 | Apple JWS/certificate/API/event adapter, commerce services/jobs/stores; Swift restore/management/refund bridge |
| C32 | Shared subscription UI/use cases; AppContainer/navigation; platform store-localized product presentation |
| C33 | Shared `vault/sync`, existing ServerMirror/UploadPump, local DB account-enrollment migration; cloud API ownership |
| C34 | Server Report/snapshot schema/query/service; preserve local ReportArtifacts/ReportPipeline compatibility |
| C35-C37 | Cloud transfer/snapshot/change services and stores; storage port; shared queue/sync repositories and local revisions |
| C38 | Shared restore/package verifier/import staging; native filesystem adapters and real snapshot API |
| C39 | Existing Share/verify handlers/services/stores; entitlement/retention guard and public redaction |
| C40 | `httpapi/panelweb` owned library/forms/views/assets; server Report version and recovery authorization |
| C41-C43 | Existing panel scopes/forms/templates/assets plus commerce/finance/support services and PostgreSQL stores |
| C44 | Privacy/retention worker, account and object-storage removal boundaries, minimized commercial claims and holds |
| C45-C48 | Structured telemetry/alerts, `infra`, recovery/load harnesses, security suites, costing and operator runbooks |
| C49-C50 | Real DB/store integration packs and shared local regression/independent verifier fixtures |
| C51-C54 | Android/iOS release artifacts, real-device/sandbox evidence, store metadata/privacy inventory, existing beta/user gates |
| C55-C56 | Versioned deployment/rollout and reviewed release evidence; protected dev->main PR/CI |
| C57-C59 | Organization/membership/seat schema and service authorization; monthly catalog/ledger extension; shared team UI |
| C60 | Actual selected remote-provider adapter, consented jobs/usage; shared suggestion review UI; free local assistant untouched |
| C61 | Actual selected trust-provider adapter and new versioned receipt format; independent verification fixtures |
| C62-C63 | Finance import/scheduling, provider-offer eligibility, pricing/admin surfaces and monthly store adapters |
| C64 | Requirements/progress/evidence plus complete regression and operating documentation |

Before any schema change, update [storage contract](../../04-backend/subscription_storage.md)
and the actual source-of-truth database docs. Before any route change, update
[API contract](../../04-backend/subscription_api.md) and the existing API surface.
Do not put business logic in a panel template, platform purchase callback,
SQL query or generic helpers file.
