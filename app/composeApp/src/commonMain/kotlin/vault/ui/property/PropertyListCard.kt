package vault.ui.property

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import vault.local.NavigationProgress
import vault.local.PropertyOverview
import vault.local.db.PropertyRestoreOutcome
import vault.ui.AppContainer
import vault.ui.Strings
import vault.ui.userErrorMessage

// Management actions belong inside the Property card. The screen's scope
// owns writes so a filter change cannot cancel restoration mid-operation.
@Composable
internal fun PropertyListCard(
    container: AppContainer,
    overview: PropertyOverview,
    scope: CoroutineScope,
    busy: Boolean,
    onBusy: (Boolean) -> Unit,
    onError: (String) -> Unit,
    onReady: (String) -> Unit,
    onOpenDetail: () -> Unit,
    onRemove: () -> Unit,
    onRestored: suspend () -> Unit,
) {
    val property = overview.property
    val title = property.nickname.ifBlank { property.address }
    PropertyCardView(
        overview = overview,
        busy = busy,
        onResume = { id ->
            scope.launch {
                try {
                    container.local?.navigation?.save(NavigationProgress(id))
                    onReady(id)
                } catch (e: Exception) {
                    onError(userErrorMessage(e))
                }
            }
        },
        onNewInspection = {
            scope.launch {
                onBusy(true)
                try {
                    startOnProperty(container, property.id, onReady)
                } catch (e: Exception) {
                    onError(userErrorMessage(e))
                } finally {
                    onBusy(false)
                }
            }
        },
        onOpenDetail = onOpenDetail,
        onRemove = onRemove,
        actions = {
            Column {
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                TextButton(
                    onClick = onOpenDetail,
                    enabled = !busy,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp).semantics {
                        contentDescription = "${PropertyStrings.openProperty}: $title"
                    },
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            PropertyStrings.openProperty,
                            style = MaterialTheme.typography.labelLarge,
                            modifier = Modifier.weight(1f),
                        )
                        Icon(Icons.Filled.ChevronRight, contentDescription = null)
                    }
                }
                if (property.archived) {
                    RestoreButton(busy = busy) {
                        scope.launch {
                            onBusy(true)
                            try {
                                when (container.local?.propertyRemoval?.restore(property.id)) {
                                    PropertyRestoreOutcome.Restored,
                                    PropertyRestoreOutcome.AlreadyActive,
                                    -> onRestored()
                                    PropertyRestoreOutcome.Missing, null -> onError(Strings.recordMissing)
                                }
                            } catch (e: Exception) {
                                onError(userErrorMessage(e))
                            } finally {
                                onBusy(false)
                            }
                        }
                    }
                }
            }
        },
    )
}
