package vault.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import vault.local.ProblemOption
import vault.ui.AppContainer
import vault.ui.ErrorText
import vault.ui.ProgressStatus
import vault.ui.ScreenTitle
import vault.ui.SkeletonLine
import vault.ui.rememberLoadingVisible
import vault.ui.VaultTextField
import vault.ui.userErrorMessage

// Problem customization: toggle built-ins, add and delete customs.
// Findings keep storing stable codes, so sealed bytes never change.
@Composable
fun FindingProblemsScreen(container: AppContainer) {
    val scope = rememberCoroutineScope()
    val local = container.local
    var options by remember { mutableStateOf<List<ProblemOption>?>(null) }
    var draft by rememberSaveable { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }
    var error by rememberSaveable { mutableStateOf<String?>(null) }

    suspend fun load() {
        try {
            options = local?.findingProblems?.listAll()
        } catch (e: Exception) {
            error = userErrorMessage(e)
        }
    }

    LaunchedEffect(local) { load() }

    val showLoading = rememberLoadingVisible(options == null)

    fun run(work: suspend () -> Unit) {
        scope.launch {
            busy = true
            error = null
            try {
                work()
                draft = ""
                load()
            } catch (e: Exception) {
                error = userErrorMessage(e)
            } finally {
                busy = false
            }
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            ScreenTitle(SettingsStrings.problemsTitle)
            Text(
                SettingsStrings.problemsHint,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        item { ErrorText(error) }
        if (showLoading) {
            item { ProgressStatus(true) }
            item { SkeletonLine(0.6f, 20.dp) }
            item { SkeletonLine(0.5f, 20.dp) }
            item { SkeletonLine(0.7f, 20.dp) }
        } else if (options == null) {
            // Grace period: data is on its way, paint nothing yet.
        } else {
            items(options!!, key = { it.code }) { option ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        option.label,
                        modifier = Modifier.weight(1f),
                        style = MaterialTheme.typography.bodyLarge,
                    )
                    if (!option.builtin) {
                        IconButton(
                            onClick = { run { local?.findingProblems?.deleteCustom(option.code) } },
                            enabled = !busy,
                        ) {
                            Icon(
                                Icons.Filled.Delete,
                                contentDescription = SettingsStrings.deleteProblemDescription,
                                tint = MaterialTheme.colorScheme.error,
                            )
                        }
                    }
                    Switch(
                        checked = option.active,
                        enabled = !busy,
                        onCheckedChange = { active ->
                            run { local?.findingProblems?.setActive(option.code, active) }
                        },
                    )
                }
            }
        }
        item {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                VaultTextField(
                    value = draft,
                    onValueChange = { draft = it },
                    label = SettingsStrings.newProblemLabel,
                    modifier = Modifier.weight(1f),
                )
                IconButton(
                    onClick = { run { local?.findingProblems?.addCustom(draft) } },
                    enabled = !busy && draft.isNotBlank(),
                ) {
                    Icon(Icons.Filled.Add, contentDescription = SettingsStrings.addProblem)
                }
            }
        }
    }
}
