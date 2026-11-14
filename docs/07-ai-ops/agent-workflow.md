# Agent Workflow — Slice by Slice

1. Read AGENTS.md and the active plan. For commercial implementation use
   [commercial runbook](../06-roadmap/commercial/agent_runbook.md), not the
   historical MVP scope.
2. Inspect branch/status/HEAD and relevant docs/code; work only on `dev`.
   Preserve unrelated work. Do not create feature branches or stacks.
3. Implement one small slice plus meaningful tests and incremental migration
   where needed. Respect existing domain and immutable history.
4. Run `make verify` and applicable real-DB/mobile/native/store checks. Fix
   failures; skipped integration is not a pass.
5. Update contracts, folder references and progress/evidence honestly.
6. Review/stage intended files, commit `type(scope): verb object`, push to
   GitHub and inspect exact-commit CI. Never finish a slice unpushed.
7. Maintain a small English `dev -> main` PR after applicable checks; never
   commit directly to main or bypass protected required checks/review.
8. Record device/visual-PDF/iOS/user/legal/store approval gates as unverified
   until their responsible owner provides actual evidence.

Keep `.github/workflows/verify.yml` green: backend (`vet`, `test`,
`test-integration` on Postgres 18.6, fixture verifier) + `verify-mobile`.

Rules: max ~200 lines/file, one concept/file, English code+docs, no secrets, no giant PRs.
