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
}
