# JVM code tests

- [Migration16Test.kt](Migration16Test.kt): populated v15 upgrade preserves
  sealed children, historical signed bytes and default claim uncertainty.

- [PersonalCertificateTest.kt](PersonalCertificateTest.kt): ephemeral synthetic PKCS12, wrong passwords, altered payloads, weak/ambiguous keys and credential clearing.
- [DeclarationInteroperabilityTest.kt](DeclarationInteroperabilityTest.kt): app-generated certificate declaration accepted independently by Go; removed statement rejected. Uses test bytes, not native PDF generation.
- [IndependentPackageVerifier.kt](IndependentPackageVerifier.kt): bounded Go verifier invocation with separate JSON/stdout and diagnostic/stderr streams.
- [FreeLocalInteroperabilityTest.kt](FreeLocalInteroperabilityTest.kt): a free local ZIP verifies independently after the issuer database/private files are deleted; modified PDF bytes are rejected.
