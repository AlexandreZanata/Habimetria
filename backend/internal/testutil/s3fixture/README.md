# Explicit private S3 fixture

`store.go` validates required TEST-only settings and creates the actual S3
adapter. No S3/AWS production variable or default localhost endpoint is read.
An absent optional endpoint skips; incomplete/non-loopback settings fail.

Used by `storage/s3store/private_integration_test.go`, collection migration
qualification and the panel's storage isolation test. Provision with
`make verify-infra`; [contract](../../../../docs/04-backend/integration_fixtures.md).
