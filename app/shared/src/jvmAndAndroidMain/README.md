# Android and JVM certificate capability

- [PersonalCertificateSigner.kt](kotlin/vault/local/PersonalCertificateSigner.kt)
  uses standard Java PKCS12/X509/RSA/P-256 APIs on both targets.

Only explicit holder intent signs Report-specific declaration bytes.
Credentials/private keys remain memory-only; passwords are cleared after
use. Included certificates establish cryptographic consistency, not civil
identity, revocation, trusted time, an ICP-Brasil chain or PAdES support.
