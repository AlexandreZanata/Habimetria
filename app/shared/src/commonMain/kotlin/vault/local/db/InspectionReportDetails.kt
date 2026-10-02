package vault.local.db

import androidx.room.*

@Entity(tableName = "inspection_report_details", foreignKeys = [
    ForeignKey(entity = LocalInspection::class, parentColumns = ["id"], childColumns = ["inspectionId"]),
])
data class InspectionReportDetails(@PrimaryKey val inspectionId: String, val json: String)

@Dao
interface InspectionReportDetailsDao {
    @Query("SELECT * FROM inspection_report_details WHERE inspectionId = :id")
    suspend fun get(id: String): InspectionReportDetails?

    @Upsert
    suspend fun save(details: InspectionReportDetails)

    @Query("DELETE FROM inspection_report_details WHERE inspectionId = :id")
    suspend fun delete(id: String)
}
