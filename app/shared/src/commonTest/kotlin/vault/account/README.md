# Shared native session tests

- `SessionFixture.kt`: test-only in-memory vault, monotonic clock and HTTP fixtures.
- `SessionRotationTest.kt`: serialized consume, ambiguous response, crash/pending
  recovery, persistence failures and response User/family scope.
- `SessionTransportTest.kt`: HTTPS origin pinning, redirect denial, cross-origin
  record preservation, bounded decoding and credential redaction.
- `SessionControlsTest.kt`: durable local logout before network, unconfirmed
  remote revocation, scoped 401 cleanup and stale coordinator isolation.
- `AccountEnrollmentTest.kt`: public enrollment/recovery transport, explicit
  acknowledgments, request bounds and HTTPS origin enforcement.

Run `:shared:jvmTest`. No production plaintext/JVM secret adapter is provided.
Platform storage and owner acceptance require separate native evidence.
