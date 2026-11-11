# Connected account registration

`registration.go` validates neutral pending registration and creates encrypted
verification mail atomically through the PostgreSQL store. The master key
is never a panel/Manifest signing key. No password/session is set before C10
verification, no existing identity is replaced and no raw proof is returned.

`registration_integration_test.go` and `registration_safety_test.go` cover real
PostgreSQL duplicates, cooldown/limits, rollback and identity isolation.
Normative [contract](../../../../docs/04-backend/account_registration.md).

`delivery.go` claims bounded leases, rechecks current proof/identity, sends outside
transactions and records only guarded durable results. `delivery_integration_test.go`
`delivery_limits_test.go` and `delivery_recovery_test.go` cover actual SMTP and failure/reclaim boundaries.
