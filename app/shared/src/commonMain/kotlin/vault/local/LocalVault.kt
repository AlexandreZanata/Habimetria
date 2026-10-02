package vault.local

import vault.local.db.VaultDatabase

// Offline facade; feature repositories keep capture and report concerns separate.
class LocalVaultRepository(db: VaultDatabase) :
    PropertiesOperations by LocalProperties(db),
    InspectionsOperations by LocalInspections(db),
    RoomsOperations by LocalRooms(db),
    EvidenceOperations by LocalEvidences(db)
