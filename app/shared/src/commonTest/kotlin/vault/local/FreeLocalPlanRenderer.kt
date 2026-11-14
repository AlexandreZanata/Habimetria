package vault.local

import kotlin.test.assertNotNull
import kotlin.test.assertTrue

// Drawing-port test double, not a native PDF writer or visual acceptance.
// Exercise the real layout/source pipeline and require every planned photo.
internal class FreeLocalPlanRenderer : PlanRenderer {
    val photoIds = mutableListOf<String>()
    val text = mutableListOf<String>()
    var calls = 0
        private set

    override suspend fun render(
        pages: List<RenderPage>,
        loadPhoto: suspend (String) -> ByteArray?,
        onProgress: (Int, Int) -> Unit,
        isCancelled: () -> Boolean,
    ): ByteArray {
        calls++
        for ((index, page) in pages.withIndex()) {
            if (isCancelled()) throw RenderCancelledException()
            for (item in page.items + page.chrome) when (item) {
                is PageItem.Photo -> {
                    val bytes = assertNotNull(loadPhoto(item.entry.evidenceId))
                    assertNotNull(imageDimensions(bytes))
                    photoIds += item.entry.evidenceId
                }
                is PageItem.Text -> text += item.text
                is PageItem.Qr -> assertTrue(item.content.startsWith("vault-report/v1/"))
                else -> Unit
            }
            onProgress(index + 1, pages.size)
        }
        return ("code-test-rendered-plan\n" + text.joinToString("\n")).encodeToByteArray()
    }
}
