package vault.ui.property

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import vault.ui.ErrorText
import vault.ui.Strings

// Confirmation dialog for U08 removal. Presentational only: the host
// chooses Excluir vs Arquivar wording from a fresh eligibility snapshot
// and commits through the transactional use cases. Cancel is side-effect
// free; confirm is busy-guarded against double activation.
@Composable
fun PropertyRemovalDialog(
    propertyLabel: String,
    headline: String,
    body: String,
    confirmLabel: String,
    busy: Boolean,
    error: String?,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = { if (!busy) onDismiss() },
        title = { Text(headline) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    propertyLabel,
                    style = MaterialTheme.typography.titleMedium,
                )
                Text(
                    body,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                ErrorText(error)
            }
        },
        confirmButton = {
            TextButton(onClick = onConfirm, enabled = !busy) { Text(confirmLabel) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !busy) { Text(Strings.cancel) }
        },
    )
}
