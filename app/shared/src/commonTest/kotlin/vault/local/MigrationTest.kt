package vault.local

import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import androidx.sqlite.execSQL
import kotlinx.coroutines.test.runTest
import okio.FileSystem
import okio.Path.Companion.toPath
import vault.local.db.localDatabaseBuilder
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull

// V1 schema copied from shared/schemas/.../1.json (Room validates every
// table on open, so the legacy file must match v1 byte-for-byte in shape).
private const val V1_PROPERTIES =
    "CREATE TABLE properties (`id` TEXT NOT NULL, `address` TEXT NOT NULL, `createdAtDevice` TEXT NOT NULL, PRIMARY KEY(`id`))"
private const val V1_TENANCIES =
    "CREATE TABLE tenancies (`id` TEXT NOT NULL, `propertyId` TEXT NOT NULL, `type` TEXT NOT NULL, `createdAtDevice` TEXT NOT NULL, PRIMARY KEY(`id`), FOREIGN KEY(`propertyId`) REFERENCES `properties`(`id`) ON UPDATE NO ACTION ON DELETE NO ACTION )"
private const val V1_TENANCIES_IDX =
    "CREATE INDEX `index_tenancies_propertyId` ON `tenancies` (`propertyId`)"
private const val V1_INSPECTIONS =
    "CREATE TABLE inspections (`id` TEXT NOT NULL, `tenancyId` TEXT NOT NULL, `kind` TEXT NOT NULL, `status` TEXT NOT NULL, `createdAtDevice` TEXT NOT NULL, `sealedAtDevice` TEXT, PRIMARY KEY(`id`), FOREIGN KEY(`tenancyId`) REFERENCES `tenancies`(`id`) ON UPDATE NO ACTION ON DELETE NO ACTION )"
private const val V1_INSPECTIONS_IDX =
    "CREATE INDEX `index_inspections_tenancyId` ON `inspections` (`tenancyId`)"
private const val V1_ROOMS =
    "CREATE TABLE rooms (`id` TEXT NOT NULL, `inspectionId` TEXT NOT NULL, `roomKey` TEXT NOT NULL, `label` TEXT NOT NULL, `sort` INTEGER NOT NULL, `naCategories` TEXT NOT NULL, PRIMARY KEY(`id`), FOREIGN KEY(`inspectionId`) REFERENCES `inspections`(`id`) ON UPDATE NO ACTION ON DELETE NO ACTION )"
private const val V1_ROOMS_IDX =
    "CREATE INDEX `index_rooms_inspectionId` ON `rooms` (`inspectionId`)"
private const val V1_EVIDENCES =
    "CREATE TABLE evidences (`id` TEXT NOT NULL, `inspectionId` TEXT NOT NULL, `roomId` TEXT, `fileName` TEXT NOT NULL, `mime` TEXT NOT NULL, `size` INTEGER NOT NULL, `sha256` TEXT NOT NULL, `sequence` INTEGER NOT NULL, `capturedAtDevice` TEXT NOT NULL, `origin` TEXT NOT NULL, `shotCategory` TEXT, `latitude` REAL, `longitude` REAL, `accuracyM` REAL, `locationStatus` TEXT NOT NULL, PRIMARY KEY(`id`), FOREIGN KEY(`inspectionId`) REFERENCES `inspections`(`id`) ON UPDATE NO ACTION ON DELETE NO ACTION , FOREIGN KEY(`roomId`) REFERENCES `rooms`(`id`) ON UPDATE NO ACTION ON DELETE NO ACTION )"
private const val V1_EVIDENCES_IDX1 =
    "CREATE INDEX `index_evidences_inspectionId` ON `evidences` (`inspectionId`)"
private const val V1_EVIDENCES_IDX2 =
    "CREATE INDEX `index_evidences_roomId` ON `evidences` (`roomId`)"
