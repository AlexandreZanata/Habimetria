package vault.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

// Live camera preview. Android renders PreviewView; iOS shows a
// placeholder until the Swift AVCapture host lands in M3.
@Composable
expect fun CameraPreview(modifier: Modifier = Modifier)
