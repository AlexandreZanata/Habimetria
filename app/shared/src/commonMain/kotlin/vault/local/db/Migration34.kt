package vault.local.db

import androidx.room.migration.Migration
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.execSQL

val MIGRATION_3_4 = object : Migration(3, 4) {
    override fun migrate(connection: SQLiteConnection) {
        connection.execSQL(
            "CREATE TABLE IF NOT EXISTS `reports` (`id` TEXT NOT NULL, `inspectionId` TEXT NOT NULL, " +
                "`manifestId` TEXT NOT NULL, `manifestSha256` TEXT NOT NULL, `status` TEXT NOT NULL, " +
                "`pdfSha256` TEXT, `pdfFileName` TEXT, `receiptSignature` TEXT, " +
                "`signerKeyId` TEXT NOT NULL, `algorithm` TEXT NOT NULL, " +
                "`hardwareBacked` INTEGER NOT NULL, `createdAtDevice` TEXT NOT NULL, " +
                "`completedAtDevice` TEXT, PRIMARY KEY(`id`), " +
                "FOREIGN KEY(`inspectionId`) REFERENCES `inspections`(`id`) " +
                "ON UPDATE NO ACTION ON DELETE NO ACTION , " +
                "FOREIGN KEY(`manifestId`) REFERENCES `manifests`(`id`) " +
                "ON UPDATE NO ACTION ON DELETE NO ACTION )",
        )
        connection.execSQL(
            "CREATE UNIQUE INDEX IF NOT EXISTS `index_reports_manifestId` " +
                "ON `reports` (`manifestId`)",
        )
    }
}

// v4 → v5: trusted issuers. Trust starts empty on every install;
// nothing is ever trusted by migration.
