package vault.local

import kotlin.math.round

// Coordinates to 7 decimals (~1.1 cm): integer math, identical output on
// every platform. Trailing zeros trimmed, never scientific notation.
fun canonicalDouble(value: Double): String {
    val scaled = round(value * 10_000_000).toLong()
    val negative = scaled < 0
    val abs = if (negative) -scaled else scaled
    val whole = abs / 10_000_000
    var frac = (abs % 10_000_000).toString().padStart(7, '0').trimEnd('0')
    if (frac.isEmpty()) frac = "0"
    return (if (negative) "-" else "") + "$whole.$frac"
}

fun canonicalFloat(value: Float): String {
    val scaled = round(value * 100).toLong()
    val negative = scaled < 0
    val abs = if (negative) -scaled else scaled
    val whole = abs / 100
    var frac = (abs % 100).toString().padStart(2, '0').trimEnd('0')
    if (frac.isEmpty()) frac = "0"
    return (if (negative) "-" else "") + "$whole.$frac"
}

internal fun StringBuilder.appendQuoted(raw: String) {
    append('"')
    for (ch in raw) {
        when (ch) {
            '"' -> append("\\\"")
            '\\' -> append("\\\\")
            '\b' -> append("\\b")
            '\u000C' -> append("\\f")
            '\n' -> append("\\n")
            '\r' -> append("\\r")
            '\t' -> append("\\t")
            else -> if (ch.code < 0x20) {
                append("\\u" + ch.code.toString(16).padStart(4, '0'))
            } else {
                append(ch)
            }
        }
    }
    append('"')
}

internal fun StringBuilder.appendNullableQuoted(raw: String?) {
    if (raw == null) append("null") else appendQuoted(raw)
}

internal fun StringBuilder.appendNullableNumber(raw: String?) {
    if (raw == null) append("null") else append(raw)
}
