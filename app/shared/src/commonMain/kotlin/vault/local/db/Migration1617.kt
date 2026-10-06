package vault.local.db

import androidx.room.migration.Migration
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.execSQL

// Manual display correction: DRAFT-only rotation frozen in Manifest v5.
// Originals stay immutable; only derivatives/display dims change.
val MIGRATION_16_17 = object : Migration(16, 17) {
    override fun migrate(connection: SQLiteConnection) {
        connection.execSQL("ALTER TABLE evidences ADD COLUMN displayRotation INTEGER NOT NULL DEFAULT 0")
    }
}
