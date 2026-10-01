package vault.local

import kotlinx.coroutines.test.runTest
import okio.FileSystem
import okio.Path.Companion.toPath
import vault.local.db.localDatabaseBuilder
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.time.measureTimedValue

private var perfSeq = 0

private fun perfSession(): LocalSession {
    perfSeq++
    val fs = FileSystem.SYSTEM
    val dir = FileSystem.SYSTEM_TEMPORARY_DIRECTORY.resolve("vault-perf-$perfSeq").toString()
    fs.deleteRecursively(dir.toPath())
    fs.createDirectories(dir.toPath())
    return LocalSession(localDatabaseBuilder("$dir/vault.db").build(), dir, SyncMode.LOCAL_ONLY, fs)
}

// U11 bulk-only measurements on the 200-inspection fixture. Timings print
// for relative run-over-run comparison (Linux cannot assert absolute
// device budgets); correctness asserts prove the bulk paths, and the
// evidence-write budget binds the identical code path as the device.
class BulkPerformanceTest {

    @Test
    fun bulkOverviewsList200Inspections() = runTest {
        val session = perfSession()
        val fixture = seedPerformanceFixture(session.repository)
        assertEquals(200, fixture.inspections.size)

        val (rows, duration) = measureTimedValue {
            session.overviews.inspectionOverviews(limit = 200)
        }
        assertEquals(200, rows.size)
        assertTrue(rows.all { it.propertyLabel.isNotBlank() })
        assertTrue(rows.all { !it.propertyArchived })
        println("perf: 200 inspection overviews in ${duration.inWholeMilliseconds}ms")
        dumpFixtureSummary(
            "perf-overviews.json",
            """{"inspections":200,"ms":${duration.inWholeMilliseconds}}""",
        )
        session.close()
    }

    @Test
    fun reusedCoverageCheckAcrossFixture() = runTest {
        val session = perfSession()
        val fixture = seedPerformanceFixture(session.repository)
        val coverage = VaultCoverage(session.db)

        // Reused coverage computation across 75 inspections: every room
        // captured every category, so each inspection completes at 100.
        val sample = fixture.inspections.take(75)
        val (scores, duration) = measureTimedValue {
            sample.map { coverage.inspectionCoverage(it) }
        }
        assertEquals(75, scores.size)
        assertTrue(scores.all { it == 100 })
        println("perf: coverage over 75 inspections in ${duration.inWholeMilliseconds}ms")
        dumpFixtureSummary(
            "perf-coverage.json",
            """{"inspections":75,"ms":${duration.inWholeMilliseconds}}""",
        )
        session.close()
    }

    @Test
    fun evidenceWriteWithinBudget() = runTest {
        val session = perfSession()
        val bytes = ByteArray(3 * 1024 * 1024) { (it % 251).toByte() }

        val (stored, duration) = measureTimedValue {
            session.originals.storeOriginal("perf-photo-1", bytes)
        }
        assertEquals(bytes.size.toLong(), stored.size)
        assertTrue(
            duration.inWholeMilliseconds <= 500,
            "evidence write took ${duration.inWholeMilliseconds}ms, budget 500ms",
        )
        println("perf: 3MB evidence write in ${duration.inWholeMilliseconds}ms")
        dumpFixtureSummary(
            "perf-evidence.json",
            """{"bytes":${bytes.size},"ms":${duration.inWholeMilliseconds}}""",
        )
        session.close()
    }
}
