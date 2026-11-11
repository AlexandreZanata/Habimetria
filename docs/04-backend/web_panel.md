# Habimetria web panel

The Go server embeds the Portuguese admin and user interface at `/painel/`.
It adapts the supplied Brand, building illustration, sidebar, cards, tables,
and forms as server-rendered templates. The stylesheet and Manrope assets
belong to this area. Browser state contains neither the account database nor
the authorization policy. No demonstration records or PDFs ship in the panel.

Planned commercial expansion: [administration](commercial_admin.md) and
[execution roadmap](../06-roadmap/commercial/README.md). Their subscription,
costing, self-service and cloud capabilities are not implemented by this panel
baseline; do not present the plan as production availability.

[Legacy ownership runbook](legacy_ownership_runbook.md) defines safe runtime
modes, rollback and individually reviewed assignment of existing NULL-owner
Properties without changing their sealed artifacts.

[Pending self-registration](account_registration.md) extends the same users.
Staff cannot activate a self-registration before email verification; existing
invitation/recovery behavior stays separate.
The [C10 authentication contract](account_authentication.md) defines working
signup/verification forms, JSON login and opt-in proof-only API deployment.

## Run locally

Use PostgreSQL 18.6 and the existing Go API configuration. The panel is
opt-in. Keep the origin exact, including its port, with no trailing slash.

```sh
PANEL_ORIGIN=http://localhost:8080 docker compose -f infra/docker-compose.yml up --build -d
docker compose -f infra/docker-compose.yml exec api \
  /panel-owner owner@example.test "Platform Owner"
```

The command prints a secret activation link. Open it in a private browser
session and set a password of at least 15 characters. For a host-run API,
set the same `PANEL_ORIGIN` and `DATABASE_URL` when running
`go run ./cmd/api` and `go run ./cmd/panel-owner <email> <name>` from
`backend/`. HTTP is accepted for local loopback origins. Use an HTTPS
origin, `APP_ENV=production`, a random HMAC key of at least 32 bytes, and TLS termination on
the public origin when hosting the panel.

Authorization expansion: [scoped administrative capabilities](admin_authorization.md).

## Identity, permissions and audit

- The operator bootstraps one primary owner. Re-running the command for
  that same email issues a new activation link and allows the primary owner
  to set a new password. Completing that reset invalidates earlier sessions.
- Staff with account-write permission can create ordinary accounts and
  issue or reissue links while activation is pending. Owners also manage
  team accounts and the role matrix. The panel generates links for manual
  delivery. Self-registration uses the separate C09 SMTP worker and C10 proof
  flow; [C11 recovery](account_recovery.md) adds customer-owned password recovery.
- A link expires after 30 minutes and is consumed atomically. Activated
  accounts authenticate with their own password. Staff cannot obtain a login
  link or reset an activated ordinary account through the panel. Legacy
  accounts are ineligible for staff activation links.
- Password credentials use versioned, salted PBKDF2-HMAC-SHA256 with
  600,000 iterations. The server limits authentication attempts by source
  address and normalized email. Reverse proxies share their connection
  address in this limiter. Only accept traffic through a trusted frontend
  when configuring deployment-specific rate limits.
- Sessions expire after 12 hours. The database stores token digests and
  checks active accounts and current capabilities on each request. Cookies
  are HTTP-only, same-site, and secure on HTTPS. Mutations require the
  configured Origin. Role and active-state changes revoke affected sessions.
- Support can receive consultation permissions. Account writes are unavailable
  to support. Owners can manage roles and staff. The primary owner cannot
  be demoted or suspended. Account and role updates serialize in PostgreSQL,
  and a database constraint protects the active primary owner.
- Users, search, paginated CSV, overview counts and activity use server
  scope. Activity records real account and permission events with server
  instants. It omits private property, inspection and report activity.

## Properties, inspections and documents

- The user area lists Properties, Inspections and Reports owned through
  `properties.owner_user_id`. An owned Property can start an entry Inspection
  in DRAFT through an atomic Tenancy and Inspection creation.
- Private legacy API routes require the Property owner's current session,
  including when the panel is disabled. Unclaimed rows are excluded by default;
  the panel creates owned Properties. `LEGACY_API_MODE=unclaimed-development`
  explicitly restores unclaimed API work only for isolated development with
  a local HTTP verification origin and no panel. Production rejects this mode.
  The exception never grants anonymous access to owned resources.
