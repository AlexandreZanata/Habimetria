package vault.local

import vault.local.db.LocalEvidence

internal fun LocalEvidence.reportClaimLines(): String = buildList {
    add("Captura informada pelo dispositivo: $capturedAtDevice · origem: $origin")
    if (latitude == null || longitude == null) {
        add("Localização informada pelo dispositivo: " + when (locationStatus) {
            "DENIED" -> "permissão negada"
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
    if (isMocked) add("O sistema indicou localização simulada.")
    if (exifLatitude != null && exifLongitude != null) {
        add("GPS EXIF (declaração separada): ${canonicalDouble(exifLatitude)}, ${canonicalDouble(exifLongitude)}")
        if (latitude != null && longitude != null && exifDisagrees(latitude, longitude, ExifGps(exifLatitude, exifLongitude))) {
            add("Divergência superior a 100 m entre GPS EXIF e leitura do dispositivo.")
        }
    }
}.joinToString("\n")
