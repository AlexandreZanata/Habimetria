# Free local regression fixtures

- [FreeLocalFixture.kt](FreeLocalFixture.kt): reusable synthetic graph using real
  SQLite, original files, installation signing and session restart/cleanup.
- [FreeLocalNetwork.kt](FreeLocalNetwork.kt): no-adapter, expired-hosted-adapter
  and offline contexts; optional mirror calls must produce zero HTTP requests.
- [FreeLocalCoreTest.kt](FreeLocalCoreTest.kt): durable capture, findings,
  coverage, safety validation and terminal signed sealing in every context.

Run `./gradlew :shared:jvmTest --tests 'vault.local.FreeLocal*'` from `app`.
These are code regressions; account/store lifecycle and native device gates
remain separate. See [C03 execution](../../../../../../../docs/06-roadmap/commercial/c03_execution.md).
