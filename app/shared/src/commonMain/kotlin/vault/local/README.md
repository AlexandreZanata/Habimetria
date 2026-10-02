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
- [LocalWriteLock.kt](LocalWriteLock.kt): draft writes serialized with seal.
- [ManifestEncoding.kt](ManifestEncoding.kt): canonical numbers and JSON quoting.
- [PdfReportDetails.kt](PdfReportDetails.kt): frozen report details pagination.

Tests: [ReportProfilesTest.kt](../../../../commonTest/kotlin/vault/local/ReportProfilesTest.kt) covers technical ownership, frozen details and immutable Report history.

- [PackageZip.kt](PackageZip.kt): streaming portable ZIP export and limits.
- [PackageZipImport.kt](PackageZipImport.kt): bounded allowlisted ZIP extraction.

- [ResponsibilityDocument.kt](ResponsibilityDocument.kt): immutable ART/RRT PDF attachment and digest binding.

- [PhotoDisplay.kt](PhotoDisplay.kt): frozen encoded/display dimensions and EXIF metadata.
