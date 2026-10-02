package vault.local.db

import androidx.room.migration.Migration
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.execSQL

// Preserve IDs and all dependent rows; defer NO ACTION foreign-key checks
// until the replacement table has regained its original name.
val MIGRATION_15_16 = object : Migration(15, 16) {
    override fun migrate(connection: SQLiteConnection) {
        connection.execSQL("PRAGMA defer_foreign_keys = ON")
        connection.execSQL("ALTER TABLE evidences ADD COLUMN mockSignal TEXT")
        connection.execSQL("ALTER TABLE evidences ADD COLUMN fixTimeDevice TEXT")
        connection.execSQL("ALTER TABLE evidences ADD COLUMN deviceUtcOffsetMinutes INTEGER")
        connection.execSQL("ALTER TABLE evidences ADD COLUMN exifOrientation INTEGER")
        connection.execSQL("CREATE TABLE inspections_new (id TEXT NOT NULL PRIMARY KEY, tenancyId TEXT, " +
            "kind TEXT NOT NULL, status TEXT NOT NULL, createdAtDevice TEXT NOT NULL, sealedAtDevice TEXT, " +
            "propertyId TEXT, FOREIGN KEY(tenancyId) REFERENCES tenancies(id) ON UPDATE NO ACTION ON DELETE NO ACTION, " +
            "FOREIGN KEY(propertyId) REFERENCES properties(id) ON UPDATE NO ACTION ON DELETE NO ACTION)")
        connection.execSQL("INSERT INTO inspections_new SELECT inspections.*, tenancies.propertyId " +
            "FROM inspections JOIN tenancies ON inspections.tenancyId = tenancies.id")
        connection.execSQL("DROP TABLE inspections")
        connection.execSQL("ALTER TABLE inspections_new RENAME TO inspections")
        connection.execSQL("CREATE INDEX index_inspections_tenancyId ON inspections(tenancyId)")
        connection.execSQL("CREATE INDEX index_inspections_propertyId ON inspections(propertyId)")
        connection.execSQL("ALTER TABLE reports ADD COLUMN versionNumber INTEGER NOT NULL DEFAULT 1")
        connection.execSQL("DROP INDEX index_reports_manifestId")
        connection.execSQL("CREATE INDEX index_reports_manifestId ON reports(manifestId)")
        connection.execSQL("CREATE INDEX index_reports_inspectionId ON reports(inspectionId)")
        connection.execSQL("CREATE TABLE inspection_report_details (inspectionId TEXT NOT NULL PRIMARY KEY, " +
            "json TEXT NOT NULL, FOREIGN KEY(inspectionId) REFERENCES inspections(id) ON UPDATE NO ACTION ON DELETE NO ACTION)")
    }
}
