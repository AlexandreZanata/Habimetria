# Connected account session authority

- `SessionModels.kt`: redacted, bounded credential/response metadata and errors.
- `SessionVault.kt`: durable platform compare-and-set/erase contract and codec.
- `AccountApi.kt`: lazy pinned HTTPS transport, bounded responses, no redirects,
  cookies or automatic retries; explicit loopback-only development opt-in.
- `AccountSessions.kt`: serialized login/refresh, memory-only access and durable
  pending rotation before sending a refresh secret. Ambiguous responses require
  new login; cancellation/crash cannot replay a persisted old secret.
- `SessionControls.kt`: own devices/revocation and local-versus-remote logout.
- `SessionInstallation.kt`: non-secret installation marker; prevents silently
  reusing Keychain credentials surviving uninstall. Never stores credentials.
- `AccountEnrollment.kt`: bounded public registration, verification and recovery
  requests with neutral initiation and explicit server acknowledgment.

No local database/collection/queue is read or deleted. Native screens are C13.
Production has no JVM/file/preference secret fallback. Platform implementations
live under androidMain/iosMain `vault/account`; tests use commonTest doubles only.
Normative [contract](../../../../../../../docs/04-backend/device_sessions.md).
