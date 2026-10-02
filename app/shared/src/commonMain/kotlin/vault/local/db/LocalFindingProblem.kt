package vault.local.db

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "finding_problems")
data class LocalFindingProblem(
    @PrimaryKey val code: String,
    val label: String,
    val active: Boolean = true,
    val builtin: Boolean = false,
)
