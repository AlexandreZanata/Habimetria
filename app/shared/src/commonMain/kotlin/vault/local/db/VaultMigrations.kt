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
