package vault.local

import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import androidx.sqlite.execSQL
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.Json
import okio.FileSystem
import okio.Path.Companion.toPath
import vault.local.db.localDatabaseBuilder
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class Migration1819Test {
    @Test
    fun v18PropertiesGainNullablePhotoPath() = runTest {
        val fs = FileSystem.SYSTEM
        val directory = FileSystem.SYSTEM_TEMPORARY_DIRECTORY.resolve("vault-migration1819")
        fs.deleteRecursively(directory); fs.createDirectories(directory)
        val database = directory.resolve("vault.db")
        val schema = Json.parseToJsonElement(fs.read("schemas/vault.local.db.VaultDatabase/18.json".toPath()) { readUtf8() })
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
        connection.execSQL("INSERT INTO properties (id,address,street,number,neighborhood,nickname,archived,createdAtDevice) VALUES ('p','Rua A, 1 — Centro','Rua A','1','Centro','Casa',0,'t')")
        connection.execSQL("PRAGMA user_version = 18"); connection.close()
        val db = localDatabaseBuilder(database.toString()).build()
        assertNull(db.propertyDao().get("p")!!.photoPath)
        db.propertyDao().update(db.propertyDao().get("p")!!.copy(photoPath = "/covers/p.bin"))
        assertEquals("/covers/p.bin", db.propertyDao().get("p")!!.photoPath)
        db.close()
    }
}
