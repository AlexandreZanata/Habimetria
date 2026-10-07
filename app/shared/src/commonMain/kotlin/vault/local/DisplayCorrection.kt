package vault.local

// Manual display correction applied to derivatives only. Original bytes
// never change; the chosen rotation is frozen in the Manifest before seal
// and reflected in the sealed display dimensions. Allowed values are
// quarter turns in degrees clockwise.
const val DISPLAY_ROTATION_NONE = 0
const val DISPLAY_ROTATION_90 = 90
const val DISPLAY_ROTATION_180 = 180
const val DISPLAY_ROTATION_270 = 270

fun validDisplayRotation(degrees: Int): Boolean =
    degrees == 0 || degrees == 90 || degrees == 180 || degrees == 270

fun requireDisplayRotation(degrees: Int): Int {
    require(validDisplayRotation(degrees)) { "display rotation must be 0/90/180/270" }
    return degrees
}

// Final display size after the manual correction. A 90/270 turn swaps
// width and height; 0/180 keeps them.
fun correctedDisplayDims(base: ImageDims, rotation: Int): ImageDims {
    requireDisplayRotation(rotation)
    return if (rotation == 90 || rotation == 270) ImageDims(base.height, base.width) else base
}

fun correctedPhotoDisplay(base: PhotoDisplay, rotation: Int): PhotoDisplay {
    val dims = correctedDisplayDims(base.dimensions(), rotation)
    return base.copy(displayWidth = dims.width, displayHeight = dims.height)
}

// Rotates EXIF-normalized JPEG bytes for display/PDF drawing. Originals
// never change; this applies only to derivatives. Platform actuals decode,
// rotate and re-encode; iOS returns input pending manual validation.
expect fun rotateDisplayBytes(bytes: ByteArray, degrees: Int): ByteArray
