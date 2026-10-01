package vault.findings

// Display labels for the stable preset codes. Findings persist the enum
// name (STAIN, …), so switching locale only changes labels, never stored
// data. pt-BR today; English appears only after a future locale switch.
enum class AppLocale {
    PT,
    EN,
}

fun FindingPreset.label(locale: AppLocale = AppLocale.PT): String = when (locale) {
    AppLocale.PT -> when (this) {
        FindingPreset.STAIN -> "Mancha"
        FindingPreset.SCRATCH -> "Arranhão"
        FindingPreset.CRACK -> "Trinca"
        FindingPreset.BROKEN -> "Quebrado"
        FindingPreset.MOISTURE -> "Umidade"
        FindingPreset.HOLE -> "Buraco"
        FindingPreset.OTHER -> "Outro"
    }
    AppLocale.EN -> when (this) {
        FindingPreset.STAIN -> "Stain"
        FindingPreset.SCRATCH -> "Scratch"
        FindingPreset.CRACK -> "Crack"
        FindingPreset.BROKEN -> "Broken"
        FindingPreset.MOISTURE -> "Moisture"
        FindingPreset.HOLE -> "Hole"
        FindingPreset.OTHER -> "Other"
    }
}
