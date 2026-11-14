package vault.ui.journey

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import vault.ui.ErrorText
import vault.ui.RecordPanel
import vault.ui.Strings

@Composable
internal fun RoomEmptyState() {
    RecordPanel(Icons.Outlined.Home, JourneyStrings.roomsEmptyTitle, JourneyStrings.roomsEmptyHint)
}

@Composable
internal fun RoomSetupForm(
    isExit: Boolean,
    error: String?,
    value: String,
    onValue: (String) -> Unit,
    canAdd: Boolean,
    onAdd: () -> Unit,
    presetCount: Int,
    onAddPresets: () -> Unit,
) {
    val keyboard = LocalSoftwareKeyboardController.current
    val canSubmit = canAdd && value.isNotBlank()
    fun submit() {
        if (canSubmit) {
            onAdd()
            keyboard?.hide()
        }
    }

    OutlinedCard(modifier = Modifier.fillMaxWidth(), shape = MaterialTheme.shapes.large) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            if (isExit) {
                Text(Strings.exitKind, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
            }
            Text(
                JourneyStrings.roomSetupTitle,
                color = MaterialTheme.colorScheme.primary,
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.semantics { heading() },
            )
            Text(JourneyStrings.roomSetupHint, style = MaterialTheme.typography.bodyMedium)
            OutlinedTextField(
                value = value,
                onValueChange = onValue,
                label = { Text(Strings.roomNameLabel) },
                supportingText = { Text(JourneyStrings.roomNameExample) },
                singleLine = true,
                shape = MaterialTheme.shapes.medium,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { submit() }),
                modifier = Modifier.fillMaxWidth(),
            )
            ErrorText(error)
            JourneyPrimaryAction(
                text = JourneyStrings.addRoom,
                icon = Icons.Filled.Add,
                onClick = ::submit,
                enabled = canSubmit,
            )
            if (presetCount > 0) {
                TextButton(onClick = onAddPresets, enabled = canAdd) {
                    Text("${Strings.addPresetRooms} ($presetCount)")
                }
            }
        }
    }
}
