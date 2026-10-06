package vault.ui.journey

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import kotlinx.coroutines.launch
import androidx.compose.ui.unit.dp
import vault.local.LocalSession
import vault.local.db.LocalEvidence
import vault.ui.ErrorText
import vault.ui.userErrorMessage

// Manual display correction during DRAFT. Original bytes never change;
// the chosen quarter turn is stored as metadata and frozen in the
// Manifest at seal. Sealed photos show the frozen value without editing.
@Composable
fun OrientationCorrectionRow(
    session: LocalSession,
    evidence: LocalEvidence,
    sealed: Boolean,
    onChanged: () -> Unit,
) {
    val scope = rememberCoroutineScope()
    var busy by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    if (sealed) {
        if (evidence.displayRotation != 0) {
            Text("Exibição corrigida: ${evidence.displayRotation}° (congelada no lacre)",
                style = MaterialTheme.typography.bodySmall)
        }
        return
    }
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        listOf(0, 90, 180, 270).forEach { degrees ->
            FilterChip(
                selected = evidence.displayRotation == degrees,
                enabled = !busy,
                onClick = {
                    busy = true
                    error = null
                    scope.launch {
                        try {
                            session.correctEvidenceOrientation(evidence.id, degrees)
                            onChanged()
                        } catch (e: Exception) {
                            error = userErrorMessage(e)
                        } finally {
                            busy = false
                        }
                    }
                },
                label = { Text(if (degrees == 0) "0°" else "${degrees}°") },
            )
        }
    }
    Text("Correção de exibição; o original é preservado e o valor é congelado no lacre.",
        style = MaterialTheme.typography.bodySmall)
    ErrorText(error)
}
