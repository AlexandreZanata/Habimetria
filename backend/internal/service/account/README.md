# Connected account registration

`registration.go` validates neutral pending registration and creates encrypted
verification mail atomically through the PostgreSQL store. The master key
is never a panel/Manifest signing key. No password/session is set before
verification, no existing identity is replaced and no raw proof is returned.

`registration_integration_test.go` and `registration_safety_test.go` cover real
PostgreSQL duplicates, cooldown/limits, rollback and identity isolation.
Normative [contract](../../../../docs/04-backend/account_registration.md).

`verification.go` and `verification_integration_test.go` implement atomic proof
consumption and credential enrollment. The
[authentication contract](../../../../docs/04-backend/account_authentication.md)
also indexes the PostgreSQL transaction, migration and shared password package.

`authentication.go` reuses panel credential verification and issues a separate
short-lived ordinary-User API audience. `authentication_integration_test.go`
tests transport separation, expiry, revocation and stale-credential denial.

`delivery.go` claims bounded leases, rechecks current proof/identity, sends outside
transactions and records only guarded durable results. `delivery_integration_test.go`
`delivery_limits_test.go` and `delivery_recovery_test.go` cover actual SMTP and failure/reclaim boundaries.

`request_policy.go` shares bounded email/peer validation and the neutral response
floor. `recovery.go` owns customer initiation and confirmed password replacement;
`delivery_message.go` dispatches only fixed purpose-bound messages. Tests:
`recovery_fixture_test.go`, `recovery_integration_test.go`, `recovery_safety_test.go`
and `recovery_mail_integration_test.go`. The
[recovery contract](../../../../docs/04-backend/account_recovery.md) indexes the
store, schema, privacy and transport boundaries.

`device_tokens.go` and `device_sessions.go` issue purpose-separated device
credentials and controls. `device_sessions_integration_test.go` and
`device_controls_integration_test.go` cover concurrency/replay, expiry, rollback,
scope, revocation and technical bounds. Store/schema/HTTP files are indexed by
[native sessions](../../../../docs/04-backend/device_sessions.md).
