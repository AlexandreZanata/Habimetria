package vault.ui.property

// Imóveis-specific pt-BR strings. Shared loading/retry strings stay in
// vault.ui.Strings; detail/history/filter copy lives here so the shared
// file stays small and the feature owns its wording.
object PropertyStrings {
    const val detail = "Detalhe do imóvel"
    const val history = "Vistorias do imóvel"
    const val filterAll = "Todas"
    const val filterDraft = "Em andamento"
    const val filterSealed = "Seladas"
    const val draftLabel = "Em andamento"
    const val sealedLabel = "Selada"
    const val noInspections = "Nenhuma vistoria neste imóvel ainda."
    const val noMatch = "Nenhuma vistoria neste filtro."
    const val openDetail = "Abrir"
    const val search = "Buscar imóvel"
    const val removeMenu = "Remover imóvel…"
    const val removeMenuDescription = "Opções de remoção do imóvel"
    const val deleteTitle = "Excluir imóvel?"
    const val deleteBody =
        "O cadastro vazio deste imóvel será apagado neste dispositivo. " +
            "Não há vistorias, fotos ou relatórios aqui — nada mais muda."
    const val deleteConfirm = "Excluir"
    const val deletedOk = "Imóvel excluído."
    const val archiveTitle = "Arquivar imóvel?"
    const val archiveBody =
        "O imóvel sai das listas ativas, mas vistorias, fotos e relatórios " +
            "ficam guardados neste dispositivo e podem ser restaurados. " +
            "Nada é apagado."
    const val archiveConfirm = "Arquivar"
    const val archivedOk = "Imóvel arquivado."
    const val blockedByHistory =
        "Este imóvel ganhou histórico e não pode mais ser excluído. " +
            "Ele pode ser arquivado."
    const val removeFailed = "Não foi possível remover. Tente de novo."
}
