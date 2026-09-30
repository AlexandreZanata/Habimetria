package vault.local

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

// Builds a minimal JPEG: SOI + APP1 Exif (little-endian TIFF with a GPS
// IFD: 23°30'00"S, 46°30'00"W) + EOI.
private fun syntheticGpsJpeg(latRef: String = "S", lonRef: String = "W"): ByteArray {
    val out = mutableListOf<Byte>()
    fun u16(v: Int) {
        out.add((v and 0xFF).toByte())
        out.add(((v shr 8) and 0xFF).toByte())
    }
    fun u32(v: Long) {
        u16((v and 0xFFFF).toInt())
        u16(((v shr 16) and 0xFFFF).toInt())
    }
    out.add(0xFF.toByte()); out.add(0xD8.toByte()) // SOI
    val tiff = mutableListOf<Byte>()
    fun tU16(v: Int) {
        tiff.add((v and 0xFF).toByte()); tiff.add(((v shr 8) and 0xFF).toByte())
    }
    fun tU32(v: Long) {
        tU16((v and 0xFFFF).toInt()); tU16(((v shr 16) and 0xFFFF).toInt())
    }
    tiff.add('I'.code.toByte()); tiff.add('I'.code.toByte())
    tU16(42); tU32(8) // IFD0 at 8
    tU16(1) // one entry: GPS IFD pointer (tag 0x8825)
    tU16(0x8825); tU16(4); tU32(1); tU32(26)
    tU32(0) // next IFD
    // GPS IFD at 26: 4 entries, data region starts at 26 + 2 + 48 + 4 = 80
    tU16(4)
    // tag 1: latRef ASCII inline
    tU16(1); tU16(2); tU32(2)
    tiff.add(latRef[0].code.toByte()); tiff.add(0); tiff.add(0); tiff.add(0)
    // tag 2: latitude rationals at 80
    tU16(2); tU16(5); tU32(3); tU32(80)
    // tag 3: lonRef ASCII inline
    tU16(3); tU16(2); tU32(2)
    tiff.add(lonRef[0].code.toByte()); tiff.add(0); tiff.add(0); tiff.add(0)
    // tag 4: longitude rationals at 104
    tU16(4); tU16(5); tU32(3); tU32(104)
    tU32(0)
    // lat data at 80: 23/1, 30/1, 0/1
    tU32(23); tU32(1); tU32(30); tU32(1); tU32(0); tU32(1)
    // lon data at 104: 46/1, 30/1, 0/1
    tU32(46); tU32(1); tU32(30); tU32(1); tU32(0); tU32(1)
    out.add(0xFF.toByte()); out.add(0xE1.toByte())
    val segLen = 6 + tiff.size + 2
    out.add(((segLen shr 8) and 0xFF).toByte()); out.add((segLen and 0xFF).toByte())
    "Exif\u0000\u0000".forEach { out.add(it.code.toByte()) }
    out.addAll(tiff)
    out.add(0xFF.toByte()); out.add(0xD9.toByte()) // EOI
    return out.toByteArray()
}

class ExifClaimTest {

    @Test
    fun parsesGpsWithHemisphereSigns() {
        val gps = readExifGps(syntheticGpsJpeg())!!
        assertEquals(-23.5, gps.latitude, 1e-6)
        assertEquals(-46.5, gps.longitude, 1e-6)
        val north = readExifGps(syntheticGpsJpeg("N", "E"))!!
        assertEquals(23.5, north.latitude, 1e-6)
        assertEquals(46.5, north.longitude, 1e-6)
    }

    @Test
    fun rejectsNonExifInput() {
        assertNull(readExifGps(byteArrayOf(0xFF.toByte(), 0xD8.toByte(), 0xFF.toByte(), 0xD9.toByte())))
        assertNull(readExifGps("not a jpeg".encodeToByteArray()))
        assertNull(readExifGps(ByteArray(0)))
    }

    @Test
    fun disagreementFlagsFarExifOnly() {
        val exif = ExifGps(-23.5, -46.5)
        assertFalse(exifDisagrees(-23.5, -46.5, exif))
        assertFalse(exifDisagrees(-23.5001, -46.5001, exif))
        assertTrue(exifDisagrees(-23.6, -46.6, exif))
        assertTrue(distanceMeters(-23.5, -46.5, -23.5, -46.5) < 1.0)
    }
}
