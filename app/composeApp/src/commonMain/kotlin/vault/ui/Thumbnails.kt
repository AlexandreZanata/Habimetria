package vault.ui

import androidx.compose.ui.graphics.ImageBitmap

// Scaled thumbnail for review grids. Android decodes with inSampleSize;
// iOS returns null until M4 (Coil lands with PDF thumbs).
expect fun decodeThumbnail(path: String, maxEdge: Int = 320): ImageBitmap?
