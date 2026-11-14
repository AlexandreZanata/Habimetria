package vault.local

import vault.local.db.LocalEvidence

internal fun LocalEvidence.reportClaimLines(): String = buildList {
    add("Categoria da foto: ${reportShotLabel(shotCategory)}")
    add("Captura informada pelo dispositivo: $capturedAtDevice · origem: $origin")
    add("Fuso na captura: ${deviceUtcOffsetMinutes?.let { "UTC, deslocamento local ${it} min" } ?: "não registrado"}")
    if (latitude == null || longitude == null) {
        add("Localização informada pelo dispositivo: " + when (locationStatus) {
            "DENIED" -> "permissão negada"
            "DISABLED" -> "serviços de localização desativados"
            else -> "indisponível (sem leitura registrada)"
        })
    } else {
        add("Localização informada pelo dispositivo (WGS84): ${canonicalDouble(latitude)}, ${canonicalDouble(longitude)}")
        add("Precisão horizontal estimada: ${accuracyM?.let { "${canonicalFloat(it)} m" } ?: "não informada"} · " +
            when (locationStatus) { "APPROXIMATE" -> "localização aproximada"; else -> "permissão precisa" })
        add("Idade da leitura na captura: ${fixAgeMs?.let { "${it / 1000} s" } ?: "desconhecida"}" +
            if (fixAgeMs == null || fixAgeMs > STALE_FIX_AGE_MS) " (leitura antiga/desconhecida)" else "")
        if (provider != null) add("Origem da leitura: $provider · nível de permissão: ${accuracyLevel ?: "não informado"}")
    }
    if (fixTimeDevice != null) add("Data da leitura de localização informada pelo sistema: $fixTimeDevice")
    add(when { isMocked || mockSignal == "MOCKED" -> "O sistema indicou localização simulada."
        mockSignal == "NOT_INDICATED" -> "O sistema não indicou simulação; isso não comprova autenticidade do GPS."
        else -> "Detecção de localização simulada: não informada." })
    if (exifLatitude != null && exifLongitude != null) {
        add("GPS EXIF (declaração separada): ${canonicalDouble(exifLatitude)}, ${canonicalDouble(exifLongitude)}")
        if (latitude != null && longitude != null && exifDisagrees(latitude, longitude, ExifGps(exifLatitude, exifLongitude))) {
            add("Divergência superior a 100 m entre GPS EXIF e leitura do dispositivo.")
        }
    }
}.joinToString("\n")

internal fun reportShotLabel(category: String?): String = when (category) {
    "OVERVIEW" -> "Visão geral"
    "WALLS" -> "Paredes"
    "OPENINGS" -> "Portas / janelas"
    "FLOOR" -> "Piso"
    "CEILING" -> "Teto"
    null -> "não registrada"
    else -> category
}
