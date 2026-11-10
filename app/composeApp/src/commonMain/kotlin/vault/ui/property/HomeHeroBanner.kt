package vault.ui.property

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.painterResource
import vault.composeapp.generated.resources.Res
import vault.composeapp.generated.resources.home_brazil_house
import vault.ui.Strings

// Short native copy and decorative Brazilian house art never overlap.
// The content grows with font scale; Nova vistoria is the only action.
@Composable
fun HomeHeroBanner(enabled: Boolean, onNewInspection: () -> Unit) {
    OutlinedCard(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.outlinedCardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(
                    modifier = Modifier.weight(0.55f),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text(
                        Strings.homeHeroTitle,
                        style = MaterialTheme.typography.headlineSmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.semantics { heading() },
                    )
                    Text(
                        Strings.homeHeroSubtitle,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                    )
                    HeroCheck(Strings.homeHeroValue)
                }
                Image(
                    painter = painterResource(Res.drawable.home_brazil_house),
                    contentDescription = null,
                    modifier = Modifier.weight(0.45f).aspectRatio(1.5f),
                    contentScale = ContentScale.Fit,
                )
            }
            Surface(
                onClick = onNewInspection,
                enabled = enabled,
                color = MaterialTheme.colorScheme.primary,
                shape = RoundedCornerShape(16.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(12.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(
                        modifier = Modifier.size(40.dp).clip(CircleShape)
                            .background(MaterialTheme.colorScheme.onPrimary),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            Icons.Filled.Add,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                        )
                    }
                    Text(
                        Strings.newInspection,
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.weight(1f),
                    )
                    Icon(
                        Icons.Filled.ChevronRight,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimary,
                    )
                }
            }
        }
    }
}

@Composable
private fun HeroCheck(text: String) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Icon(
            Icons.Filled.CheckCircle,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onPrimaryContainer,
            modifier = Modifier.padding(top = 2.dp).size(16.dp),
        )
        Text(
            text,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onPrimaryContainer,
        )
    }
}
