# Agent Workflow — Slice by Slice

1. Pick smallest slice in `mvp-01.md`. Read its docs only.
2. Create branch `feat/s1-health`.
3. Add code + migration (if needed) + test.
4. Run `make verify`. Fix.
5. Update docs if contract changed.
6. Commit `feat(api): add health check`. Open small PR.

Rules: max ~200 lines/file, one concept/file, English code+docs, no secrets, no giant PRs.
