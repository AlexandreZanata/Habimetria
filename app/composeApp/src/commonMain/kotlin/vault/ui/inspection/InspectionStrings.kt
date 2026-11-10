package vault.ui.inspection

// Vistorias-specific pt-BR copy. Shared loading/retry/continue
// strings stay in vault.ui.Strings; the feature owns its wording.
object InspectionStrings {
    const val listTitle = "Histórico de vistorias"
    const val listBody = "Retome uma vistoria em andamento ou consulte um registro lacrado."
    const val search = "Buscar vistoria"
    const val emptyAll = "Nenhuma vistoria ainda. Crie um imóvel para começar."
    const val emptyFilter = "Nenhuma vistoria neste filtro."
    const val kindEntry = "Entrada"
    const val kindExit = "Saída"
    const val kindTechnical = "Profissional"
    const val unknownProperty = "Imóvel removido"
    const val optionsDescription = "Opções da vistoria"
    const val deleteTitle = "Excluir vistoria?"
    const val deleteDraftBody =
        "Cômodos, fotos, achados e anotações desta vistoria serão apagados " +
            "neste dispositivo. O imóvel e as outras vistorias não mudam."
    const val deleteSealedBody =
        "A vistoria lacrada sai deste dispositivo com o relatório, o PDF " +
            "e o lacre. O imóvel e as outras vistorias não mudam."
    const val blockedBody =
        "Esta vistoria tem comparações com outra e não pode ser excluída " +
            "sem corromper a revisão do outro lado."
    const val deletedOk = "Vistoria excluída."
}
