package vault.local

import kotlinx.coroutines.test.runTest
import okio.FileSystem
import okio.Path.Companion.toPath
import vault.local.db.VaultDatabase
import vault.local.db.localDatabaseBuilder
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull

private var propertyPhotoSeq = 0

private fun freshPhotoVault(): Pair<VaultDatabase, LocalVaultRepository> {
    propertyPhotoSeq++
    val dir = FileSystem.SYSTEM_TEMPORARY_DIRECTORY.resolve("vault-prop-photo-$propertyPhotoSeq").toString()
    FileSystem.SYSTEM.deleteRecursively(dir.toPath())
    FileSystem.SYSTEM.createDirectories(dir.toPath())
    val db = localDatabaseBuilder("$dir/vault.db").build()
    return db to LocalVaultRepository(db)
}

class PropertyPhotoTest {
    @Test
    fun coverPhotoUpdatesWithoutTouchingAddress() = runTest {
        val (db, repo) = freshPhotoVault()
        val prop = repo.createPropertyStructured("Rua A", "1", null, "Centro", "Casa")
        assertNull(prop.photoPath)
        val withPhoto = repo.setPropertyPhoto(prop.id, "/covers/p1.bin")
        assertEquals("/covers/p1.bin", withPhoto.photoPath)
        val edited = repo.updatePropertyDetails(prop.id, "Casa nova", "Rua A", "1", null, "Centro")
        assertEquals("/covers/p1.bin", edited.photoPath)
        assertEquals("Casa nova", edited.nickname)
        val cleared = repo.setPropertyPhoto(prop.id, null)
        assertNull(cleared.photoPath)
        assertFailsWith<LocalValidationException> { repo.setPropertyPhoto("prop_missing", "/x.bin") }
        db.close()
    }
}
