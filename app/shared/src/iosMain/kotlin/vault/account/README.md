# iOS protected account storage

- `KeychainSessionItem.kt`: bounded generic-password reads/atomic updates/deletes,
  default application access group, non-synchronizing,
  WhenUnlockedThisDeviceOnly. No file/preferences secret fallback.
- `IosSessionVault.kt`: process-wide compare-and-set, background operations,
  readback before authority is exposed and explicit credential-only recovery.

The shared `SessionInstallation.kt` manages a one-byte non-secret marker in an
excluded-from-backup directory. On a missing marker, any surviving Keychain
session is deleted before the new installation is acknowledged. Failed purge
does not write a marker; corrupt markers fail closed until explicit forget.
It never reads or deletes local collection data. Construction performs no IO.
The [installation tests](../../../../jvmTest/kotlin/vault/account/README.md)
exercise marker behavior on a real temporary filesystem.

`MainViewController` injects this vault through AppContainer. Account transport
requires HTTPS; the local development HTTP default cannot carry credentials.
Linux compiles the iOS Kotlin adapter only. Keychain accessibility, reinstall,
backup/restore, entitlements and signed host execution require macOS/iPhone
validation; they are not approved by that compilation.
