package vault.pdf

import okio.FileSystem
import okio.Path.Companion.toPath
import vault.local.*

internal suspend fun seedDeviceInspection(session: LocalSession, technical: Boolean, photoCount: Int): String {
    val repo = session.repository
    val property = repo.createProperty("ENDEREÇO SINTÉTICO — TESTE DE PDF")
    val inspection = if (technical) repo.createTechnicalInspection(property.id) else repo.createInspection(repo.createTenancy(property.id).id)
    if (technical) ReportDetailsRepository(session.db).save(inspection.id, ReportDetails(profile = "technical",
        authorName = "AUTOR SINTÉTICO DE TESTE", authorTitle = "TÍTULO SINTÉTICO DE TESTE", photoLayout = "detail", participants = "SEM PARTICIPANTES REAIS", purpose = "Validação do aplicativo",
        scope = "Fotografias sintéticas", methods = "Verificação visual do PDF", limitations = "Não representa imóvel ou serviço real",
        analysis = "Teste de paginação e integridade", conclusions = "Documento de teste sem conclusão técnica sobre imóvel",
        council = "CREA", councilState = "MT", registration = "SINTÉTICO-TESTE", responsibilityType = "ART",
        responsibilityReference = "TESTE — NÃO É ART VÁLIDA", authorAcknowledged = true))
    val rooms = List(5) { repo.addRoom(inspection.id, "Ambiente sintético ${it + 1} · Suíte São João") }
    repeat(photoCount) { index ->
        val id = newLocalId("fixture")
        val bytes = devicePhoto(index % 8 + 1, portrait = index % 9 == 0)
        val original = session.originals.storeOriginal(id, bytes)
        val row = repo.recordEvidence(inspection.id, rooms[index % rooms.size].id, "$id.bin", "image/jpeg", original.size, original.sha256,
            shotCategory = ShotCategory.entries[index % ShotCategory.entries.size], evidenceId = id,
            location = if (index % 2 == 0) LocationClaim(null,null,null,null,null,null,LocationStatus.DENIED) else
                LocationClaim(-15.0,-56.0,200f,120_000,"synthetic","coarse",LocationStatus.APPROXIMATE, mockSignal = "MOCKED", isMocked = true),
            deviceUtcOffsetMinutes = -240, exifOrientation = index % 8 + 1)
        if (index == 0) repo.addFinding(row.roomId!!, row.id, vault.findings.FindingPreset.CRACK,
            ("NOTA COMPLETA SINTÉTICA com acentos — não descreve defeito real. ").repeat(200) + " FIM_DA_NOTA")
    }
    return inspection.id
}
