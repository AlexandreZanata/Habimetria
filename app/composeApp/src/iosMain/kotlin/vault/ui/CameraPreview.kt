package vault.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

// iOS camera host (AVCapture) lands in M3. Compiles today, shoots later.
@Composable
actual fun CameraPreview(modifier: Modifier) {
    Box(modifier) {
        Text("Camera arrives in M3")
    }
}
