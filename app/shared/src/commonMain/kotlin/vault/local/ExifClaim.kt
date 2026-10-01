package vault.local

import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

// EXIF GPS read straight from JPEG bytes: no platform codec, no new
// dependency, identical results on Android/iOS/JVM. EXIF coordinates are
// a separate device claim — they never overwrite the shutter-time fix.
data class ExifGps(val latitude: Double, val longitude: Double)

fun readExifGps(bytes: ByteArray): ExifGps? {
    if (bytes.size < 4 || bytes[0] != 0xFF.toByte() || bytes[1] != 0xD8.toByte()) return null
    var pos = 2
    while (pos + 4 <= bytes.size) {
        if (bytes[pos] != 0xFF.toByte()) return null
        val marker = bytes[pos + 1].toInt() and 0xFF
        if (marker == 0xD9 || marker == 0xDA) return null // EOI / start of scan
        if (pos + 4 > bytes.size) return null
        val length = ((bytes[pos + 2].toInt() and 0xFF) shl 8) or (bytes[pos + 3].toInt() and 0xFF)
        if (length < 2 || pos + 2 + length > bytes.size) return null
        if (marker == 0xE1 && hasExifHeader(bytes, pos + 4)) {
            parseTiffGps(bytes, pos + 4 + 6, pos + 4 + length)?.let { return it }
        }
        pos += 2 + length
    }
    return null
}

private fun hasExifHeader(bytes: ByteArray, at: Int): Boolean {
    if (at + 6 > bytes.size) return false
    return bytes[at] == 'E'.code.toByte() && bytes[at + 1] == 'x'.code.toByte() &&
        bytes[at + 2] == 'i'.code.toByte() && bytes[at + 3] == 'f'.code.toByte() &&
        bytes[at + 4] == 0.toByte() && bytes[at + 5] == 0.toByte()
}

private class TiffReader(val bytes: ByteArray, val base: Int, val little: Boolean, val limit: Int) {
    fun u16(at: Int): Int {
        val i = base + at
        if (i + 2 > limit || i < 0) throw IndexOutOfBoundsException()
        val a = bytes[i].toInt() and 0xFF
        val b = bytes[i + 1].toInt() and 0xFF
        return if (little) a or (b shl 8) else (a shl 8) or b
    }

    fun u32(at: Int): Long {
        val i = base + at
        if (i + 4 > limit || i < 0) throw IndexOutOfBoundsException()
        val a = bytes[i].toLong() and 0xFF
        val b = bytes[i + 1].toLong() and 0xFF
        val c = bytes[i + 2].toLong() and 0xFF
        val d = bytes[i + 3].toLong() and 0xFF
        return if (little) a or (b shl 8) or (c shl 16) or (d shl 24)
        else (a shl 24) or (b shl 16) or (c shl 8) or d
    }
}

private fun parseTiffGps(bytes: ByteArray, tiffStart: Int, limit: Int): ExifGps? {
    try {
        if (tiffStart + 8 > limit) return null
        val little = when {
            bytes[tiffStart] == 'I'.code.toByte() && bytes[tiffStart + 1] == 'I'.code.toByte() -> true
            bytes[tiffStart] == 'M'.code.toByte() && bytes[tiffStart + 1] == 'M'.code.toByte() -> false
            else -> return null
        }
        val r = TiffReader(bytes, tiffStart, little, limit)
        if (r.u16(2) != 42) return null
        val ifd0 = r.u32(4).toInt()
        val gpsOffset = findEntryOffset(r, ifd0, 0x8825) ?: return null
        return readGpsIfd(r, gpsOffset)
    } catch (_: Exception) {
        return null
    }
}

private fun findEntryOffset(r: TiffReader, ifd: Int, tag: Int): Int? {
    val count = r.u16(ifd)
    for (i in 0 until count) {
        val entry = ifd + 2 + i * 12
        if (r.u16(entry) == tag) return r.u32(entry + 8).toInt()
    }
    return null
}

private fun asciiAt(r: TiffReader, entry: Int): String {
    // Type ASCII (2), count includes NUL; short values sit inline.
    val count = r.u32(entry + 4).toInt()
    val valueOrOffset = r.u32(entry + 8).toInt()
    val start = if (count <= 4) entry + 8 else valueOrOffset
    val chars = CharArray(count.coerceAtMost(8))
    for (i in chars.indices) {
        val b = r.bytes.getOrNull(r.base + start + i) ?: 0
        if (b == 0.toByte()) return chars.concatToString(0, i)
        chars[i] = b.toInt().toChar()
    }
    return chars.concatToString().trimEnd('\u0000')
}

private fun rationalsAt(r: TiffReader, entry: Int): DoubleArray {
    // Type RATIONAL (5): count triples of num/den pairs at offset.
    val count = r.u32(entry + 4).toInt().coerceAtMost(3)
    val offset = r.u32(entry + 8).toInt()
    return DoubleArray(count) { i ->
        val num = r.u32(offset + i * 8)
        val den = r.u32(offset + i * 8 + 4)
        if (den == 0L) Double.NaN else num.toDouble() / den.toDouble()
    }
}

private fun dmsToDecimal(parts: DoubleArray): Double {
    val d = parts.getOrElse(0) { 0.0 }
    val m = parts.getOrElse(1) { 0.0 }
    val s = parts.getOrElse(2) { 0.0 }
    return d + m / 60.0 + s / 3600.0
}

private fun readGpsIfd(r: TiffReader, gpsIfd: Int): ExifGps? {
    val count = r.u16(gpsIfd)
    var latRef = "N"
    var lonRef = "E"
    var lat = doubleArrayOf()
    var lon = doubleArrayOf()
    for (i in 0 until count) {
        val entry = gpsIfd + 2 + i * 12
        when (r.u16(entry)) {
            1 -> latRef = asciiAt(r, entry)
            2 -> lat = rationalsAt(r, entry)
            3 -> lonRef = asciiAt(r, entry)
            4 -> lon = rationalsAt(r, entry)
        }
    }
    if (lat.size != 3 || lon.size != 3) return null
    var latitude = dmsToDecimal(lat)
    var longitude = dmsToDecimal(lon)
    if (latRef.startsWith("S")) latitude = -latitude
    if (lonRef.startsWith("W")) longitude = -longitude
    if (latitude.isNaN() || longitude.isNaN()) return null
    return ExifGps(latitude, longitude)
}

// Haversine distance in meters. A large gap between the shutter fix and
// the EXIF claim stays visible in review; neither value is "corrected".
fun distanceMeters(aLat: Double, aLon: Double, bLat: Double, bLon: Double): Double {
    val earth = 6_371_000.0
    val dLat = toRad(bLat - aLat)
    val dLon = toRad(bLon - aLon)
    val h = sin(dLat / 2).pow2() + cos(toRad(aLat)) *
        cos(toRad(bLat)) * sin(dLon / 2).pow2()
    return 2 * earth * atan2(sqrt(h), sqrt(1 - h))
}

private fun toRad(degrees: Double): Double = degrees * PI / 180.0

private fun Double.pow2(): Double = this * this

fun exifDisagrees(fixLat: Double, fixLon: Double, exif: ExifGps, thresholdM: Double = 100.0): Boolean =
    distanceMeters(fixLat, fixLon, exif.latitude, exif.longitude) > thresholdM
