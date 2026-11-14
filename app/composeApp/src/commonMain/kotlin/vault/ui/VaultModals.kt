package vault.ui

import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AlertDialogDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties

private val ModalWidth = Modifier.widthIn(max = 600.dp).fillMaxWidth(0.94f)
private val WideDialogProperties = DialogProperties(usePlatformDefaultWidth = false)

@Composable
internal fun VaultAlertDialog(
    onDismissRequest: () -> Unit,
    confirmButton: @Composable () -> Unit,
    dismissButton: (@Composable () -> Unit)? = null,
    title: (@Composable () -> Unit)? = null,
    text: (@Composable () -> Unit)? = null,
    containerColor: Color = AlertDialogDefaults.containerColor,
    titleContentColor: Color = AlertDialogDefaults.titleContentColor,
) {
    AlertDialog(
        onDismissRequest = onDismissRequest,
        confirmButton = confirmButton,
        dismissButton = dismissButton,
        title = title,
        text = text,
        modifier = ModalWidth,
        shape = MaterialTheme.shapes.extraLarge,
        containerColor = containerColor,
        titleContentColor = titleContentColor,
        properties = WideDialogProperties,
    )
}

@Composable
internal fun VaultFormDialog(onDismissRequest: () -> Unit, content: @Composable () -> Unit) {
    Dialog(onDismissRequest = onDismissRequest, properties = WideDialogProperties) {
        Surface(modifier = ModalWidth, shape = MaterialTheme.shapes.large, content = content)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun VaultModalBottomSheet(
    onDismissRequest: () -> Unit,
    content: @Composable ColumnScope.() -> Unit,
) {
    ModalBottomSheet(onDismissRequest = onDismissRequest, modifier = ModalWidth, content = content)
}
