package vault.local

import vault.local.db.LocalEvidence
import vault.local.db.LocalInspection
import vault.local.db.LocalRoom
import vault.local.db.LocalTenancy
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

private fun stampEvidence(
    latitude: Double? = -12.736,
    longitude: Double? = -55.717,
    accuracyM: Float? = 8.5f,
    locationStatus: String = "PRECISE",
): LocalEvidence = LocalEvidence(
    id = "e1", inspectionId = "i1", roomId = "r1", fileName = "f.jpg", mime = "image/jpeg",
    size = 10, sha256 = "s", sequence = 1, capturedAtDevice = "2025-07-11T07:17:21",
    origin = "camera", shotCategory = "OVERVIEW", latitude = latitude, longitude = longitude,
    accuracyM = accuracyM, locationStatus = locationStatus,
)

class PhotoStampTest {
    @Test
    fun fullStampCarriesTimePlaceAndCoordinates() {
        val lines = photoStampLines(stampEvidence(), "290 Rua Panambi, Sorriso")
        assertEquals(3, lines.size)
        assertTrue(lines[0].startsWith("2025-07-11T07:17:21"))
        assertTrue(lines[0].contains("dispositivo"))
        assertEquals("290 Rua Panambi, Sorriso", lines[1])
        assertTrue(lines[2].contains("-12.736"))
        assertTrue(lines[2].contains("-55.717"))
        assertTrue(lines[2].contains("8.5 m"))
    }

    @Test
    fun approximateFixIsLabeled() {
        val lines = photoStampLines(stampEvidence(locationStatus = "APPROXIMATE"), "Rua A")
        assertTrue(lines.last().contains("aproximada"))
    }

    @Test
    fun deniedDisabledAndMissingStayExplicit() {
        assertTrue(photoStampLines(stampEvidence(null, null, null, "DENIED"), "Rua A").last().contains("negada"))
        assertTrue(photoStampLines(stampEvidence(null, null, null, "DISABLED"), "Rua A").last().contains("desativada"))
        assertTrue(photoStampLines(stampEvidence(null, null, null, "UNKNOWN"), "Rua A").last().contains("sem leitura"))
    }

    @Test
    fun blankAddressIsOmittedNeverFabricated() {
        val lines = photoStampLines(stampEvidence(), "   ")
        assertEquals(2, lines.size)
        assertTrue(lines.none { it.contains("0.0, 0.0") })
    }

    @Test
    fun wrapKeepsEveryCharacterWithinPhotoWidth() {
        val long = "Rua das Palmeiras Imperial do Oeste, 1234, Fundos, Bairro Alto da Serra"
        val wrapped = wrapStampLines(listOf("t (hora do dispositivo)", long, "-12.7, -55.7"), 100.0) { text, _, width ->
            measuredPdfLines(text, width) { it.length * 5.0 }
        }
        assertTrue(wrapped.size >= 3)
        assertTrue(wrapped.all { it.length * 5.0 <= 100.0 })
        assertEquals(
            listOf("t (hora do dispositivo)", long, "-12.7, -55.7").joinToString("").replace(" ", ""),
            wrapped.joinToString("").replace(" ", ""),
        )
    }

    @Test
    fun modelCarriesStampIntoPaginatedPhoto() {
        val evidence = stampEvidence()
        val input = ManifestInput(
            propertyId = "p1", address = "Rua X, 10",
            tenancy = LocalTenancy("t1", "p1", "entry", "t"),
            inspection = LocalInspection("i1", "t1", "entry", "SEALED", "t", "t"),
            rooms = listOf(LocalRoom("r1", "i1", "k1", "Sala", 0, "")),
            evidences = listOf(evidence.copy(id = "e1", roomId = "r1", sequence = 1)),
            findings = emptyList(), sealedAtDevice = "t",
        )
        val model = buildPdfModel("rep1", "Rua X, 10", input, "man1", "m".repeat(64), "key1") { 80 }
        val entry = model.rooms.single().photos.single()
        assertEquals(photoStampLines(evidence, "Rua X, 10"), entry.stampLines)
        val photos = paginateModel(model) { ImageDims(800, 600) }
            .flatMap { it.items }.filterIsInstance<PageItem.Photo>()
        assertTrue(photos.isNotEmpty())
        assertTrue(photos.all { it.entry.stampLines.isNotEmpty() })
    }
}
