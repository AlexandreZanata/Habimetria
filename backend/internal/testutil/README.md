# Synthetic test infrastructure

- [testutil.go](testutil.go): deterministic fake identifiers, instants and keys.
- [database.go](database.go): isolated per-test PostgreSQL database with explicit
  migration setup and cleanup. TEST_DATABASE_URL must name disposable test
  infrastructure with database-creation permission; no production fallback.

- [s3fixture](s3fixture/README.md): explicit loopback-only real S3 configuration.
- [collectionfixture](collectionfixture/README.md): real-service synthetic owned/legacy
  collections, immutable snapshots and migration qualification.

- [mailfixture](mailfixture/README.md): explicit loopback SMTP and bounded capture
  polling without production defaults, HTTP redirects or inherited proxies.
