package vault.ui.property

// Imóveis-specific pt-BR strings. Shared loading/retry strings stay in
// vault.ui.Strings; detail/history/filter copy lives here so the shared
// file stays small and the feature owns its wording.
object PropertyStrings {
    const val inspectionAddressTitle = "Endereço do imóvel"
    const val rentalMode = "Locação"
    const val technicalMode = "Profissional"
    const val listTitle = "Meus imóveis"
    const val listBody = "Consulte os endereços cadastrados e suas vistorias."
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
    const val searchHint = "Nome ou endereço"
    const val clearSearch = "Limpar busca"
    const val openProperty = "Abrir imóvel"
    const val noPropertyMatch = "Nenhum imóvel encontrado para esta busca."
    const val propertyNotFound = "Imóvel não encontrado neste dispositivo."
    const val closeCreateForm = "Fechar cadastro"
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
    const val propertyData = "Dados"
    const val removeShort = "Remover"
    const val optionsDescription = "Opções do imóvel"
    const val dataTitle = "Dados do imóvel"
    const val editData = "Editar dados"
    const val dataSaved = "Dados salvos."
    const val visibilityActive = "Ativos"
    const val visibilityArchived = "Arquivados"
    const val archivedBadge = "Arquivado"
    const val noArchived = "Nenhum imóvel arquivado."
    const val restoreAction = "Restaurar imóvel"
    const val restoredOk = "Imóvel restaurado."
    const val archivedHint = "Veja em Arquivados."
    const val coverTitle = "Foto de capa"
    const val coverPick = "Adicionar foto de capa"
    const val coverChange = "Trocar foto"
    const val coverRemove = "Remover foto"
    const val archivedDetailNote =
        "Imóvel arquivado. Vistorias, fotos e relatórios ficam guardados " +
            "neste dispositivo. Restaure para iniciar uma nova vistoria."

    // History rows keep archived inspections listed with an explicit label.
    fun archivedSuffix(label: String, archived: Boolean): String =
        if (archived) "$label · $archivedBadge" else label
}
