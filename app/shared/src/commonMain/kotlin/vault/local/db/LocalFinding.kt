package vault.local.db

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "findings",
    foreignKeys = [
        ForeignKey(
            entity = LocalRoom::class,
            parentColumns = ["id"],
            childColumns = ["roomId"],
        ),
        ForeignKey(
            entity = LocalEvidence::class,
            parentColumns = ["id"],
            childColumns = ["evidenceId"],
        ),
    ],
    indices = [Index("roomId"), Index("evidenceId")],
)
data class LocalFinding(
    @PrimaryKey val id: String,
    val roomId: String,
    val evidenceId: String,
    val preset: String,
    val note: String?,
)

// User-customizable finding problems (v15). Built-ins seed lazily with
// stable enum-name codes; customs carry "custom:<id>" codes. Findings
// keep storing codes, so sealed bytes stay stable while labels evolve.
