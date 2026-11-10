package vault.ui.property

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.outlined.Apartment
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import vault.local.PropertyOverview
import vault.ui.ErrorText
import vault.ui.LoadingSkeleton
import vault.ui.RecordPanel
import vault.ui.PropertyCardSkeleton
import vault.ui.Strings

// Home cover photos load as their rows enter the viewport.
internal fun LazyListScope.homePropertyItems(
    rows: List<PropertyOverview>?,
    showLoading: Boolean,
    loadError: String?,
    busy: Boolean,
    onRetry: () -> Unit,
    onResume: (String) -> Unit,
    onNewInspection: (String) -> Unit,
    onOpenDetail: (PropertyOverview) -> Unit,
    onOpenAllProperties: () -> Unit,
    onRemove: ((PropertyOverview) -> Unit)? = null,
) {
    item(key = "properties-header") {
        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 4.dp).heightIn(min = 48.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    Strings.myProperties,
                    color = MaterialTheme.colorScheme.primary,
                    style = MaterialTheme.typography.headlineSmall,
                    modifier = Modifier.semantics { heading() },
                )
                if (rows != null && rows.isNotEmpty()) {
                    val noun = if (rows.size == 1) Strings.propertySingular else Strings.propertyPlural
                    Text(
                        "${rows.size} $noun",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            if (!rows.isNullOrEmpty()) {
                TextButton(onClick = onOpenAllProperties) {
                    Text(Strings.viewAll)
                    Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null)
                }
            }
        }
    }
    when {
        loadError != null -> item(key = "properties-error") {
            Column {
                ErrorText(loadError)
                OutlinedButton(onClick = onRetry) { Text(Strings.retry) }
            }
        }
        showLoading -> {
            item(key = "properties-skeleton") {
                LoadingSkeleton { PropertyCardSkeleton(featured = true) }
            }
        }
        rows == null -> Unit
        rows.isEmpty() -> item(key = "properties-empty") {
            RecordPanel(Icons.Outlined.Apartment, Strings.homeEmptyTitle, Strings.homeEmptyBody)
        }
        else -> items(rows, key = { "property:${it.property.id}" }) { overview ->
            PropertyCardView(
                overview = overview,
                busy = busy,
                onResume = onResume,
                onNewInspection = { onNewInspection(overview.property.id) },
                onOpenDetail = { onOpenDetail(overview) },
                onRemove = onRemove?.let { remove -> { remove(overview) } },
                featured = true,
            )
        }
    }
}
