package vault.ui.journey

// Feature-owned pt-BR copy for room setup and guided capture.
internal object JourneyStrings {
    const val roomSetupTitle = "Adicione os cômodos"
    const val roomSetupHint = "Digite o nome de um cômodo por vez."
    const val roomNameExample = "Ex.: Sala, cozinha ou quarto"
    const val addRoom = "Adicionar cômodo"
    const val roomsEmptyTitle = "Nenhum cômodo adicionado"
    const val roomsEmptyHint = "Adicione o primeiro cômodo para começar a fotografar."
    const val captureHint = "Escolha o enquadramento da próxima foto."
    const val pending = "Falta fotografar"
    const val captured = "Fotografado"
    const val excluded = "Não se aplica"
    const val naHint = "Use N/A apenas quando a categoria não se aplicar ao cômodo."
    const val captureOverview = "Cobertura do cômodo"
    const val reviewPhotos = "Revisar fotos"
    const val photos = "Fotos do cômodo"
    fun roomOptions(label: String) = "Opções de $label"
    fun naAction(label: String, excluded: Boolean) =
        if (excluded) "Incluir $label na cobertura" else "$label: marcar como não se aplica"
    fun covered(captured: Int, required: Int) = "$captured de $required categorias fotografadas"
    fun roomProgress(percent: Int, photos: Int) = "$percent% de cobertura · $photos fotos"
    fun roomCount(count: Int) = if (count == 1) "1 cômodo" else "$count cômodos"
    fun deletePhoto(sequence: Int) = "Excluir foto $sequence"
    fun photoNumber(sequence: Int) = "Foto $sequence"
}
