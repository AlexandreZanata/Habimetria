package vault.ui.property

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import vault.local.PropertyOverview
import vault.ui.ErrorText
import vault.ui.ProgressStatus
import vault.ui.Strings

// Shared loading/error/empty/content states for property rows.
// Home and Imóveis reuse the same distinct states with retry.
@Composable
fun HomePropertyStates(
    rows: List<PropertyOverview>?,
    loadError: String?,
    busy: Boolean,
    onRetry: () -> Unit,
    onResume: (String) -> Unit,
    onNewInspection: (String) -> Unit,
    onRename: (PropertyOverview) -> Unit,
    onRemove: ((PropertyOverview) -> Unit)? = null,
) {
    when {
        loadError != null -> {
            ErrorText(loadError)
            OutlinedButton(onClick = onRetry) { Text(Strings.retry) }
        }
        rows == null -> {
            ProgressStatus(true)
            Text(Strings.loadingProperties)
        }
        rows.isEmpty() -> Text(Strings.propertiesEmpty)
        else -> {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(Strings.myProperties, style = MaterialTheme.typography.titleLarge)
                Text(
                    "${rows.size} ${if (rows.size == 1) Strings.propertySingular else Strings.propertyPlural}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            rows.forEach { overview ->
                PropertyCardView(
                    overview = overview,
                    busy = busy,
                    onResume = onResume,
                    onNewInspection = { onNewInspection(overview.property.id) },
                    onRename = { onRename(overview) },
                    onRemove = onRemove?.let { remove -> { remove(overview) } },
                )
            }
        }
    }
}
