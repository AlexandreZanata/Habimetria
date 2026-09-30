package vault.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

// VaultShell is the single inset boundary: Scaffold consumes system,
// gesture and keyboard insets once. Screens render inside innerPadding;
// camera surfaces may draw under it, controls never do.
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
        Box(modifier = Modifier.padding(inner)) { content() }
    }
}
