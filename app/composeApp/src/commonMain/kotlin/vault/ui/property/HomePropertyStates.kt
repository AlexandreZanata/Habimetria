package vault.ui.property

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import vault.local.PropertyOverview
import vault.ui.ErrorText
import vault.ui.ProgressStatus
import vault.ui.PropertyCardSkeleton
import vault.ui.Strings
import vault.ui.rememberLoadingVisible

// Shared loading/error/empty/content states for property rows.
// Home and Imóveis reuse the same distinct states with retry. The header
// links to the full list; there are no stat cards on Home.
@Composable
fun HomePropertyStates(
    rows: List<PropertyOverview>?,
    loadError: String?,
    busy: Boolean,
    onRetry: () -> Unit,
    onResume: (String) -> Unit,
    onNewInspection: (String) -> Unit,
    onOpenDetail: (PropertyOverview) -> Unit,
    onOpenAllProperties: () -> Unit,
    onRemove: ((PropertyOverview) -> Unit)? = null,
) {
    val showLoading = rememberLoadingVisible(rows == null)
    when {
        loadError != null -> {
            ErrorText(loadError)
            OutlinedButton(onClick = onRetry) { Text(Strings.retry) }
        }
        showLoading -> {
            ProgressStatus(true)
            PropertyCardSkeleton()
            PropertyCardSkeleton()
        }
        rows == null -> {
            // Grace period: data is on its way, paint nothing yet.
        }
        rows.isEmpty() -> Text(Strings.propertiesEmpty)
        else -> {
            val countText =
                "${rows.size} ${if (rows.size == 1) Strings.propertySingular else Strings.propertyPlural}"
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(Strings.myProperties, style = MaterialTheme.typography.titleLarge)
                TextButton(
                    onClick = onOpenAllProperties,
                    modifier = Modifier.semantics { contentDescription = "$countText: ${Strings.properties}" },
                ) {
                    Text(
                        countText,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Icon(
                        Icons.Filled.ChevronRight,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            rows.forEach { overview ->
                PropertyCardView(
                    overview = overview,
                    busy = busy,
                    onResume = onResume,
                    onNewInspection = { onNewInspection(overview.property.id) },
                    onOpenDetail = { onOpenDetail(overview) },
                    onRemove = onRemove?.let { remove -> { remove(overview) } },
                )
            }
        }
    }
}
