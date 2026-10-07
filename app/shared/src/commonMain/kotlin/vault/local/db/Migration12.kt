package vault.local.db

import androidx.room.migration.Migration
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.execSQL

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
