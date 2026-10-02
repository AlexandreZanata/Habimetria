package vault.ui.property

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.FilterChip
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import vault.local.STATUS_DRAFT
import vault.local.STATUS_SEALED
import vault.local.db.LocalInspection
import vault.local.db.LocalProperty
import vault.ui.AppContainer
import vault.ui.ErrorText
import vault.ui.ProgressStatus
import vault.ui.Strings
import vault.ui.userErrorMessage


@Composable
internal fun InspectionRow(
    inspection: LocalInspection,
    busy: Boolean,
    onResume: () -> Unit,
    onReview: () -> Unit,
) {
    val sealed = inspection.status == STATUS_SEALED
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            if (sealed) PropertyStrings.sealedLabel else PropertyStrings.draftLabel,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        OutlinedButton(onClick = { if (sealed) onReview() else onResume() }, enabled = !busy) {
            Text(if (sealed) Strings.review else Strings.continueAction)
        }
    }
}
