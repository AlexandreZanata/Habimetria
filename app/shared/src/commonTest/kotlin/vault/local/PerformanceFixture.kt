package vault.local

import okio.FileSystem
import okio.Path.Companion.toPath

// Deterministic 200-inspection fixture for bulk-only performance
// measurements (U11). Rows only, no photo bytes on disk: evidence rows
// carry synthetic size/sha while the byte path (write + sha256) is
// measured separately against the 500ms/photo budget. The fixture
// itself is never committed: it lives in temp dirs, with an optional
// JSON summary dump when -Dvault.fixture.dir is set (the existing
// independent-verifier convention, off by default).
data class PerformanceFixture(
    val properties: Int,
    val inspections: List<String>,
    val rooms: List<String>,
    val evidences: Int,
)

suspend fun seedPerformanceFixture(
    repo: LocalVaultRepository,
    properties: Int = 10,
    inspectionsPerProperty: Int = 20,
    roomsPerInspection: Int = 2,
): PerformanceFixture {
    val inspectionIds = mutableListOf<String>()
    val roomIds = mutableListOf<String>()
    var evidences = 0
    repeat(properties) { p ->
        val prop = repo.createPropertyStructured("Rua Perf", "${100 + p}", null, "Centro")
        repeat(inspectionsPerProperty) { i ->
            val inspection = repo.createInspection(repo.createTenancy(prop.id).id).id
            inspectionIds.add(inspection)
            repeat(roomsPerInspection) { r ->
                val room = repo.addRoom(inspection, "Sala $r")
                roomIds.add(room.id)
                for (category in ShotCategory.all()) {
                    repo.recordEvidence(
                        inspectionId = inspection,
                        roomId = room.id,
                        fileName = "perf-$p-$i-$r.bin",
                        mime = "image/jpeg",
                        size = 1024,
                        sha256 = "ab".repeat(32),
                        shotCategory = category,
                        evidenceId = "perf-ev-$p-$i-$r-${category.name}",
                    )
                    evidences++
                }
            }
        }
    }
    return PerformanceFixture(properties, inspectionIds, roomIds, evidences)
}

// Summary for the independent verifier. No-op unless the fixture dir
// system property points at a writable directory (okio: multiplatform).
fun dumpFixtureSummary(name: String, body: String) {
    val dir = System.getProperty("vault.fixture.dir")?.takeIf { it.isNotBlank() } ?: return
    try {
        val fs = FileSystem.SYSTEM
        fs.createDirectories(dir.toPath())
        fs.write("$dir/$name".toPath()) { writeUtf8(body) }
    } catch (_: Exception) {
    }
}
