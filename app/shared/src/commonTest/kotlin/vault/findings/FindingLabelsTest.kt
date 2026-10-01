package vault.findings

import kotlin.test.Test
import kotlin.test.assertEquals

class FindingLabelsTest {

    @Test
    fun everyPresetHasPtAndEnLabels() {
        assertEquals(7, FindingPreset.entries.size)
        val pt = FindingPreset.entries.map { it.label(AppLocale.PT) }
        val en = FindingPreset.entries.map { it.label(AppLocale.EN) }
        assertEquals(
            listOf("Mancha", "Arranhão", "Trinca", "Quebrado", "Umidade", "Buraco", "Outro"),
            pt,
        )
        assertEquals(
            listOf("Stain", "Scratch", "Crack", "Broken", "Moisture", "Hole", "Other"),
            en,
        )
    }

    @Test
    fun defaultLabelIsPt() {
        assertEquals("Trinca", FindingPreset.CRACK.label())
    }
}
