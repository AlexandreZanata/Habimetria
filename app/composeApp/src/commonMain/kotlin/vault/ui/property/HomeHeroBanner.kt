package vault.ui.property

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import vault.ui.Strings
import vault.ui.brand.FacadeDrawing

@Composable
internal fun HomeHeroBanner(
    enabled: Boolean,
    showLocalMode: Boolean,
    onNewInspection: () -> Unit,
    onDismiss: (() -> Unit)?,
) {
    val colors = MaterialTheme.colorScheme
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.extraLarge,
        color = colors.inverseSurface,
        contentColor = colors.inverseOnSurface,
    ) {
        Box(Modifier.clipToBounds()) {
            FacadeDrawing(
                modifier = Modifier.align(Alignment.CenterEnd).width(186.dp).height(164.dp),
                ink = colors.inversePrimary.copy(alpha = 0.24f),
            )
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(start = 22.dp, end = 10.dp, top = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Box(Modifier.weight(1f)) {
                        if (showLocalMode) {
                            Surface(
                                color = colors.inverseOnSurface,
                                contentColor = colors.inverseSurface,
                                shape = CircleShape,
                            ) {
                                Text(
                                    Strings.localModeLabel,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                    style = MaterialTheme.typography.labelSmall,
                                )
                            }
                        } else {
                            HomeHeroEyebrow()
                        }
                    }
                    IconButton(onClick = { onDismiss?.invoke() }, enabled = onDismiss != null) {
                        Icon(Icons.Filled.Close, contentDescription = Strings.hideHomeBanner)
                    }
                }
                Column(
                    modifier = Modifier.fillMaxWidth().padding(start = 22.dp, end = 22.dp, top = 4.dp, bottom = 22.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    if (showLocalMode) HomeHeroEyebrow()
                    Text(
                        Strings.tagline,
                        style = MaterialTheme.typography.displaySmall,
                        modifier = Modifier.semantics { heading() },
                    )
                    Text(Strings.homeHeroSubtitle, style = MaterialTheme.typography.bodyMedium)
                    HomeNewInspectionAction(enabled, onNewInspection, onBanner = true)
                }
            }
        }
    }
}

@Composable
private fun HomeHeroEyebrow() {
    Text(
        Strings.appDescription.uppercase(),
        color = MaterialTheme.colorScheme.inversePrimary,
        style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 1.3.sp),
    )
}

@Composable
internal fun HomeNewInspectionAction(enabled: Boolean, onClick: () -> Unit, onBanner: Boolean = false) {
    val colors = MaterialTheme.colorScheme
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp),
        shape = MaterialTheme.shapes.medium,
        colors = ButtonDefaults.buttonColors(
            containerColor = if (onBanner) colors.inverseOnSurface else colors.inverseSurface,
            contentColor = if (onBanner) colors.inverseSurface else colors.inverseOnSurface,
        ),
        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(Icons.Filled.Add, contentDescription = null)
            Text(Strings.newInspection, modifier = Modifier.weight(1f))
            Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null)
        }
    }
}
