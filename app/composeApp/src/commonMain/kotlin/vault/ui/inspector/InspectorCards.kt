package vault.ui.inspector

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Photo
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import vault.local.InspectorGap
import vault.local.ShotCategory
import vault.local.SuggestionKind
import vault.local.db.AiSuggestion
import vault.local.db.LocalEvidence
import vault.ui.journey.JourneyStrings
import vault.ui.shotLabel

@Composable
internal fun InspectorGapCard(
    gap: InspectorGap,
    reminders: List<AiSuggestion>,
    editable: Boolean,
    busy: Boolean,
    onIgnore: (String) -> Unit,
) {
    InspectorCard {
        Text(gap.roomLabel, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
        gap.missing.forEach { category ->
            val reminder = reminders.firstOrNull { it.detail == category.name }
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(shotLabel(category), modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
                if (editable && reminder != null) {
                    TextButton(onClick = { onIgnore(reminder.id) }, enabled = !busy) { Text(InspectorStrings.ignore) }
                }
            }
        }
    }
}

@Composable
internal fun InspectorPhotoCard(
    photo: LocalEvidence?,
    roomLabel: String?,
    hints: List<AiSuggestion>,
    editable: Boolean,
    busy: Boolean,
    onPreview: (LocalEvidence) -> Unit,
    onConfirm: (String) -> Unit,
    onIgnore: (String) -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    InspectorCard {
        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Surface(
                color = colors.secondaryContainer,
                contentColor = colors.onSecondaryContainer,
                shape = MaterialTheme.shapes.small,
            ) {
                Icon(Icons.Outlined.Photo, contentDescription = null, modifier = Modifier.size(40.dp).padding(10.dp))
            }
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                val number = photo?.let { JourneyStrings.photoNumber(it.sequence) }
                Text(
                    number ?: InspectorStrings.unavailablePhoto,
                    style = MaterialTheme.typography.titleMedium,
                    color = colors.primary,
                )
                val category = ShotCategory.parse(photo?.shotCategory)?.let(::shotLabel)
                val context = listOfNotNull(roomLabel, category).joinToString(" · ")
                if (context.isNotBlank()) {
                    Text(context, style = MaterialTheme.typography.bodySmall, color = colors.onSurfaceVariant)
                }
            }
        }
        if (photo != null) {
            TextButton(onClick = { onPreview(photo) }, enabled = !busy) { Text(InspectorStrings.viewPhoto) }
        }
        hints.forEach { hint ->
            HorizontalDivider(color = colors.outlineVariant)
            val kind = SuggestionKind.parse(hint.kind)
            val title = when (kind) {
                SuggestionKind.POSSIBLE_STAIN -> InspectorStrings.reviewStain
                SuggestionKind.POSSIBLE_CRACK -> InspectorStrings.reviewCrack
                else -> InspectorStrings.reviewPhoto
            }
            Text(title, style = MaterialTheme.typography.titleSmall)
            if (editable) {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    val action = when (kind) {
                        SuggestionKind.POSSIBLE_STAIN -> InspectorStrings.recordStain
                        SuggestionKind.POSSIBLE_CRACK -> InspectorStrings.recordCrack
                        else -> null
                    }
                    if (photo != null && action != null) {
                        OutlinedButton(
                            onClick = { onConfirm(hint.id) },
                            enabled = !busy,
                            shape = MaterialTheme.shapes.medium,
                        ) { Text(action) }
                    }
                    TextButton(onClick = { onIgnore(hint.id) }, enabled = !busy) { Text(InspectorStrings.ignore) }
                }
            }
        }
    }
}

@Composable
private fun InspectorCard(content: @Composable () -> Unit) {
    val colors = MaterialTheme.colorScheme
    OutlinedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.outlinedCardColors(containerColor = colors.surface),
        border = BorderStroke(1.dp, colors.outlineVariant),
    ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            content()
        }
    }
}
