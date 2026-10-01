package vault.ui.report

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import vault.local.ReportOverview
import vault.ui.Strings
import vault.ui.property.PropertyStrings

// Finalized report row: open/verify entries plus a red delete action
// that confirms through ReportDeleteHost before touching anything.
@Composable
fun FinalizedRow(
    overview: ReportOverview,
    onOpen: () -> Unit,
    onVerify: () -> Unit,
    onDelete: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        OutlinedButton(onClick = onOpen, modifier = Modifier.weight(1f)) {
            Text(PropertyStrings.openDetail)
        }
        OutlinedButton(onClick = onVerify, modifier = Modifier.weight(1f)) {
            Text(Strings.pdfVerify)
        }
        IconButton(onClick = onDelete) {
            Icon(
                Icons.Filled.Delete,
                contentDescription = ReportStrings.deleteDescription,
                tint = MaterialTheme.colorScheme.error,
            )
        }
    }
}
