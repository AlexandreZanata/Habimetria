package vault.local

import vault.local.db.LocalEvidence
import vault.local.db.LocalFinding
import vault.local.db.LocalInspection
import vault.local.db.LocalRoom
import vault.local.db.LocalTenancy
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

private fun modelFixture(photos: Int, rooms: Int = 2): PdfModel {
    val roomList = (0 until rooms).map { i ->
        LocalRoom("r$i", "i1", "k$i", "Cômodo $i", i, "")
    }
    val evidenceList = (0 until photos).map { i ->
        LocalEvidence(
            id = "e$i", inspectionId = "i1", roomId = "r${i % rooms}",
            fileName = "e$i.bin", mime = "image/jpeg", size = 100, sha256 = "s$i",
            sequence = i + 1, capturedAtDevice = "t", origin = "capture",
            shotCategory = "OVERVIEW", latitude = -23.5, longitude = -46.5,
            accuracyM = 5f, locationStatus = "PRECISE",
        )
    }
    val findingList = listOf(LocalFinding("f1", "r0", "e0", "CRACK", "nota"))
    val input = ManifestInput(
        propertyId = "p1", address = "Rua X",
        tenancy = LocalTenancy("t1", "p1", "entry", "t"),
        inspection = LocalInspection("i1", "t1", "entry", "SEALED", "t", "t"),
        rooms = roomList, evidences = evidenceList, findings = findingList,
        sealedAtDevice = "t",
    )
    return buildPdfModel("rep1", "Rua X", input, "man1", "mhash", "key1") { 80 }
}

class PdfContractTest {

    @Test
    fun hundredPhotoPlanContainsEveryPhotoAndFindingOnce() {
        val model = modelFixture(photos = 100, rooms = 5)
        val pages = paginateModel(model) { ImageDims(800, 600) }
        val photoIds = pages.flatMap { it.items }
            .filterIsInstance<PageItem.Photo>()
            .map { it.entry.evidenceId }
        assertEquals(100, photoIds.size)
        assertEquals((0 until 100).map { "e$it" }.toSet(), photoIds.toSet())
        val findings = pages.flatMap { it.items }.filterIsInstance<PageItem.Finding>()
        assertEquals(1, findings.size)
        assertEquals("CRACK", findings.single().entry.preset)
        // Sequential pages starting at 1 with no empties.
        assertEquals((1..pages.size).toList(), pages.map { it.number })
        assertTrue(pages.all { it.items.isNotEmpty() })
        // Appendix carries the manifest plus every evidence digest in full.
        val hashes = pages.flatMap { it.items }.filterIsInstance<PageItem.HashRow>()
        assertEquals(101, hashes.size)
        assertTrue(hashes.any { it.label == "manifest" && it.hex == "mhash" })
        // Every photo rect fits its page.
        pages.flatMap { it.items }.filterIsInstance<PageItem.Photo>().forEach { photo ->
            assertTrue(photo.rect.x + photo.rect.w <= PDF_PAGE_WIDTH_PT - PDF_MARGIN_RIGHT_PT + 1e-6)
            assertTrue(photo.rect.y + photo.rect.h <= PDF_PAGE_HEIGHT_PT - PDF_MARGIN_BOTTOM_PT + 1e-6)
        }
    }

    @Test
    fun tallPhotosPaginateWithoutOverflow() {
        val model = modelFixture(photos = 10, rooms = 1)
        val pages = paginateModel(model) { ImageDims(600, 2400) }
        val photoIds = pages.flatMap { it.items }
            .filterIsInstance<PageItem.Photo>()
            .map { it.entry.evidenceId }
        assertEquals(10, photoIds.toSet().size)
        pages.flatMap { it.items }.filterIsInstance<PageItem.Photo>().forEach {
            assertEquals(0.25, it.rect.w / it.rect.h, 0.000001)
            assertTrue(it.rect.y + it.rect.h + 30 <= bottomLimitPt())
        }
    }

    @Test
    fun undecodableDimsFallBackWithoutLoss() {
        val model = modelFixture(photos = 3, rooms = 1)
        val pages = paginateModel(model) { null }
        val photoIds = pages.flatMap { it.items }
            .filterIsInstance<PageItem.Photo>()
            .map { it.entry.evidenceId }
        assertEquals(listOf("e0", "e1", "e2"), photoIds)
    }

    @Test
    fun sealTimestampRendersBrazilianDisplayFormat() {
        val rendered = formatSealedAtDevice("2026-10-01T15:10:51.619137Z")
        assertTrue(Regex("""\d{2}/\d{2}/\d{4} às \d{2}:\d{2}""").matches(rendered), rendered)
        assertTrue(!rendered.contains("T"), rendered)
        // Garbage never breaks the cover page.
        assertEquals("not-a-date", formatSealedAtDevice("not-a-date"))
    }
}
