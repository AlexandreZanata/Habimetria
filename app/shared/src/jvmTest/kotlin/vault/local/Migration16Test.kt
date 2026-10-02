package vault.local

import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import androidx.sqlite.execSQL
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.*
import okio.FileSystem
import okio.Path.Companion.toPath
import vault.local.db.localDatabaseBuilder
import kotlin.test.*

class Migration16Test {
    @Test
    fun populatedV15PreservesSealedChildrenAndCanonicalBytes() = runTest {
        val fs = FileSystem.SYSTEM
        val directory = FileSystem.SYSTEM_TEMPORARY_DIRECTORY.resolve("vault-migration16")
        fs.deleteRecursively(directory); fs.createDirectories(directory)
        val database = directory.resolve("vault.db")
        val schema = Json.parseToJsonElement(fs.read("schemas/vault.local.db.VaultDatabase/15.json".toPath()) { readUtf8() })
            .jsonObject.getValue("database").jsonObject
        val connection = BundledSQLiteDriver().open(database.toString())
        schema.getValue("entities").jsonArray.forEach { element ->
            val entity = element.jsonObject
            val table = entity.getValue("tableName").jsonPrimitive.content
            connection.execSQL(entity.getValue("createSql").jsonPrimitive.content.replace("\${TABLE_NAME}", table))
            entity["indices"]?.jsonArray.orEmpty().forEach { index ->
                connection.execSQL(index.jsonObject.getValue("createSql").jsonPrimitive.content.replace("\${TABLE_NAME}", table))
            }
        }
        connection.execSQL("INSERT INTO properties (id,address,street,number,neighborhood,nickname,archived,createdAtDevice) VALUES ('p','Rua','Rua','1','Centro','',0,'t')")
        connection.execSQL("INSERT INTO tenancies VALUES ('t','p','entry','t')")
        connection.execSQL("INSERT INTO inspections VALUES ('i','t','entry','SEALED','t','t')")
        connection.execSQL("INSERT INTO rooms VALUES ('r','i','k','Sala',0,'',NULL)")
        connection.execSQL("INSERT INTO evidences (id,inspectionId,roomId,fileName,mime,size,sha256,sequence,capturedAtDevice,origin,locationStatus,isMocked) VALUES ('e','i','r','e.bin','image/jpeg',3,'abc',1,'t','capture','UNAVAILABLE',0)")
        connection.execSQL("INSERT INTO findings VALUES ('f','r','e','CRACK','nota histórica')")
        connection.execSQL("INSERT INTO manifests VALUES ('m','i',2,'historical-hash','historical-signed-bytes','t','key','ECDSA-P256-SHA256-raw64',0,'signature')")
        connection.execSQL("INSERT INTO reports VALUES ('report','i','m','historical-hash','READY','pdf-hash','report.pdf','receipt-signature','key','ECDSA-P256-SHA256-raw64',0,'t','t')")
        connection.execSQL("PRAGMA user_version = 15"); connection.close()
        val db = localDatabaseBuilder(database.toString()).build()
        assertEquals("p", db.inspectionDao().get("i")!!.propertyId)
        assertEquals(STATUS_SEALED, db.inspectionDao().get("i")!!.status)
        assertEquals("historical-signed-bytes", db.manifestDao().get("m")!!.canonicalJson)
        assertEquals("nota histórica", db.findingDao().listForEvidence("e").single().note)
        assertEquals("abc", db.evidenceDao().get("e")!!.sha256)
        assertEquals(1, db.reportDao().get("report")!!.versionNumber)
        assertNull(db.evidenceDao().get("e")!!.mockSignal)
        db.close()
    }
}
