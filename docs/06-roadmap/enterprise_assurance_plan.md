# Enterprise Assurance Plan — Tests, Scale and LGPD for Municipal Use

Status: planning only; no suite below is implemented. Baseline: current
`main` (198 JVM tests / 65 suites, 26 Go test files, `make verify`,
`verify-mobile`, `test-integration` on real PostgreSQL 18.6, `bench`,
`bench-apk`). Target: a municipal (prefeitura) enterprise bar for a
tenancy-vistoria system holding citizen personal data. Law: LGPD Lei
13.709/2018. This plan defines code-provable gates; legal sign-off stays
counsel-owned and is never claimed by tests.

## Principles

- Business-logic first: every suite maps to a domain rule in
  `docs/03-architecture/domain-model.md` or a product flow in
  `docs/02-product/`. No coverage theater.
- Synthetic fixtures only: no real CPF, address, photo, location or key in
  any test, seed or benchmark. Fixture factory shared by Go and Kotlin.
- Failure injection over happy paths: each new suite must include the
  hostile case (corrupt bytes, clock skew, denied permission, full disk,
  killed process, hostile proxy, expired token).
- Budgets are gates, not wishes: every benchmark has a number, a workload
  and a machine class; regressions fail CI or the nightly job.
- Honesty preserved: offline UNKNOWN stays UNKNOWN; device claims stay
  device claims; manual/device/counsel gates stay explicitly unverified.

## T1 — Business-logic suites (per domain rule)

Close rule-by-rule gaps on top of existing suites (seal terminality,
manifest bytes, PDF completeness, declarations chain, removal/restore,
tamper rejection, V1.1–V1.5, V-sync mirror).

| Area | New suites (names) | Gate |
|---|---|---|
| Tenancy lifecycle | `TenancyLinkTest` (entry/exit pairing, no dummy tenancy, overlap rejection) | overlap/orphan rejected |
| Coverage honesty | `CoverageCategoryTest` (distinct shot kinds, repeated overview never completes, N/A handling) | false completion impossible |
| Findings | `FindingEvidenceBindingTest` (preset taxonomy, note bounds, finding survives room rename, orphan finding blocked) | binding enforced |
| Seal eligibility | `SealEligibilityTest` (unreadable original, unhashed evidence, empty room set, double-seal race) | seal refused with reason |
| Occurrence/addendum | `OccurrenceChainTest` (sealed snapshot immutable, occurrence links, exit repeat photos) | history append-only |
| Compare/inspector | `CompareTagTest`, `AiSuggestionTest` (tags never assign liability, ignore persists) | no liability text |
| Share/verify | `ShareTokenTest` (token entropy/uniqueness, revoked link 404s, access-log trio created/accessed/downloaded) | no token reuse |
| Paywall seam | `PaywallOffTest` (mode off seals freely, mode on requires proof, stub never charges) | off by default |

## T2 — Integration tests (real dependencies)

