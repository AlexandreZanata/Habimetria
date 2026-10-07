package vault.local

import kotlin.test.*

class PdfTextLayoutTest {
    @Test fun longWordsNewlinesAndUnicodeKeepEveryCharacter() {
        val text = "Primeira linha\n" + "Áçã".repeat(500) + "\nÚltima linha"
        val lines = measuredPdfLines(text, 100.0) { it.length * 5.0 }
        assertEquals(text.replace("\n", "").replace(" ", ""), lines.joinToString("").replace(" ", ""))
        assertTrue(lines.all { it.length <= 20 })
    }

    @Test fun longNotesContinueOnUniquePagesAndEveryTextRectFits() {
        val note = "Descrição técnica longa com acentos e detalhes da fotografia. ".repeat(150)
        val photo = PdfPhotoEntry("e1", "Suíte", 1, "Foto 1", "Localização indisponível")
        val finding = PdfFindingEntry("foto 1", "Suíte", "Trinca", note)
        val model = PdfModel("r1", "Rua de teste", "i1", "m1", "a".repeat(64), "2026-10-02T00:00:00Z",
            "b".repeat(64), listOf(PdfRoomSection("Suíte", 20, listOf(photo), listOf(finding))), emptyList(), emptyList())
        val pages = paginateModel(model) { ImageDims(600, 1200) }
        assertEquals((1..pages.size).toList(), pages.map { it.number })
        val texts = pages.flatMap { it.items }.filterIsInstance<PageItem.Text>()
        assertEquals(150, texts.sumOf { "Descrição".toRegex().findAll(it.text).count() })
        texts.forEach { assertTrue(it.rect!!.y + it.rect.h <= bottomLimitPt() + 0.001) }
    }
}
