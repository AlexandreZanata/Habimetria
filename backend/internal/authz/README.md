# Administrative capabilities

`capabilities.go` is the closed permission registry and role-policy validator.
`capabilities_test.go` pins known/unknown, inactive/user, explicit role grants,
support restrictions, dependencies and defensive copies.

`change_reason.go` and `change_reason_test.go` define/test the closed operator
reason vocabulary for policy mutations.

Used by panel services, PostgreSQL authority checks and panel permission forms.
It has no billing, network or local mobile dependency. SQL registry parity is
tested in `store/postgres/panel_capabilities_test.go`.
Normative contract: [authorization](../../../docs/04-backend/admin_authorization.md).
