package vault.ui.property

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
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

    LaunchedEffect(property.id) {
        nickname = property.nickname
        street = property.street
        number = property.number
        complement = property.complement.orEmpty()
        neighborhood = property.neighborhood
        saved = false
    }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        PropertyCoverSection(
            container = container,
            propertyId = property.id,
            photoPath = property.photoPath,
            onSaved = onSaved,
        )
        Text(PropertyStrings.dataTitle, style = MaterialTheme.typography.titleMedium)
        if (!open) {
            OutlinedButton(onClick = { open = true }) { Text(PropertyStrings.editData) }
            return@Column
        }
        PropertyAddressForm(
            nickname, { nickname = it; saved = false },
            street, { street = it; saved = false },
            number, { number = it; saved = false },
            complement, { complement = it; saved = false },
            neighborhood, { neighborhood = it; saved = false },
            showNickname = true,
        )
        ErrorText(error)
        if (saved) {
            Text(
                PropertyStrings.dataSaved,
                color = MaterialTheme.colorScheme.primary,
                style = MaterialTheme.typography.bodyMedium,
            )
        }
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
