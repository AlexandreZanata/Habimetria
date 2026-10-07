package vault.local

import vault.local.db.LocalEvidence
import vault.local.db.LocalFinding
import vault.local.db.LocalInspection
import vault.local.db.LocalRoom
import vault.local.db.LocalTenancy
import kotlin.test.Test
import kotlin.test.assertTrue

// Scale budget gates (JVM host-relative, generous on purpose): catch 10x
// regressions in CI, not timing noise. Device timings stay manual.
// The print-only PerfProbe keeps its contract; this file asserts.

private fun nowMs(): Long = System.currentTimeMillis()
class ScaleBudgetTest {
    private fun largeInput(photos: Int): ManifestInput {
        val rooms = (0 until 5).map { i -> LocalRoom("r$i", "i1", "k$i", "Cômodo $i", i, "") }
        val evidences = (0 until photos).map { i ->
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
            propertyId = "p1", address = "Rua Escala, 1000",
            tenancy = LocalTenancy("t1", "p1", "entry", "t"),
            inspection = LocalInspection("i1", "t1", "entry", "SEALED", "t", "t"),
            rooms = rooms, evidences = evidences,
            findings = listOf(LocalFinding("f1", "r0", "e0", "CRACK", "nota")),
            sealedAtDevice = "t",
        )
    }

    @Test
    fun thousandEvidenceManifestAndHundredPhotoPaginationStayWithinBudget() {
        val input = largeInput(1000)
        val manifestStart = nowMs()
        val manifest = canonicalManifestJson(input)
        assertTrue(nowMs() - manifestStart < 5_000, "manifest 1000ev over budget")
        assertTrue(manifest.isNotEmpty())
        val model = buildPdfModel("rep1", "Rua", input, "man1", "mhash", "key1") { 80 }
        val paginateStart = nowMs()
        val pages = paginateModel(model) { ImageDims(800, 600) }
        assertTrue(nowMs() - paginateStart < 10_000, "paginate 1000 photos over budget")
        assertTrue(pages.flatMap { it.items }.filterIsInstance<PageItem.Photo>().size == 1000)
    }
}
