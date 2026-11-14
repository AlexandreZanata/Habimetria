# Android protected account storage

`AndroidSessionVault.kt` implements process-wide atomic compare-and-set with a
no-backup AtomicFile, explicit data/parent-directory sync and readback before
acknowledgment. It rejects locked, corrupt and unavailable storage. Namespace
selection isolates synthetic device probes; production uses main.

`AndroidSessionCipher.kt` uses an app-UID Android Keystore AES-256-GCM key,
random provider IV, versioned authenticated app/namespace binding and no raw key
export. Android 15+ also enforces unlocked-device key use; older versions use
the Keyguard precheck and OS app/credential-encrypted storage. Hardware-backed
implementation is device-dependent, never assumed or advertised as attestation.
Explicit erase forgets only account credentials/key, including corrupt records.
No local collection is affected and no plaintext/preference fallback exists.
