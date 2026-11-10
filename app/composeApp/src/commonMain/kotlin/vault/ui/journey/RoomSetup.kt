package vault.ui.journey

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import vault.ui.ErrorText
import vault.ui.RecordPanel
import vault.ui.Strings

// The field and keyboard share the same explicit add operation.
@Composable
fun RoomNameInput(value: String, onValue: (String) -> Unit, enabled: Boolean, onAdd: () -> Unit) {
    val keyboard = LocalSoftwareKeyboardController.current
    fun submit() {
        if (enabled) {
            onAdd()
            keyboard?.hide()
        }
    }
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.Bottom) {
        OutlinedTextField(
            value = value,
            onValueChange = onValue,
            label = { Text(Strings.roomNameLabel) },
            placeholder = { Text(Strings.roomNamePlaceholder) },
            leadingIcon = { Icon(Icons.Outlined.Home, contentDescription = null) },
            singleLine = true,
            shape = MaterialTheme.shapes.medium,
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = MaterialTheme.colorScheme.surfaceContainer,
                unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainer,
                unfocusedBorderColor = MaterialTheme.colorScheme.outline,
            ),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = { submit() }),
            modifier = Modifier.weight(1f),
        )
        Button(
            onClick = { submit() },
            enabled = enabled,
            shape = MaterialTheme.shapes.medium,
            modifier = Modifier.size(56.dp),
            contentPadding = PaddingValues(0.dp),
        ) { Icon(Icons.Filled.Add, contentDescription = Strings.add) }
    }
}

@Composable
fun RoomEmptyState() {
    RecordPanel(Icons.Outlined.Home, Strings.roomsEmptyTitle, Strings.roomsEmptyHint)
}

// Intro and optional presets belong to the same scrollable setup section.
@Composable
fun RoomSetupHeader(
    isExit: Boolean,
    error: String?,
    value: String,
    onValue: (String) -> Unit,
    onAdd: () -> Unit,
    presetCount: Int,
    onAddPresets: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        if (isExit) {
            Text(Strings.exitKind, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
        }
        Text(Strings.roomsSubtitle, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        ErrorText(error)
        RoomNameInput(value, onValue, value.isNotBlank(), onAdd)
        if (presetCount > 0) {
            TextButton(onClick = onAddPresets) { Text("${Strings.addPresetRooms} ($presetCount)") }
        }
    }
}
