package vault.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import vault.nav.Route
import vault.nav.WindowClass
import vault.nav.maxContentWidthDp
import vault.nav.windowClassFor
import vault.ui.brand.HabimetriaMark

private val MastheadPadding = 20.dp
private val MastheadMarkSize = 36.dp
private val MastheadSpacing = 10.dp

// VaultShell is the single inset boundary: one Scaffold consumes system,
// gesture and keyboard insets once, hosting the shared header, the
// destination bar/rail and the content. Compact windows use a bottom
// bar; medium/expanded windows use a side rail (same four destinations,
// existing vault.nav window classes). Detail screens keep one arrow Back
// control (Material arrow, 48dp target); destination roots show none.
// Content centers under a width cap on tablets/foldables; camera
// surfaces inside may still draw under system bars, controls never do.
@Composable
fun VaultShell(
    title: String,
    canGoBack: Boolean,
    onBack: () -> Unit,
    destination: Route,
    onSelect: (Route) -> Unit,
    content: @Composable () -> Unit,
) {
    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val windowClass = windowClassFor(maxWidth.value.toInt())
        Scaffold(
            containerColor = MaterialTheme.colorScheme.background,
            topBar = {
                VaultMasthead(title, canGoBack, onBack)
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
            // Single inset boundary: Scaffold padding is consumed here so
            // screen-level imePadding() never double-counts it — the fix
            // for the dead gap above the keyboard on every device.
            Row(modifier = Modifier.padding(inner).consumeWindowInsets(inner).fillMaxSize()) {
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

@Composable
private fun VaultMasthead(
    title: String,
    canGoBack: Boolean,
    onBack: () -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    val dark = LocalHabimetriaDarkTheme.current
    val background = if (dark) colors.background else colors.inverseSurface
    val foreground = if (dark) colors.primary else BrandWhite
    Column(
        Modifier.fillMaxWidth().background(background)
            .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Top + WindowInsetsSides.Horizontal)),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth()
                .height(64.dp).padding(horizontal = MastheadPadding),
            horizontalArrangement = Arrangement.spacedBy(MastheadSpacing),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            HabimetriaMark(size = MastheadMarkSize, tint = foreground)
            Text(
                title,
                color = foreground,
                style = MaterialTheme.typography.titleLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f).semantics { heading() },
            )
            if (canGoBack) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, Strings.back, tint = foreground)
                }
            }
        }
        HorizontalDivider(color = colors.outlineVariant)
    }
}
