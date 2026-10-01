package vault.local

import vault.local.db.LocalEvidence
import vault.local.db.LocalFinding
import vault.local.db.LocalInspection
import vault.local.db.LocalRoom
import vault.local.db.LocalTenancy
import kotlin.test.Test

// Print-only performance probe (never asserts timing): reproducible via
// `make bench`. Numbers land in docs/06-roadmap/acceptance-report.md.
// JVM timings are host-relative, not device claims.
class PerfProbeTest {

    private fun hundredEvidenceInput(): ManifestInput {
        val rooms = (0 until 5).map { i -> LocalRoom("r$i", "i1", "k$i", "Cômodo $i", i, "") }
        val evidences = (0 until 100).map { i ->
            LocalEvidence(
                id = "e$i", inspectionId = "i1", roomId = "r${i % 5}",
                fileName = "e$i.bin", mime = "image/jpeg", size = 1_000_000,
                sha256 = "s$i", sequence = i + 1, capturedAtDevice = "t",
                origin = "capture", shotCategory = "OVERVIEW",
                latitude = -23.5, longitude = -46.5, accuracyM = 5f,
                locationStatus = "PRECISE",
            )
        }
        return ManifestInput(
            propertyId = "p1", address = "Rua Bench, 100",
            tenancy = LocalTenancy("t1", "p1", "entry", "t"),
            inspection = LocalInspection("i1", "t1", "entry", "SEALED", "t", "t"),
            rooms = rooms, evidences = evidences,
            findings = listOf(LocalFinding("f1", "r0", "e0", "CRACK", "nota")),
            sealedAtDevice = "t",
        )
    }

    private fun inlineMeasure(label: String, block: () -> Unit) {
        // Warmup, then best of three. Reported in test stdout (TEST-*.xml).
        repeat(2) { block() }
        val samples = (0 until 3).map {
            val start = currentTimeMs()
            block()
            currentTimeMs() - start
        }
        println("PERF $label best-of-3ms=${samples.minOrNull()}")
    }

    @Test
    fun probe() {
        val input = hundredEvidenceInput()
        inlineMeasure("canonicalManifest+sha(100ev)") {
            manifestSha256(canonicalManifestJson(input))
        }
        val model = buildPdfModel("rep1", "Rua", input, "man1", "mhash", "key1") { 80 }
        inlineMeasure("paginateModel(100photos)") {
            paginateModel(model) { ImageDims(800, 600) }
        }
    }
}

private fun currentTimeMs(): Long = System.currentTimeMillis() % 1_000_000_000L
