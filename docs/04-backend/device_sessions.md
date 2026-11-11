# Native Device Sessions

C12 extends [authentication](account_authentication.md) and
[recovery](account_recovery.md). It grants no subscription or sync enrollment.
Local work never reads credentials or requires a successful account operation.

## Authority and rotation

Optional `device` login metadata contains a random UUID installation claim,
platform android/ios and a user-facing label (1-80 characters). It is not hardware
attestation. Password login creates a User-scoped family, 15-minute API access
and a purpose-specific random 256-bit refresh secret. Only digests reach SQL.
Legacy C10 login without device metadata remains access-only; cookies are separate.

Families expire after 30 server days, without extending on rotation. At most
20 live device sessions per User and 8192 generations per family are technical
abuse bounds, never commercial quotas. Re-login on the same User/device revokes
the earlier family. List returns only the User's live sessions, never secrets.

Refresh locks User then family, checks current role/status, device binding and
server expiry, consumes the current digest and inserts the next pair atomically.
Each rotation invalidates earlier family access. Reuse of any spent digest
commits revocation of the entire family before returning a neutral 401. A second
concurrent consume can therefore invalidate the first response; clients serialize
refresh and never retry an ambiguous response with the old secret. Other devices
and Users retain their own authority. Expiry/rotation exhaustion also deny access.
Password reset and credential/role/status/email changes revoke every User family.

Migration `0014_account_device_sessions.sql` stores device families, spent
refresh digests and a family link on API access. Current audience, User and family
are checked on every request. Revocation deletes family access immediately for
subsequent requests. Consumed digests remain until family expiry for replay
detection; bounded cleanup deletes expired families and cascades their digests.
Privacy inventory covers identifiers, labels and server activity times; no lawful
retention duration is invented. Rollback disables device issuance, retains current
schema/consumer for outstanding sessions and never converts refresh into cookies.

## Transport

POST `/api/v1/account/sessions` retains legacy email/password behavior, or accepts
`device:{id,platform,label}` and returns access_token, refresh_token, session_id,
user_id, token_type=Bearer, expires_in (at most 900, clipped to the family
deadline) and refresh_expires_at.
POST `/account/sessions/refresh` under the same base accepts
`{refresh_token,device_id}` with no cookie/Origin/Authorization. Existing source
authentication limits apply. GET `/account/sessions` and DELETE
`/account/sessions/{id}` require a current API bearer and scope to its User.
Current-access logout revokes the linked family; legacy access logout is retained.
Own already-revoked session deletion is idempotent; foreign/unknown IDs are 404.
JSON body/media/known-field bounds and no-store errors follow C10. Invalid refresh
is neutral 401, malformed metadata 400, technical session cap 409 and outages 503.

## Native storage and interruption contract

The shared coordinator pins credentials to one API origin and User/family. Access
stays in memory, refresh and identity metadata use only platform-protected storage.
Before sending refresh it atomically persists a pending state without the old
secret. Lost response, crash or persistence failure then requires deliberate login;
the previous secret is never replayed automatically. Concurrent callers serialize.
Logout forgets local authority durably before network revocation and distinguishes
local logout from unconfirmed remote revocation. It never deletes local collection.

Android uses an app-private no-backup atomic encrypted file with a non-exportable
Android Keystore AES-GCM key. iOS uses non-synchronizing application Keychain
generic-password records with WhenUnlockedThisDeviceOnly accessibility. No
plaintext, preferences or file fallback is permitted. Locked/corrupt/unavailable
storage fails closed for connected work, while the local app remains usable.
The adapters are injected through AppContainer; screens arrive in C13.

Security references: [Android Keystore](https://developer.android.com/privacy-and-security/keystore),
[Apple accessibility](https://developer.apple.com/documentation/security/ksecattraccessiblewhenunlockedthisdeviceonly),
[refresh replay detection](https://www.rfc-editor.org/rfc/rfc9700.html#section-4.14.2).
This is application session rotation, not a claim to implement an OAuth server.
Device/iOS runtime and owner release acceptance remain separately unverified.

## Backend file index

`service/account/device_tokens.go`, `device_sessions.go` and their README index
the service/tests. PostgreSQL `device_access.go`, `device_login.go`,
`device_rotation.go`, `device_controls.go` own locks/issuance/rotation/controls;
existing access/password-reset stores apply family revocation. HTTP
`account_devices.go`, `account_devices_integration_test.go` bind/test the classified
JSON surface. `internal/privacy/device_fields.go` registers personal-data purposes.
