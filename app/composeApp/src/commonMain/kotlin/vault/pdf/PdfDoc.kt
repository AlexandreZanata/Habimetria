package vault.pdf

import vault.api.Finding

// PdfDoc is the platform-neutral layout model. Android draws it with
// PdfDocument, iOS with PDFKit; both from this single description.
data class PdfDoc(
    val title: String,
    val verifyCode: String,
    val verifyUrl: String,
    val manifestSha: String,
    val sealedAt: String,
    val sections: List<RoomSection>,
)

data class RoomSection(
    val label: String,
    val photos: List<DocPhoto>,
    val findings: List<Finding>,
)

data class DocPhoto(
    val bytes: ByteArray,
    val caption: String,
) {
    override fun equals(other: Any?): Boolean =
        other is DocPhoto && bytes.contentEquals(other.bytes) && caption == other.caption

    override fun hashCode(): Int = 31 * bytes.contentHashCode() + caption.hashCode()
}

data class SealBlock(
    val verifyCode: String,
    val verifyUrl: String,
    val manifestSha: String,
    val qrPng: ByteArray,
) {
    override fun equals(other: Any?): Boolean =
        other is SealBlock && verifyCode == other.verifyCode && qrPng.contentEquals(other.qrPng)

    override fun hashCode(): Int = 31 * verifyCode.hashCode() + qrPng.contentHashCode()
}
