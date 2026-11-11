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

This sub-slice does not complete C03.b or claim real purchase-expiration
tests. Commit/ref/CI are checked after push; its own commit hash is recorded
in the next sub-slice instead of guessing a self-referential identity.

## Manual gates

G01-G12 remain UNVERIFIED. No native rendering, device, user-session, legal
or store approval is granted by these automated local-core regressions.
