package vault.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

// Home: property entry point. Capture-first; address inline, no signup.
@Composable
fun HomeScreen(
    coverage: Int,
    onStartRoom: () -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text("Don't pay for what you didn't break.")
        LinearProgressIndicator(progress = { coverage / 100f }, modifier = Modifier.fillMaxWidth())
        Text("Coverage: $coverage%")
        Button(onClick = onStartRoom, modifier = Modifier.fillMaxWidth()) {
            Text("Photograph kitchen")
        }
    }
}
