# Conventions — Naming, Size, DoD

- Docs: `snake_case.md`, one topic, ≤200 lines.
- Go: `handler/service/store` split. `errFoo` wrapped with `%w`.
- Kotlin: feature-first, `commonMain` logic, platform adapters thin.
- SQL: one version per file `0001_*.sql`, `0002_*.sql`.
- Env: `infra/.env.example` only. Never commit `.env`.
- DoD: `make verify` green + docs updated + small diff.
