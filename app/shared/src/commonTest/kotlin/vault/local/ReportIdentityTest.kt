package vault.local

import kotlin.test.*

class ReportIdentityTest {
    @Test fun rentalCanStayAnonymousButSuppliedDataIsValidated() {
        ReportDetails().validate("entry")
        ReportDetails(identification = PersonalIdentification()).validate("exit")
        assertFailsWith<IllegalArgumentException> { ReportDetails(identification = PersonalIdentification(cpf = "11111111111")).validate("entry") }
        assertFailsWith<IllegalArgumentException> { PersonalIdentification(email = "wrong").validate() }
        assertTrue(validCpf("529.982.247-25"))
        assertFalse(validCpf("529.982.247-26"))
        assertFalse(validCpf("５２９９８２２４７２５"))
        val id = PersonalIdentification(cpf = "529.982.247-25")
        assertEquals("***.***.***-25", id.displayedCpf())
        assertEquals(id.cpf, id.copy(exposeCpf = true).displayedCpf())
    }

    @Test fun calendarAndProfessionalReviewRejectIncompleteClaims() {
        assertTrue(validReportDate("2024-02-29"))
        assertFalse(validReportDate("2026-02-29"))
        assertFalse(validReportDate("2026-13-01"))
        assertFailsWith<IllegalArgumentException> { ReportContext().validate(true) }
        assertFailsWith<IllegalArgumentException> { ProfessionalReview().validate() }
        assertFailsWith<IllegalArgumentException> { ProfessionalReview(true,true,true, "Council page", "").validate() }
        ProfessionalReview(true,true,true, "Author-declared source", "2026-10-02").validate()
    }

    @Test fun editorialContentPreservesMaskedIdentityAndFullConclusions() {
        val details = ReportDetails(authorName = "Declared author", identification = PersonalIdentification(cpf = "52998224725"),
            context = ReportContext(summary = "SUMMARY_END", references = "REFERENCE_END", inspectionDate = "2026-10-02"),
            analysis = "ANALYSIS_END", conclusions = "CONCLUSION_END")
        val model = PdfModel("report", "Address", "inspection", "manifest", "a".repeat(64), "device", "key", emptyList(), emptyList(), emptyList(), details)
        val pages = paginateModel(model) { null }
        val lines = pages.flatMap { it.items }.filterIsInstance<PageItem.Text>().map { it.text }
        assertTrue(lines.any { it.contains("***.***.***-25") })
        assertFalse(lines.any { it.contains("52998224725") })
        listOf("SUMMARY_END", "REFERENCE_END", "ANALYSIS_END", "CONCLUSION_END").forEach { marker -> assertTrue(lines.any { marker in it }) }
        pages.flatMap { it.items }.filterIsInstance<PageItem.Text>().forEach {
            val rect = it.rect!!
            assertTrue(rect.x >= PDF_MARGIN_LEFT_PT && rect.y >= PDF_MARGIN_TOP_PT)
            assertTrue(rect.x + rect.w <= PDF_PAGE_WIDTH_PT - PDF_MARGIN_RIGHT_PT + 0.001)
            assertTrue(rect.y + rect.h <= bottomLimitPt() + 0.001)
        }
        assertEquals(12.0, PDF_BODY_SIZE)
    }
}