- API contract per route (`properties` → `verify`): status codes, error
  shape, auth boundary (Bearer required vs public `/v/{code}`,
  `/s/{token}`), IDOR (inspector A cannot read inspector B's draft),
  pagination bounds, idempotency keys on create/upload/seal.
- Real PostgreSQL 18.6 (existing job): all 4 migrations replay v1→head
  with fixture rows; downgrade path documented where reversible;
  concurrent seals on distinct inspections; unique-key races.
- Real S3 path via MinIO service: multipart upload, size/mime/magic-byte
  validation, signed-URL expiry, object-not-found mapping, bucket outage
  → explicit failure (never silent loss).
- Storage failure matrix: disk-full mid-write (atomicity), corrupt queue
  surfacing, DB/file state reconciliation job with tests.
- Clock discipline: server `received_at_server`/`sealed_at` from server
  clock only; device-clock skew ±24 h never leaks into server fields.

## T3 — Regression and goldens

- Golden fixtures versioned under `backend/testdata/` and
  `app/shared/schemas/`: canonical manifest bytes, paginated page map,
  claim/stamp text, `fixture-package` extended to photo-without-GPS,
  mocked-GPS and EXIF-divergence variants; both verifiers must accept.
- Coverage ratchet: per-package floor in CI (backend `service`,
  `httpapi`, `store`; mobile `vault.local`); ratchet only rises.
- Flake quarantine policy: 3 strikes → quarantined with issue link, never
  silently retried; quarantine list reviewed weekly.
- `go vet`, `gofmt`, `detekt`/`ktlint` (new) and Android lint in CI;
  warnings fail the build.

## T4 — Benchmarks and scalability (prefeitura capacity)

Measured on stated runners (Linux CI) plus the reference mid-range device
for device budgets. Soak: 30 min at peak before release.

| Benchmark (new `make bench-*`) | Workload | Budget |
|---|---|---|
| `bench-api` p95 per route | 200 concurrent inspectors, mixed read/write | reads <200 ms, seal <2 s, upload 5 MB <5 s |
| Concurrent seals | 50 simultaneous seals, distinct inspections | 0 lost updates, 0 double manifests |
| Manifest 1k evidences | canonical JSON + SHA-256 | <500 ms CI runner |
| Report render 100 photos | existing `PerfProbe` + device pass | <10 s device, bytes identical |
| List 500 properties / 2k inspections | bounded queries, lazy lists | warm switch p95 <200 ms, no full-original decode |
| DB growth | 100k evidences + 10k reports | index plan reviewed, pgbench baseline stored |
| APK/AAB size | existing `bench-apk` + per-ABI split | ≤15 MB, delta commented on PRs |
| Cold start / shutter | reference device, release build | <1.5 s / <300 ms |

Capacity model documented: inspectors × inspections/day × photos →
storage, DB rows and bandwidth for 12 months; scale-out note (read
replicas, object storage lifecycle) without implementing it.

## T5 — LGPD verification (Lei 13.709/2018)

PII in scope: CPF/name/contact, addresses, tenant/contracting-party data,
CREA/CAU + ART/RRT identity, evidence photos (may incidentally show people
or sensitive contexts), GPS trails, device timestamps, certificate
subjects, declaration authorship, `ip_hash` access records.

| Obligation | Code gate |
|---|---|
| Necessity/minimization (art. 6) | `PiiInventoryTest`: allowlisted PII fields per table/endpoint; new PII field fails CI until registered with purpose |
| Masking (existing CPF) extended | names/contacts masked in lists, PDFs and public views; full value only in explicit complete-data export |
| Public views (`/v`, `/s`, shared PDF) vs LAI | `PublicRedactionTest`: no CPF, contact, exact GPS or interior-photo leak beyond the redaction policy; policy file versioned |
| Subject rights (arts. 18–19) | export/portability endpoint + test; correction flow; consent/intent receipts stored with declarations |
| Erasure vs immutable seal (arts. 16, 18 VI) | architectural decision first: PII-minimized sealed payload + keyed-PII vault enabling erasure without breaking hash chains; `ErasureTest` proves post-erasure verification semantics; counsel approves the semantics |
| Retention/disposal (arts. 15–16) | retention table per record class (municipal archive law input); scheduled disposal job + test; legal-hold flag blocks disposal |
| Anonymization (arts. 5 XI, 12) | irreversibility test on published aggregates (re-identification attempt by quasi-identifiers must fail) |
| Security (art. 46) | encryption at rest (PG + volumes) and in transit (TLS) checks in deploy tests; key rotation drill; secret-scan in CI; `HMAC_KEY`/JWT handling tests |
| Breach drill (art. 48) | runbook + tabletop record: detection → containment → ANPD + subject notice draft; log redaction verified (no PII in app logs: `LogRedactionTest`) |
| Processor records (art. 37) | processing-records file (data, purpose, basis, retention, operators) kept next to code and tested for staleness vs migrations |

Counsel-owned, never code-claimed: legal bases per flow, RIPD (art. 38),
ANPD notifications, operator contracts, DPO designation (mandatory for
public entities), retention periods under municipal archive law.

## T6 — Security testing

- AuthN/Z: magic-link issuance/attempt limits, JWT expiry/scope/audience,
  refresh/reuse rejection, public-route allowlist test (no auth drift).
- Input: SQL/LDAP/path-traversal payloads, oversized bodies, hostile
  filenames, polyglot image upload (magic-byte vs extension), EXIF bomb
  bounds, JSON depth limits.
- Rate limiting + lockout on auth and public verify/share endpoints;
  abuse test asserts 429, not 500.
- Supply chain: `govulncheck` and Gradle dependency audit in CI,
  SBOM per release, pinned actions/runners, signed release artifacts.
- Headers/TLS: security headers test, HSTS/min-TLS in deploy check.

## T7 — Enterprise operations

- Migration safety: forward replay + documented rollback per file;
  zero-downtime pattern (expand→migrate→contract) for locking changes.
- Backup/restore drill quarterly: PG PITR + object-storage versioning,
  restore rehearsal timed against RTO/RPO declared with the prefeitura.
- Observability: structured logs (PII-free), RED metrics per route,
  tracing across API→DB→storage, dashboards and alert thresholds;
  on-call runbook per alert.
- Release engineering: versioned artifacts, changelog from commits,
  staged rollout with kill-switch, `verify.yml` required on `dev`→`main`.
- Audit pack for controladoria: one command exporting evidence
  (test reports, SBOM, access-log digest, retention/disposal log).

## Execution order and gating

E1 foundations (fixtures factory, lint, ratchet, secret-scan) →
E2 business suites T1 → E3 integration T2 → E4 goldens/regression T3 →
E5 benchmarks T4 (nightly + device) → E6 LGPD T5 (with counsel checkpoints
before erasure/retention semantics) → E7 security T6 → E8 ops T7.
CI runs E1–E4 + unit scope of E5; nightly runs full T4/soak; device lab and
tabletop drills stay manual with dated records. No gate is marked passed
without its artifact attached.
