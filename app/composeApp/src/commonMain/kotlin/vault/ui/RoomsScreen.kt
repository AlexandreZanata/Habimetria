package vault.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import vault.api.Room

// Rooms: template rooms with per-room photo counts. Tap to capture.
@Composable
fun RoomsScreen(
    container: AppContainer,
    inspectionId: String,
    onOpenRoom: (roomId: String, label: String) -> Unit,
    onReview: () -> Unit,
    onBack: () -> Unit,
) {
    var rooms by remember { mutableStateOf<List<Room>>(emptyList()) }
    var counts by remember { mutableStateOf(mapOf<String, Int>()) }
    var error by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(inspectionId) {
        try {
            rooms = container.api.listRooms(inspectionId)
            counts = container.api.coverage(inspectionId).rooms.associate { it.roomId to it.photos }
        } catch (e: Exception) {
            error = e.message?.take(160)
        }
    }

    ScreenColumn {
        ScreenTitle(Strings.rooms)
        ErrorText(error)
        rooms.forEach { room ->
            Row(
                modifier = Modifier.fillMaxWidth().clickable { onOpenRoom(room.id, room.label) }.padding(8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(room.label, style = MaterialTheme.typography.bodyLarge)
                Text(
                    "${counts[room.id] ?: 0} ${Strings.photosSuffix}",
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        }
        VaultButton(text = Strings.review, onClick = onReview)
        VaultButton(text = Strings.back, onClick = onBack)
    }
}
