package vault.ui.property

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
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
        if (showForm) {
            OutlinedButton(
                onClick = onToggle,
                enabled = !busy,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
            ) { Text(PropertyStrings.closeCreateForm) }
        } else {
            Button(
                onClick = onToggle,
                enabled = !busy,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp),
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(Icons.Filled.Add, contentDescription = null)
                    Text(Strings.newProperty, style = MaterialTheme.typography.titleMedium)
                }
            }
        }
    }
    if (showForm) {
        item {
            OutlinedCard(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp)) {
                Column(modifier = Modifier.padding(16.dp)) {
                    PropertyAddressForm(
                        nickname, onNickname, street, onStreet, number, onNumber,
                        complement, onComplement, neighborhood, onNeighborhood,
                        showNickname = showNickname,
                    )
                }
            }
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
