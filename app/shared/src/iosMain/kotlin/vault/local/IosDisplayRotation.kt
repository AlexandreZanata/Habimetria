package vault.local

// iOS manual display rotation is not verified in this delivery (iOS
// checking excluded by user instruction). Dimensions already carry the
// frozen rotation so layout never stretches; pixel rotation needs manual
// validation on a supported device before any correctness claim.
actual fun rotateDisplayBytes(bytes: ByteArray, degrees: Int): ByteArray {
    requireDisplayRotation(degrees)
    return bytes
}
