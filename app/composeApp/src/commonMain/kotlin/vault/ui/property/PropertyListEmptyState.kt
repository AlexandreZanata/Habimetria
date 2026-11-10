package vault.ui.property

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Apartment
import androidx.compose.runtime.Composable
import vault.ui.RecordPanel
import vault.ui.Strings

@Composable
internal fun PropertyListEmptyState(query: String, visibility: String) {
    RecordPanel(
        icon = Icons.Outlined.Apartment,
        title = if (visibility == VISIBILITY_ARCHIVED) PropertyStrings.archivedBadge else Strings.properties,
        body = when {
            query.isNotBlank() -> PropertyStrings.noPropertyMatch
            visibility == VISIBILITY_ARCHIVED -> PropertyStrings.noArchived
            else -> Strings.propertiesEmpty
        },
    )
}
