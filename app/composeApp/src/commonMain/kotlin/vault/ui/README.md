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
- [property/PropertyListCard.kt](property/PropertyListCard.kt): integrated open/restore actions for property management.
- [property/PropertyListEmptyState.kt](property/PropertyListEmptyState.kt): distinct search, archive and first-use empty messages.
- [property/PropertySearchField.kt](property/PropertySearchField.kt): rounded labeled search with clear and keyboard actions.
- [property/PropertyCardRow.kt](property/PropertyCardRow.kt): text-first measurement with the cover constrained to the metadata height.
- [property/PropertyCardMenu.kt](property/PropertyCardMenu.kt): overlaid property options with a full-sized touch target.
- [property/PropertyCardVisuals.kt](property/PropertyCardVisuals.kt): cover thumbnail and rooms/status chips.
- [property/PropertyCoverPicker.kt](property/PropertyCoverPicker.kt): durable cover photo pick/capture/remove.

# Review

- [review/ReviewCards.kt](review/ReviewCards.kt): status card with seal action and room cards in the Relatório PDF style.
  Sealed inspections have no Rooms destination, so the Cômodos segment scrolls to the rooms section in place.

# Room setup and capture

- [journey/RoomSetup.kt](journey/RoomSetup.kt): scrollable setup intro, rounded input with keyboard add, presets and empty state.
- [journey/RoomListRow.kt](journey/RoomListRow.kt): outlined Room cards with progress, photo count and management menu.
- [journey/JourneyShortcuts.kt](journey/JourneyShortcuts.kt): secondary Rooms/Review shortcuts without a false active state during capture.
- [journey/JourneyAction.kt](journey/JourneyAction.kt): prominent icon task action with an adaptive height.
- [journey/JourneyStrings.kt](journey/JourneyStrings.kt): feature-owned pt-BR copy and accessible action labels.
- [journey/RoomCaptureCard.kt](journey/RoomCaptureCard.kt): wrapping green category selectors and system-camera shutter.
- [journey/RoomCoverageCard.kt](journey/RoomCoverageCard.kt): distinct applicable-category coverage and explicit N/A states.
- [journey/RoomCaptureControls.kt](journey/RoomCaptureControls.kt): capture sections, saved photos and onward actions.
- [journey/RoomPhotos.kt](journey/RoomPhotos.kt): bounded thumbnails, viewer and individual removal.

Contract: [capture UX](../../../../../../../docs/05-mobile/capture-ux.md).