private const val V1_FINDINGS =
    "CREATE TABLE findings (`id` TEXT NOT NULL, `roomId` TEXT NOT NULL, `evidenceId` TEXT NOT NULL, `preset` TEXT NOT NULL, `note` TEXT, PRIMARY KEY(`id`), FOREIGN KEY(`roomId`) REFERENCES `rooms`(`id`) ON UPDATE NO ACTION ON DELETE NO ACTION , FOREIGN KEY(`evidenceId`) REFERENCES `evidences`(`id`) ON UPDATE NO ACTION ON DELETE NO ACTION )"
private const val V1_FINDINGS_IDX1 =
    "CREATE INDEX `index_findings_roomId` ON `findings` (`roomId`)"
private const val V1_FINDINGS_IDX2 =
    "CREATE INDEX `index_findings_evidenceId` ON `findings` (`evidenceId`)"

class MigrationTest {

    @Test
    fun v1RowsSurviveMigrationToV2() = runTest {
        val dir = FileSystem.SYSTEM_TEMPORARY_DIRECTORY.resolve("vault-mig12").toString()
        FileSystem.SYSTEM.deleteRecursively(dir.toPath())
        FileSystem.SYSTEM.createDirectories(dir.toPath())
        val path = "$dir/vault.db"

        val raw = BundledSQLiteDriver().open(path)
        raw.execSQL(V1_PROPERTIES)
        raw.execSQL(V1_TENANCIES)
        raw.execSQL(V1_TENANCIES_IDX)
        raw.execSQL(V1_INSPECTIONS)
        raw.execSQL(V1_INSPECTIONS_IDX)
        raw.execSQL(V1_ROOMS)
        raw.execSQL(V1_ROOMS_IDX)
        raw.execSQL(V1_EVIDENCES)
        raw.execSQL(V1_EVIDENCES_IDX1)
        raw.execSQL(V1_EVIDENCES_IDX2)
        raw.execSQL(V1_FINDINGS)
        raw.execSQL(V1_FINDINGS_IDX1)
        raw.execSQL(V1_FINDINGS_IDX2)
        raw.execSQL("INSERT INTO properties VALUES ('p1','Rua','t')")
        raw.execSQL("INSERT INTO tenancies VALUES ('t1','p1','entry','t')")
        raw.execSQL("INSERT INTO inspections VALUES ('i1','t1','entry','DRAFT','t',NULL)")
        raw.execSQL("INSERT INTO rooms VALUES ('r1','i1','k','Sala',0,'')")
        raw.execSQL(
            "INSERT INTO evidences VALUES ('e1','i1','r1','e1.bin','image/jpeg',10,'aa','1','t','capture','OVERVIEW',NULL,NULL,NULL,'unavailable')",
        )
        raw.execSQL("PRAGMA user_version = 1")
        raw.close()

        val db = localDatabaseBuilder(path).build()
        val evidence = db.evidenceDao().get("e1")!!
        assertEquals("Sala", db.roomDao().get("r1")!!.label)
        assertNull(evidence.fixAgeMs)
        assertNull(evidence.provider)
        assertNull(evidence.exifLatitude)
        assertFalse(evidence.isMocked)

        // v12: legacy property keeps its line with street backfilled.
        val legacy = db.propertyDao().get("p1")!!
        assertEquals("Rua", legacy.address)
        assertEquals("Rua", legacy.street)
        assertEquals("", legacy.number)
        assertNull(legacy.complement)
        assertEquals("", legacy.neighborhood)
        // v13: nickname defaults to empty on legacy rows.
        assertEquals("", legacy.nickname)
        // v14: visibility migrates as active on legacy rows.
        assertEquals(false, legacy.archived)

        // New columns accept writes after migration.
        val repo = LocalVaultRepository(db)
        val withClaim = repo.recordEvidence(
            inspectionId = "i1", roomId = "r1", fileName = "e2.bin",
            mime = "image/jpeg", size = 5, sha256 = "bb",
            location = LocationClaim(-23.5, -46.5, 8f, 2_000, "gps", "fine", LocationStatus.PRECISE),
            exif = ExifGps(-23.5001, -46.5001),
        )
        val reread = db.evidenceDao().get(withClaim.id)!!
        assertEquals(-23.5, reread.latitude)
        assertEquals("gps", reread.provider)
        assertEquals(-23.5001, reread.exifLatitude)
        db.close()
    }
}
