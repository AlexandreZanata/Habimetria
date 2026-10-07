# Local application core

[Database definitions and migrations](db/README.md).

LocalVaultRepository delegates feature operations to:

- [LocalValueTypes.kt](LocalValueTypes.kt)
- [LocalWriteGuard.kt](LocalWriteGuard.kt)
- [LocalProperties.kt](LocalProperties.kt)
- [LocalInspections.kt](LocalInspections.kt)
- [LocalRooms.kt](LocalRooms.kt)
- [LocalEvidences.kt](LocalEvidences.kt)

- [ReportDetails.kt](ReportDetails.kt): reviewed report metadata and validation.
- [RoomPresets.kt](RoomPresets.kt): Settings-managed preset room names copied into DRAFT inspections on demand.
- [LocalWriteLock.kt](LocalWriteLock.kt): draft writes serialized with seal.
- [ManifestEncoding.kt](ManifestEncoding.kt): canonical numbers and JSON quoting.
- [PdfReportDetails.kt](PdfReportDetails.kt): frozen report details pagination.

Tests: [ReportProfilesTest.kt](../../../../commonTest/kotlin/vault/local/ReportProfilesTest.kt) covers technical ownership, frozen details and immutable Report history.

- [PackageZip.kt](PackageZip.kt): streaming portable ZIP export and limits.
- [PackageZipImport.kt](PackageZipImport.kt): bounded allowlisted ZIP extraction.

- [ResponsibilityDocument.kt](ResponsibilityDocument.kt): immutable ART/RRT PDF attachment and digest binding.

- [PhotoDisplay.kt](PhotoDisplay.kt): frozen encoded/display dimensions and EXIF metadata.
- [PhotoStamp.kt](PhotoStamp.kt): on-photo location stamp lines (time, place, device GPS) for PDF overlay.

- [DisplayCorrection.kt](DisplayCorrection.kt): DRAFT-only manual rotation, frozen in Manifest v5.

## Authorship and editorial extension

- [PersonalIdentification.kt](PersonalIdentification.kt): optional CPF/contact and presentation masking.
- [ReportContext.kt](ReportContext.kt): frozen context, calendar and professional applicability declarations.
- [PdfCover.kt](PdfCover.kt): corporate cover and summary.
- [PersonalCertificate.kt](PersonalCertificate.kt): certificate identity/proof and platform signing capability.
- [CertificateValidation.kt](CertificateValidation.kt): separate chain/validity/revocation/time/trust verdicts.
- [RevocationPolicy.kt](RevocationPolicy.kt): offline UNKNOWN revocation, no fabricated GOOD/REVOKED.
- [TimestampRecord.kt](TimestampRecord.kt): optional contracted ACT token binding, ABSENT versus EXTERNAL.
- [PadesReadiness.kt](PadesReadiness.kt): PDF readiness for external PAdES without local embedding.
- [ExternalSigning.kt](ExternalSigning.kt): honest external PAdES/Gov.br/ACT handoffs preserving READY.
- [PdfOutline.kt](PdfOutline.kt): logical navigation outline; contents page stays portable.
- [ShareDeclaration.kt](ShareDeclaration.kt): Report-bound intents, index and verdict types.
- [ReportArtifactLock.kt](ReportArtifactLock.kt): serialized artifact mutations.
- [ReportDeclarations.kt](ReportDeclarations.kt): append, signed-index commit and bounded recovery.
- [DeclarationVerifier.kt](DeclarationVerifier.kt): exact set/chain/signature checks.

Code tests: [ReportIdentityTest.kt](../../../../commonTest/kotlin/vault/local/ReportIdentityTest.kt),
[ReportDeclarationsTest.kt](../../../../commonTest/kotlin/vault/local/ReportDeclarationsTest.kt),
[DeclarationRecoveryTest.kt](../../../../commonTest/kotlin/vault/local/DeclarationRecoveryTest.kt).
