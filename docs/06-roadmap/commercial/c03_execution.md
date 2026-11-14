# C03 Execution — Complete Free Local Journey

Started 2026-11-11 at `f9d7d97`, branch dev. Scope C03 only, ordered sub-slices
from [commit breakdown](commit_breakdown.md). C04-C64 remain pending.

## C03.a — Reusable fixture and durable core

Added a synthetic file-backed fixture using actual LocalSession repositories,
original storage and installation signing. Three contexts run the same work:
no account/API adapter, expired hosted access with a previous persisted mirror
link and a rejecting HTTP adapter, and offline with an unavailable adapter.
No mobile account or billing ledger is invented: real lifecycle integration
remains in its planned slices. Local operations construct no billing adapter.

The regression captures 17 originals in two manually named Rooms, preserves
Findings/report details/navigation over restart, checks distinct-category
coverage and safety bounds, then verifies terminal signed sealing and byte
hashes over another restart. Optional mirror operations must be skipped with
zero HTTP requests; previous links cannot grant or fabricate confirmation.
The synthetic JPEG digest is fixed; randomized IDs/device seal time are not
misrepresented as a deterministic Manifest hash across independent runs.

Executed checks:

- `./gradlew :shared:jvmTest --tests 'vault.local.FreeLocal*'` from app: PASS,
  all three core contexts.
- `make verify-mobile`: PASS (all shared JVM tests, shared iOS Simulator
  Kotlin compilation, Android debug assembly).
- `make verify`: PASS. DB-specific suites skip without TEST_DATABASE_URL;
  this is not new PostgreSQL integration evidence.
- `make secrets`, `git diff --check`, local documentation links/new-file
  references and changed-file line limits: PASS.

Commit `5a22116913195a9e8a43d7c4b849d39cbbac2239` was pushed to origin/dev.
[CI run 353](https://github.com/AlexandreZanata/Habimetria/actions/runs/43024308547)
passed backend and mobile-linux. This sub-slice alone does not complete
C03.b or claim real purchase-expiration tests.

## C03.b — Artifacts and complete local regression

Extended all three contexts through real shared layout/photo-source processing,
Report publication/receipts, versions, ZIP export/import and verification.
The drawing port uses an explicit test double: artifact bytes are synthetic,
so passing tests do not qualify native PDF rendering, visual layout or sharing.
The assistant cannot create Findings until explicitly confirmed; ignored
suggestions and entry/exit comparison tags survive restart. All four explicit
declaration intents are exported without modifying the original Report,
receipt, Manifest or original-photo hashes. Historical ZIPs remain valid.

Fresh-recipient verification keeps an included signer UNKNOWN until independently
pinned. Equal-length tampering is rejected without conflating source integrity
with unchanged PDF/signature checks. A JVM regression deletes the entire issuer
database/private-file directory before import and runs the independent Go
verifier: original artifacts must pass and a modified PDF must fail. The shared
Go invocation also retains the existing certificate declaration regression.

Executed checks:

- `./gradlew :shared:jvmTest --tests 'vault.local.FreeLocal*' --tests
  'vault.local.DeclarationInteroperabilityTest'` from app: PASS. Both independent
  verifier scenarios preserve strict stdout JSON parsing and tamper rejection.
- `make verify-mobile`: PASS, 216 shared JVM tests with no failures/skips,
  shared iOS Simulator Kotlin compilation and Android debug assembly.
- `make verify`: PASS; no new PostgreSQL integration claim without a DB.
- `make secrets`, `git diff --check`, local document links/new-file references
  and changed-file limits: PASS.

Commit/ref and exact-commit CI are checked through Git/PR after push; this
file does not invent its own not-yet-known hash. No native store expiration,
account lifecycle or device gate is inferred. Real account/billing event
matrices still require their later implementations and C50 acceptance work.

## Manual gates

G01-G12 remain UNVERIFIED. No native rendering, device, user-session, legal
or store approval is granted by these automated local-core regressions.
