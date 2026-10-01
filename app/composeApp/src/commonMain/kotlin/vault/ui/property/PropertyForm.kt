package vault.ui.property

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import vault.ui.ErrorText
import vault.ui.Strings
import vault.ui.VaultButton
import vault.ui.VaultTextField

// Address form validation mirrors LocalVaultRepository rules.
fun isPropertyFormValid(street: String, number: String, neighborhood: String): Boolean =
    street.trim().length >= 3 &&
        number.trim().isNotEmpty() &&
        neighborhood.trim().length >= 2

// Checkout-style address form shared by Home and Imóveis.
@Composable
fun PropertyAddressForm(
    nickname: String,
    onNickname: (String) -> Unit,
    street: String,
    onStreet: (String) -> Unit,
    number: String,
    onNumber: (String) -> Unit,
    complement: String,
    onComplement: (String) -> Unit,
    neighborhood: String,
    onNeighborhood: (String) -> Unit,
    showNickname: Boolean,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        if (showNickname) {
            VaultTextField(
                value = nickname,
                onValueChange = onNickname,
                label = Strings.nicknameLabel,
                modifier = Modifier.fillMaxWidth(),
            )
        }
        VaultTextField(
            value = street,
            onValueChange = onStreet,
            label = Strings.streetLabel,
            modifier = Modifier.fillMaxWidth(),
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            VaultTextField(
                value = number,
                onValueChange = onNumber,
                label = Strings.numberLabel,
                modifier = Modifier.weight(1f),
            )
            VaultTextField(
                value = complement,
                onValueChange = onComplement,
                label = Strings.complementLabel,
                modifier = Modifier.weight(2f),
            )
        }
        VaultTextField(
            value = neighborhood,
            onValueChange = onNeighborhood,
            label = Strings.neighborhoodLabel,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

// Creation block for lazy lists: toggle, address form, error, submit.
// Emits four items so Imóveis keeps one scroll position with retry.
fun LazyListScope.propertyCreateItems(
    showForm: Boolean,
    onToggle: () -> Unit,
    nickname: String,
    onNickname: (String) -> Unit,
    street: String,
    onStreet: (String) -> Unit,
    number: String,
    onNumber: (String) -> Unit,
    complement: String,
    onComplement: (String) -> Unit,
    neighborhood: String,
    onNeighborhood: (String) -> Unit,
    showNickname: Boolean,
    error: String?,
    busy: Boolean,
    onSubmit: () -> Unit,
) {
    item {
        OutlinedButton(onClick = onToggle, modifier = Modifier.fillMaxWidth()) {
            Text(Strings.newProperty)
        }
    }
    if (showForm) {
        item {
            PropertyAddressForm(
                nickname, onNickname, street, onStreet, number, onNumber,
                complement, onComplement, neighborhood, onNeighborhood,
                showNickname = showNickname,
            )
        }
        item { ErrorText(error) }
        item {
            VaultButton(
                text = if (busy) Strings.starting else Strings.start,
                onClick = onSubmit,
                enabled = !busy && isPropertyFormValid(street, number, neighborhood),
            )
        }
    }
}

// Rename dialog for the property nickname. Organizational only.
@Composable
fun PropertyRenameDialog(
    currentTitle: String,
    draft: String,
    onDraft: (String) -> Unit,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(Strings.nicknameTitle) },
        text = {
            VaultTextField(value = draft, onValueChange = onDraft, label = currentTitle)
        },
        confirmButton = { TextButton(onClick = onConfirm) { Text(Strings.save) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text(Strings.cancel) } },
    )
}
