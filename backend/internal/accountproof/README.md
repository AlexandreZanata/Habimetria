# Verification proof envelope

`key.go` derives independent AEAD/HMAC keys, generates random proof envelopes,
and authenticates User/purpose/operation binding. `key_test.go` exercises tamper,
cross-identity/purpose isolation, entropy/digest and formatting redaction.

Only the encrypted outbox proof is recoverable for delivery; verification uses
its digest. The operator master is a private random 32-byte mounted file, not
a signing/billing key. [Contract](../../../docs/04-backend/account_registration.md).
