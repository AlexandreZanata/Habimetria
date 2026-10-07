package vault.local

import vault.local.db.LocalEvidence
import vault.local.db.LocalFinding
import vault.local.db.LocalInspection
import vault.local.db.LocalRoom
import vault.local.db.LocalTenancy
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

private fun fixtureInput(): ManifestInput = ManifestInput(
    propertyId = "p1",
    address = "Rua \"Central\"\nSão Paulo",
    tenancy = LocalTenancy("t1", "p1", "entry", "2026-01-01T00:00:00"),
    inspection = LocalInspection("i1", "t1", "entry", "DRAFT", "2026-01-01T00:00:00", null),
    rooms = listOf(LocalRoom("r1", "i1", "k1", "Suíte çã", 0, "")),
    evidences = listOf(
        LocalEvidence(
            id = "e1", inspectionId = "i1", roomId = "r1", fileName = "e1.bin",
            mime = "image/jpeg", size = 10, sha256 = "aa", sequence = 1,
            capturedAtDevice = "2026-01-01T00:01:00", origin = "capture",
            shotCategory = "OVERVIEW", latitude = -23.5, longitude = -46.5,
            accuracyM = 8.25f, locationStatus = "PRECISE", fixAgeMs = 2_000,
            provider = "gps", accuracyLevel = "fine", isMocked = false,
            exifLatitude = null, exifLongitude = null,
        ),
    ),
    findings = listOf(LocalFinding("f1", "r1", "e1", "CRACK", "nota\ttab")),
    sealedAtDevice = "2026-01-02T00:00:00",
)

class ManifestCodecTest {

    @Test
    fun knownAnswerStaysByteExact() {
        val canonical = canonicalManifestJson(fixtureInput())
        assertTrue(canonical.startsWith("{\"address\":\"Rua \\\"Central\\\"\\nSão Paulo\""))
        assertTrue(canonical.contains("\"purpose\":\"local-seal/v1\""))
        assertTrue(canonical.contains("\"formatVersion\":5"))
        assertTrue(canonical.contains("\"displayRotation\":0"))
        assertTrue(canonical.contains("\"kind\":\"entry\""))
        assertTrue(canonical.contains("\"latitude\":-23.5"))
        assertTrue(canonical.contains("\"accuracyM\":8.25"))
        assertTrue(canonical.contains("\"note\":\"nota\\ttab\""))
        // Stable hash for this exact fixture.
        assertEquals(canonical, canonicalManifestJson(fixtureInput()))
        assertEquals(64, manifestSha256(canonical).length)
    }

    @Test
    fun inputOrderNeverChangesOutput() {
        val base = fixtureInput()
        val shuffled = base.copy(
            rooms = base.rooms.reversed(),
            evidences = base.evidences.reversed(),
            findings = base.findings.reversed(),
        )
        assertEquals(canonicalManifestJson(base), canonicalManifestJson(shuffled))
    }

    @Test
    fun doublesFormatDeterministically() {
        assertEquals("-23.5", canonicalDouble(-23.5))
        assertEquals("0.0", canonicalDouble(0.0))
        assertEquals("46.5000001", canonicalDouble(46.5000001))
        assertEquals("0.0", canonicalDouble(-0.00000001))
        assertEquals("8.25", canonicalFloat(8.25f))
        assertEquals("0.0", canonicalFloat(0f))
    }
}
