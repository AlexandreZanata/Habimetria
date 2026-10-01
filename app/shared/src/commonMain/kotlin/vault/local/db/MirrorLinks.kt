package vault.local.db

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query

// MirrorLink remembers localId -> remoteId for the optional server
// mirror (vault.sync). First write wins: retried mirrors reuse the
// original remote row instead of duplicating server state. The link
// only records that a push happened; no server-certified columns here.
@Entity(tableName = "mirror_links")
data class MirrorLink(
    @PrimaryKey val localId: String,
    val remoteId: String,
    val createdAtDevice: String,
)

@Dao
interface MirrorLinkDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(link: MirrorLink): Long

    @Query("SELECT * FROM mirror_links WHERE localId = :localId")
    suspend fun get(localId: String): MirrorLink?
}
