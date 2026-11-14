package vault.ui.report

// Relatórios-specific pt-BR copy. Shared action/loading strings stay
// in vault.ui.Strings; the feature owns its section wording. Pending
// entries never claim a report already exists.
object ReportStrings {
    const val listTitle = "Arquivo de relatórios"
    const val listBody = "Abra os PDFs gerados neste dispositivo e confira os lacres."
    const val search = "Buscar relatório"
    const val finalized = "Relatórios prontos"
    const val pending = "Seladas aguardando PDF"
    const val emptyAll = "Nenhum relatório ainda. Lacre uma vistoria para gerar o primeiro."
    const val emptySearch = "Nada encontrado para esta busca."
    const val unknownProperty = "Imóvel removido"
    const val deleteDescription = "Excluir relatório"
    const val deleteTitle = "Excluir relatório?"
    const val deleteBody =
        "O PDF sai deste dispositivo, mas a vistoria lacrada, as fotos " +
            "e o recibo ficam guardados. Gere de novo quando precisar."
    const val deleteConfirm = "Excluir"
    const val deletedOk = "Relatório excluído."
    const val openPdf = "Abrir PDF"
    const val verifyShort = "Verificar"
    const val versionTitle = "Relatório"
    const val readyBody = "O PDF está salvo neste dispositivo e pronto para compartilhar."
    const val documentsSection = "Arquivos e declarações"
    const val verificationSection = "Verificação e versões"
    const val packageTitle = "Pacote verificável (ZIP)"
    const val packageBody = "Inclui o PDF, fotos originais e os dados pessoais informados."
    const val declarationsTitle = "Registrar declaração"
    const val declarationsBody = "Declare autoria, recebimento ou concordância com o relatório."
    const val externalTitle = "Assinatura externa"
    const val externalBody = "Veja como assinar uma cópia fora do app e registrar o arquivo assinado."
    const val newVersionTitle = "Gerar nova versão"
    const val newVersionBody = "Gere outro PDF a partir da mesma vistoria lacrada."
    const val verifyBody = "Confira a integridade dos arquivos sem conexão com a internet."
    const val notesTitle = "Sobre os arquivos"
    const val hideTechnicalDetails = "Ocultar detalhes técnicos"
    const val packageNotes = "O ZIP reúne PDF, recibo, manifesto, chave pública, declarações, fotos originais e os dados pessoais informados. A conferência funciona offline. A identidade do autor é declarada."
    const val externalNotes = "A assinatura externa é opcional e feita em outro serviço. Confira a cópia assinada no verificador oficial."
    const val integrityFailure = "Um arquivo está ausente ou foi alterado. O compartilhamento está bloqueado."
}
