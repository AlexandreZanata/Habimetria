# Report authorship UI

The existing offline review → seal → PDF → share journey is unchanged.

- [ReportPersonalFields.kt](ReportPersonalFields.kt): opt-in rental identification and optional professional CPF/contact.
- [ReportInputErrors.kt](ReportInputErrors.kt): actionable curated input failures without provider details.
- [ReportContextFields.kt](ReportContextFields.kt): frozen context and explicit professional applicability.
- [DeclarationSheet.kt](DeclarationSheet.kt): deliberate statement intent and private bounded A1 import/signing.
- [DeclarationStatus.kt](DeclarationStatus.kt): holder, fingerprint, signature, trust and time status separately.
- [ReportDetailsSheet.kt](ReportDetailsSheet.kt): DRAFT editing/frozen SEALED review.
- [LocalPdfScreen.kt](LocalPdfScreen.kt): selected Report history, declarations and fresh export.
- [LocalVerifyScreen.kt](LocalVerifyScreen.kt): received-package checks and independently compared fingerprints.
- [PackageVerdictBlock.kt](PackageVerdictBlock.kt): separate integrity/declaration results.

Contract: [report authorship](../../../../../../../docs/05-mobile/report_authorship_contract.md).

# Home and properties

- [property/HomeHeroBanner.kt](property/HomeHeroBanner.kt): compact mint banner with short native copy, Brazilian house art and Nova vistoria.
- [property/PropertyCards.kt](property/PropertyCards.kt): property card rows driven by overviews.
- [property/PropertyCardRow.kt](property/PropertyCardRow.kt): text-first measurement with the cover constrained to the metadata height.
- [property/PropertyCardMenu.kt](property/PropertyCardMenu.kt): overlaid property options with a full-sized touch target.
- [property/PropertyCardVisuals.kt](property/PropertyCardVisuals.kt): cover thumbnail and rooms/status chips.
- [property/PropertyCoverPicker.kt](property/PropertyCoverPicker.kt): durable cover photo pick/capture/remove.
