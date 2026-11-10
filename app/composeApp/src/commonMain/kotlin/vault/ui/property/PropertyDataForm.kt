package vault.ui.property

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.TextButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import vault.local.db.LocalProperty
import vault.ui.AppContainer
import vault.ui.ErrorText
import vault.ui.Strings
import vault.ui.VaultButton
import vault.ui.userErrorMessage

// Editable property data on the detail screen: nickname plus every
// address part, same rules as creation. Collapsed behind one toggle so
// history stays the focus; saving reloads the detail above.
@Composable
fun PropertyDataSection(
    container: AppContainer,
    property: LocalProperty,
    onSaved: () -> Unit,
) {
    var open by rememberSaveable(property.id) { mutableStateOf(false) }
    var nickname by rememberSaveable(property.id) { mutableStateOf(property.nickname) }
    var street by rememberSaveable(property.id) { mutableStateOf(property.street) }
    var number by rememberSaveable(property.id) { mutableStateOf(property.number) }
    var complement by rememberSaveable(property.id) { mutableStateOf(property.complement.orEmpty()) }
    var neighborhood by rememberSaveable(property.id) { mutableStateOf(property.neighborhood) }
    var busy by remember { mutableStateOf(false) }
    var error by rememberSaveable(property.id) { mutableStateOf<String?>(null) }
    var saved by rememberSaveable(property.id) { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    OutlinedCard(modifier = Modifier.fillMaxWidth(), shape = MaterialTheme.shapes.large) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        PropertyStrings.dataTitle,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.semantics { heading() },
                    )
                    Text(
                        PropertyStrings.dataHint,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Box(Modifier.weight(0.65f), contentAlignment = Alignment.CenterEnd) {
                    TextButton(onClick = { open = !open }, enabled = !busy) {
                        Text(if (open) PropertyStrings.closeDataEdit else PropertyStrings.editData)
                    }
                }
            }
            if (saved) RemovalFeedback(PropertyStrings.dataSaved)
            if (!open) return@Column
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            PropertyAddressForm(
                nickname, { nickname = it; saved = false },
                street, { street = it; saved = false },
                number, { number = it; saved = false },
                complement, { complement = it; saved = false },
                neighborhood, { neighborhood = it; saved = false },
                showNickname = true,
            )
            ErrorText(error)
            VaultButton(
                text = if (busy) Strings.saving else Strings.save,
                onClick = {
                    busy = true
                    error = null
                    scope.launch {
                        try {
                            container.local?.repository?.updatePropertyDetails(
                                property.id, nickname, street, number,
                                complement.ifBlank { null }, neighborhood,
                            )
                            saved = true
                            open = false
                            onSaved()
                        } catch (e: Exception) {
                            error = userErrorMessage(e)
                        } finally {
                            busy = false
                        }
                    }
                },
                enabled = !busy && isPropertyFormValid(street, number, neighborhood),
            )
        }
    }
}
