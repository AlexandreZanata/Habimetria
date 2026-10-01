package vault.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import vault.nav.Route
import vault.nav.WindowClass
import vault.nav.maxContentWidthDp
import vault.nav.windowClassFor

// VaultShell is the single inset boundary: one Scaffold consumes system,
// gesture and keyboard insets once, hosting the shared header, the
// destination bar/rail and the content. Compact windows use a bottom
// bar; medium/expanded windows use a side rail (same four destinations,
// existing vault.nav window classes). Detail screens keep one labeled
// Back control; destination roots show none. Content centers under a
// width cap on tablets/foldables; camera surfaces inside may still draw
// under system bars, controls never do.
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VaultShell(
    title: String,
    canGoBack: Boolean,
    onBack: () -> Unit,
    destination: Route,
    onSelect: (Route) -> Unit,
    subtitle: String,
    content: @Composable () -> Unit,
) {
    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val windowClass = windowClassFor(maxWidth.value.toInt())
        Scaffold(
            topBar = {
                Column {
                    TopAppBar(
                        title = {
                            Column {
                                Text(title, maxLines = 1)
                                if (subtitle.isNotBlank()) {
                                    Text(
                                        subtitle,
                                        maxLines = 1,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                            }
                        },
                        navigationIcon = {
                            if (canGoBack) {
                                TextButton(
                                    onClick = onBack,
                                    modifier = Modifier.heightIn(min = 48.dp),
                                ) { Text(Strings.back) }
                            }
                        },
                        colors = TopAppBarDefaults.topAppBarColors(
                            containerColor = MaterialTheme.colorScheme.surfaceContainer,
                        ),
                    )
                    // Header and destination bar share the darker container
                    // with a hairline, so chrome reads apart from content.
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                }
            },
            bottomBar = {
                if (windowClass == WindowClass.COMPACT) {
                    Column {
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                        DestinationsBar(selected = destination, onSelect = onSelect)
                    }
                }
            },
        ) { inner ->
            Row(modifier = Modifier.padding(inner).fillMaxSize()) {
                if (windowClass != WindowClass.COMPACT) {
                    DestinationsRail(selected = destination, onSelect = onSelect)
                }
                BoxWithConstraints(modifier = Modifier.weight(1f).fillMaxSize()) {
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
    }
}
