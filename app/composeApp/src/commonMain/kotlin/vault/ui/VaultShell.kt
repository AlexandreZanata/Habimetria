package vault.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import vault.nav.maxContentWidthDp
import vault.nav.windowClassFor

// VaultShell is the single inset boundary: Scaffold consumes system,
// gesture and keyboard insets once. Content centers under a width cap on
// tablets/foldables so controls never stretch edge to edge; camera
// surfaces inside may still draw under system bars, controls never do.
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VaultShell(
    title: String,
    canGoBack: Boolean,
    onBack: () -> Unit,
    content: @Composable () -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(title) },
                navigationIcon = {
                    if (canGoBack) {
                        TextButton(
                            onClick = onBack,
                            modifier = Modifier.heightIn(min = 48.dp),
                        ) { Text(Strings.back) }
                    }
                },
            )
        },
    ) { inner ->
        BoxWithConstraints(modifier = Modifier.padding(inner).fillMaxSize()) {
            val cap = maxContentWidthDp(windowClassFor(maxWidth.value.toInt()))
            val width = if (cap > 0) {
                Modifier.widthIn(max = cap.dp).align(Alignment.TopCenter).fillMaxWidth()
            } else {
                Modifier.fillMaxWidth()
            }
            Box(modifier = width) { content() }
        }
    }
}
