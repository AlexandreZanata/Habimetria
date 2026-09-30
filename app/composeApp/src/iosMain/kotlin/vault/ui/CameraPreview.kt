package vault.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

// iOS camera host (AVFoundation) lands in L4. Compiles today, shoots later.
@Composable
actual fun CameraPreview(modifier: Modifier) {
    Box(modifier) {
        Text(Strings.cameraComingSoon)
    }
}
