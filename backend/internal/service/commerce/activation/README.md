# Internal store activation boundary

[Configuration](../../../../../docs/04-backend/commercial_configuration.md) and
[operational controls](../../../../../docs/04-backend/commercial_rollout.md).

- [gate.go](gate.go): immutable validated config snapshot; exact app/store/env
  binding, separate new-sale control and explicit recovery operations.
- [credentials.go](credentials.go): pinned directory traversal, private regular
  files, bounded credential streams and redacted errors/log serialization.
- [gate_test.go](gate_test.go): per-store preparation, exact binding, inactive
  startup and independent new-sale/recovery/incident controls.
- [credentials_test.go](credentials_test.go): regular/private/size constraints,
  alias rejection, bounded readers, atomic replacement and error redaction.

Adapters must close prepared credentials, verify real provider app/environment
and transaction data, and independently enforce sessions, ownership, published
offers and entitlements. Prepared configuration proves none of those facts.
No provider client, purchase API or native checkout is introduced here.
