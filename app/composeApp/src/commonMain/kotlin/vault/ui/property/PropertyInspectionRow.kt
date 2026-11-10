package vault.ui.property

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import vault.local.STATUS_SEALED
import vault.local.db.LocalInspection
import vault.ui.Strings
import vault.ui.inspection.InspectionStatusBadge

@Composable
internal fun InspectionRow(
    inspection: LocalInspection,
    busy: Boolean,
    onResume: () -> Unit,
    onReview: () -> Unit,
) {
    val sealed = inspection.status == STATUS_SEALED
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        InspectionStatusBadge(sealed)
        OutlinedButton(onClick = { if (sealed) onReview() else onResume() }, enabled = !busy) {
            Text(if (sealed) Strings.review else Strings.continueAction)
        }
    }
}
