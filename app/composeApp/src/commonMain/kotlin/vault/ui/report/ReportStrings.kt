package vault.ui.report

// Relatórios-specific pt-BR copy. Shared action/loading strings stay
// in vault.ui.Strings; the feature owns its section wording. Pending
// entries never claim a report already exists.
object ReportStrings {
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
}
