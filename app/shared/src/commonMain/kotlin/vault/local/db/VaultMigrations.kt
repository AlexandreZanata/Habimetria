package vault.local.db

import androidx.room.migration.Migration
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.execSQL

// v1 → v2: shutter-time location details plus a separate EXIF claim.
// Pure ADD COLUMN migration: every existing row survives with nulls
// (isMocked defaults to false). Destructive migration stays forbidden.
val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(connection: SQLiteConnection) {
        connection.execSQL("ALTER TABLE evidences ADD COLUMN fixAgeMs INTEGER")
        connection.execSQL("ALTER TABLE evidences ADD COLUMN provider TEXT")
        connection.execSQL("ALTER TABLE evidences ADD COLUMN accuracyLevel TEXT")
        connection.execSQL("ALTER TABLE evidences ADD COLUMN isMocked INTEGER NOT NULL DEFAULT 0")
        connection.execSQL("ALTER TABLE evidences ADD COLUMN exifLatitude REAL")
        connection.execSQL("ALTER TABLE evidences ADD COLUMN exifLongitude REAL")
    }
}

// v2 → v3: manifests table with a unique inspection binding. Existing
// inspections stay DRAFT; nothing is sealed by the migration itself.
val MIGRATION_2_3 = object : Migration(2, 3) {
    override fun migrate(connection: SQLiteConnection) {
        connection.execSQL(
            "CREATE TABLE IF NOT EXISTS `manifests` (`id` TEXT NOT NULL, `inspectionId` TEXT NOT NULL, " +
                "`formatVersion` INTEGER NOT NULL, `canonicalSha256` TEXT NOT NULL, " +
                "`canonicalJson` TEXT NOT NULL, `sealedAtDevice` TEXT NOT NULL, " +
                "`signerKeyId` TEXT NOT NULL, `algorithm` TEXT NOT NULL, " +
                "`hardwareBacked` INTEGER NOT NULL, `signature` TEXT NOT NULL, PRIMARY KEY(`id`), " +
                "FOREIGN KEY(`inspectionId`) REFERENCES `inspections`(`id`) " +
                "ON UPDATE NO ACTION ON DELETE NO ACTION )",
        )
        connection.execSQL(
            "CREATE UNIQUE INDEX IF NOT EXISTS `index_manifests_inspectionId` " +
                "ON `manifests` (`inspectionId`)",
        )
    }
}
