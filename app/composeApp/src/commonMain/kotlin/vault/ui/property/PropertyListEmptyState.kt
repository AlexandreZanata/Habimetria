package vault.ui.property

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import vault.ui.Strings

// Empty search, archived list and first-use list have different next steps.
@Composable
internal fun PropertyListEmptyState(query: String, visibility: String) {
    OutlinedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
    ) {
        Text(
            if (query.isNotBlank()) {
                PropertyStrings.noPropertyMatch
            } else if (visibility == VISIBILITY_ARCHIVED) {
                PropertyStrings.noArchived
            } else {
                Strings.propertiesEmpty
            },
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(16.dp),
        )
    }
}