- The mobile-only collection stays on its device. The panel does not claim
  unowned records or automatically synchronize local inspections or PDFs.
  Capture, seal, PDF rendering and upload continue through their existing
  authorized domain flows. The web UI creates drafts and reads real records.
- A Report is available to staff only when the titleholder granted access
  to that exact Report and staff has the current report-read permission.
  The titleholder can revoke staff access. The next request checks the grant
  again. Bytes delivered previously cannot be recalled.
- Preview opens the authorized PDF in the browser's viewer. Download uses
  the same immutable object with an attachment disposition. There are no
  separate public thumbnails. Browser PDF support determines inline
  presentation. Public verification and explicit public Share links retain
  their existing contracts and are separate from staff availability.
- Server seal remains terminal. The first uploaded PDF is fixed for its
  server Report ID. Identical retries are idempotent. Changed bytes are
  rejected and the existing PDF stays available. The panel does not generate
  or certify a PDF, author identity, receipt or agreement.

## Files and checks

C11 adds `panelweb/account_services.go`, `recovery_forms.go`, `event_labels.go`
and `views/recovery.html`, retaining the existing embedded identity and form
styles. Recovery tests and activation details are indexed in the
[recovery contract](account_recovery.md).

- `backend/migrations/0005_panel.sql` extends identity and adds activation
  links, sessions, limits, permissions, report availability and audit.
  To roll back exposure, unset `PANEL_ORIGIN` and retain the migration and
  ownership guard so owned data stays private.
- `backend/internal/store/postgres/panel_{identity,login,accounts,policy,properties,reports,legacy}.go`
  contains credential, policy, resource and audit queries.
- `backend/internal/service/panel/{service,password,accounts,vault}.go`
  validates authentication, account and document use cases.
- `backend/internal/httpapi/panelweb/{web,auth,pages,forms,documents,legacy}.go`
  registers, authenticates, renders and protects the routes. Templates in
  `views/{layout,access,overview,accounts,settings,vault,reports}.html`
  render each section. Assets include `base.css`, `components.css`,
  `access.js`, the monogram `favicon.svg`, and the four Manrope TTF weights
  and `OFL.txt` in `assets/fonts/`.
- `backend/cmd/panel-owner/main.go` is the operator bootstrap and primary
  recovery command. The API image includes both binaries.
- `backend/internal/httpapi/panelweb/integration_test.go` checks activation,
  login, revocation, server-side permissions, duplicate email, and primary
  owner protection. `documents_integration_test.go` checks ownership,
  isolation, availability, PDF byte consistency and terminal seal.
- `seal_integration_test.go` checks no-proof/ignored-legacy-proof sealing,
  ownership, concurrent requests, empty validation and frozen history.
- `route_policies.go`, `routes.go` and `route_policies_test.go` bind/test concrete
  classified panel routes; `boundary_fixture_test.go` and
  `boundary_integration_test.go` prove all API owner scopes, private panel
  reads and non-paid-gated account/recipient controls against real PostgreSQL.
- `legacy_ownership_integration_test.go` checks every owner route/HEAD with
  panel enabled/disabled, unclaimed records, forged worker/owner claims and
  authorization failure. `legacy_test.go` covers absent authorization;
  `config/legacy_api_test.go` pins secure defaults and development restrictions.
- `comparison_ownership_integration_test.go` rejects cross-owner/wrong-side
  Evidence references and blocks reads of legacy invalid comparison rows.
- `legacy_compatibility_fixture_test.go` and `legacy_compatibility_integration_test.go`
  exercise real development API uploads/seal/PDF/Share and secure mode transitions.
- `legacy_revocation_integration_test.go` checks current session/role/capability
  authority; `legacy_assignment_integration_test.go` rehearses reviewed ownership
  assignment, immutable artifacts, audit and no automatic bootstrap claims.

Run `make verify` and `make secrets`. Run `make test-integration` with
`TEST_DATABASE_URL` pointing at a disposable PostgreSQL 18.6 instance.
The panel suite creates independent databases and needs a test role with
permission to create databases. No test uses production data.

Password parameters follow the [OWASP password storage guidance](https://cheatsheetseries.owasp.org/cheatsheets/Password_Storage_Cheat_Sheet.html).
