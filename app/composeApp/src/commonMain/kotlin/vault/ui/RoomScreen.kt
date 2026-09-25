package vault.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

// Room: guided checklist placeholder. M2 wires the real camera here;
// the shutter stays the biggest element on screen.
@Composable
fun RoomScreen(
    roomLabel: String,
    shotIndex: Int,
    shotTotal: Int,
    shotHint: String,
    onShutter: () -> Unit,
    onBack: () -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(roomLabel)
        Text("$shotIndex of $shotTotal — $shotHint")
        Button(onClick = onShutter, modifier = Modifier.fillMaxWidth()) {
            Text("Capture")
        }
        Button(onClick = onBack, modifier = Modifier.fillMaxWidth()) {
            Text("Back")
        }
    }
}
