# JVM code tests

- [Migration16Test.kt](Migration16Test.kt): populated v15 upgrade preserves
  sealed children, historical signed bytes and default claim uncertainty.

- [PersonalCertificateTest.kt](PersonalCertificateTest.kt): ephemeral synthetic PKCS12, wrong passwords, altered payloads, weak/ambiguous keys and credential clearing.
- [DeclarationInteroperabilityTest.kt](DeclarationInteroperabilityTest.kt): app-generated certificate declaration accepted independently by Go; removed statement rejected. Uses test bytes, not native PDF generation.
